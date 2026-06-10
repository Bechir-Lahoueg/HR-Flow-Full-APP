package org.example.service;

import org.example.config.DatabaseConfig;
import org.example.model.LeaveBalance;
import org.example.model.LeaveRequest;
import org.example.model.LeaveRequest.LeaveStatus;
import org.example.model.LeaveSubmitResult;
import org.example.service.PublicHolidayService.HolidayEntry;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour la gestion des demandes de congés
 * Intègre la logique métier et l'accès aux données
 */
public class LeaveRequestService {

    private final LeaveBalanceService      leaveBalanceService      = new LeaveBalanceService();
    private final PublicHolidayService     publicHolidayService     = new PublicHolidayService();
    private final ConflictDetectionService conflictDetectionService = new ConflictDetectionService();

    /** Pays utilisé pour les jours fériés (modifiable si besoin). */
    private String countryCode = PublicHolidayService.DEFAULT_COUNTRY;

    // ─── Workflow constants (mirrors web) ───────────────────────────────────────
    public static final String CATEGORY_NORMAL             = LeaveRequest.CATEGORY_NORMAL;
    public static final String CATEGORY_EXCEPTION          = LeaveRequest.CATEGORY_EXCEPTION;
    public static final String WORKFLOW_NORMAL             = LeaveRequest.WORKFLOW_NORMAL;
    public static final String WORKFLOW_RH_PENDING         = LeaveRequest.WORKFLOW_RH_PENDING;
    public static final String WORKFLOW_ADMIN_PENDING      = LeaveRequest.WORKFLOW_ADMIN_PENDING;
    public static final String WORKFLOW_ADMIN_APPROVED     = LeaveRequest.WORKFLOW_ADMIN_APPROVED;
    public static final String WORKFLOW_RH_REJECTED        = LeaveRequest.WORKFLOW_RH_REJECTED;
    public static final String WORKFLOW_ADMIN_REJECTED     = LeaveRequest.WORKFLOW_ADMIN_REJECTED;
    public static final String WORKFLOW_FROZEN_UNPROCESSED = LeaveRequest.WORKFLOW_FROZEN_UNPROCESSED;

    private static final String[] VALID_URGENCY_LEVELS = {"LOW", "MEDIUM", "HIGH"};
    private boolean freezeAlreadyRan = false;

    public LeaveRequestService() {
        initializeTable();
    }

    /**
     * Initialise la table leave_requests et ajoute les colonnes manquantes (migration).
     */
    private void initializeTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS leave_requests (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    employee_id INT NOT NULL,
                    employee_name VARCHAR(255) NOT NULL,
                    start_date DATE NOT NULL,
                    end_date DATE NOT NULL,
                    leave_type VARCHAR(100) NOT NULL,
                    reason TEXT,
                    status VARCHAR(20) NOT NULL DEFAULT 'ATTENTE',
                    request_date DATE NOT NULL,
                    rh_comment TEXT,
                    days_count INT NOT NULL,
                    FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            // Migrate existing tables that don't have the new columns yet
            String[] alters = {
                "ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS request_category VARCHAR(20) NOT NULL DEFAULT 'NORMAL'",
                "ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS workflow_status VARCHAR(50) NULL",
                "ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS urgency_level VARCHAR(20) NULL",
                "ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS expected_return_date DATE NULL",
                "ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS attachment_path VARCHAR(255) NULL",
                "ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS attachment_ocr_text TEXT NULL",
                "ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS attachment_ocr_summary TEXT NULL",
                "ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS admin_comment TEXT NULL",
                "ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS rh_decision_at DATETIME NULL",
                "ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS rh_decision_by VARCHAR(120) NULL",
                "ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS admin_decision_at DATETIME NULL",
                "ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS admin_decision_by VARCHAR(120) NULL",
                "ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS audit_log TEXT NULL"
            };
            for (String alter : alters) {
                try { stmt.execute(alter); } catch (SQLException ignored) { /* column may already exist */ }
            }
        } catch (SQLException e) {
            System.err.println("Erreur initialisation table leave_requests: " + e.getMessage());
        }
    }

    /**
     * Soumettre une nouvelle demande de congé.
     * Retourne un {@link LeaveSubmitResult} détaillé :
     * <ul>
     *   <li>Vérifie qu'aucun jour férié n'est inclus dans la période.</li>
     *   <li>Vérifie qu'il existe au moins un jour ouvrable.</li>
     *   <li>Calcule {@code days_count} en jours ouvrables (hors week-ends et fériés).</li>
     * </ul>
     */
    public LeaveSubmitResult submitLeaveRequest(int employeeId, String employeeName,
                                     LocalDate startDate, LocalDate endDate,
                                     String leaveType, String reason) {
        autoFreezeExpiredExceptionalRequests();
        return submitLeaveRequest(employeeId, employeeName, startDate, endDate,
                leaveType, reason, CATEGORY_NORMAL, null);
    }

    /**
     * Soumettre une demande de congé — NORMALE ou EXCEPTIONNELLE.
     * Mirrors web LeaveRequestService::submitEmployeeRequest().
     */
    public LeaveSubmitResult submitLeaveRequest(int employeeId, String employeeName,
                                                LocalDate startDate, LocalDate endDate,
                                                String leaveType, String reason,
                                                String requestMode, String urgencyLevel) {
        autoFreezeExpiredExceptionalRequests();

        if (startDate.isBefore(LocalDate.now()))
            return LeaveSubmitResult.validationError("La date de début ne peut pas être dans le passé.");
        if (endDate.isBefore(startDate))
            return LeaveSubmitResult.validationError("La date de fin doit être après la date de début.");

        int calendarDays = (int)(endDate.toEpochDay() - startDate.toEpochDay()) + 1;

        List<HolidayEntry> holidays = publicHolidayService.findHolidaysInRange(startDate, endDate, countryCode);
        if (!holidays.isEmpty()) {
            int wd = publicHolidayService.countWorkingDays(startDate, endDate, countryCode);
            return LeaveSubmitResult.blockedByHoliday(holidays, wd, calendarDays);
        }

        int workingDays = publicHolidayService.countWorkingDays(startDate, endDate, countryCode);
        if (workingDays == 0)
            return LeaveSubmitResult.noWorkingDays(calendarDays);

        if (hasDateOverlap(employeeId, startDate, endDate))
            return LeaveSubmitResult.validationError("Cette période chevauche une demande en attente ou acceptée.");

        String normalizedMode = (requestMode == null) ? CATEGORY_NORMAL : requestMode.toUpperCase().trim();
        boolean isException = CATEGORY_EXCEPTION.equals(normalizedMode);

        // Balance check: block normal when insufficient
        if (!isException) {
            LeaveBalance balance = leaveBalanceService.getBalance(employeeId);
            double available = (balance != null) ? balance.getAvailableDays() : 0;
            if (available <= 0 || workingDays > available) {
                return LeaveSubmitResult.validationError(
                    "Crédit insuffisant (" + String.format("%.1f", available) + " j disponibles). " +
                    "Utilisez une demande exceptionnelle pour dépassement.");
            }
        }

        // Exception-specific validation
        String normalizedUrgency = null;
        if (isException) {
            if (reason == null || reason.trim().length() < 15)
                return LeaveSubmitResult.validationError("Pour une demande exceptionnelle, le motif détaillé est obligatoire (minimum 15 caractères).");
            normalizedUrgency = (urgencyLevel != null) ? urgencyLevel.toUpperCase().trim() : "";
            boolean validUrgency = false;
            for (String u : VALID_URGENCY_LEVELS) if (u.equals(normalizedUrgency)) { validUrgency = true; break; }
            if (!validUrgency)
                return LeaveSubmitResult.validationError("Niveau d'urgence invalide. Choisissez: LOW, MEDIUM ou HIGH.");
        }

        LeaveRequest req = new LeaveRequest(employeeId, employeeName, startDate, endDate, leaveType, reason);
        req.setDaysCount(workingDays);
        req.setRequestCategory(normalizedMode);
        if (isException) {
            req.setWorkflowStatus(WORKFLOW_RH_PENDING);
            req.setUrgencyLevel(normalizedUrgency);
            req.appendAuditLog(employeeName, "EXCEPTION_REQUEST_CREATED", reason);
        } else {
            req.setWorkflowStatus(WORKFLOW_NORMAL);
            req.appendAuditLog(employeeName, "NORMAL_REQUEST_CREATED");
        }

        String sql = """
            INSERT INTO leave_requests
            (employee_id, employee_name, start_date, end_date, leave_type, reason,
             status, request_date, days_count, request_category, workflow_status,
             urgency_level, audit_log)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, req.getEmployeeId());
            ps.setString(2, req.getEmployeeName());
            ps.setDate(3, Date.valueOf(req.getStartDate()));
            ps.setDate(4, Date.valueOf(req.getEndDate()));
            ps.setString(5, req.getLeaveType());
            ps.setString(6, req.getReason());
            ps.setString(7, req.getStatus().name());
            ps.setDate(8, Date.valueOf(req.getRequestDate()));
            ps.setInt(9, req.getDaysCount());
            ps.setString(10, req.getRequestCategory());
            ps.setString(11, req.getWorkflowStatus());
            ps.setString(12, req.getUrgencyLevel());
            ps.setString(13, req.getAuditLog());
            if (ps.executeUpdate() > 0)
                return LeaveSubmitResult.success(workingDays, calendarDays);
        } catch (SQLException e) {
            System.err.println("Erreur création demande: " + e.getMessage());
        }
        return LeaveSubmitResult.dbError();
    }

    /**
     * Pré-calcule les informations d'une période sans rien enregistrer.
     * Utilisé pour l'affichage en temps réel dans le formulaire.
     */
    public LeaveSubmitResult previewRequest(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return LeaveSubmitResult.validationError("Dates invalides.");
        }
        int calendarDays = (int)(endDate.toEpochDay() - startDate.toEpochDay()) + 1;
        List<HolidayEntry> holidays = publicHolidayService.findHolidaysInRange(startDate, endDate, countryCode);
        int workingDays = publicHolidayService.countWorkingDays(startDate, endDate, countryCode);
        if (!holidays.isEmpty()) {
            return LeaveSubmitResult.blockedByHoliday(holidays, workingDays, calendarDays);
        }
        if (workingDays == 0) {
            return LeaveSubmitResult.noWorkingDays(calendarDays);
        }
        return LeaveSubmitResult.success(workingDays, calendarDays);
    }

    /** Accès direct au service jours fériés (utilisé par l'UI). */
    public PublicHolidayService getPublicHolidayService() {
        return publicHolidayService;
    }

    /** Permet de changer le pays (ex : "MA", "DZ", "TN"...). */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
    public String getCountryCode() {
        return countryCode;
    }

    // ─── Détection de conflits ───────────────────────────────────────────────────

    /**
     * Analyse les conflits pour une demande existante.
     * Charge toutes les demandes depuis la DB et délègue au {@link ConflictDetectionService}.
     *
     * @param request Demande à analyser
     * @return {@link ConflictResult} avec le niveau de gravité
     */
    public ConflictResult detectConflicts(LeaveRequest request) {
        List<LeaveRequest> all = getAllLeaveRequests();
        return conflictDetectionService.detectConflicts(request, all);
    }

    /**
     * Analyse les conflits pour une période avant soumission.
     *
     * @param employeeId ID de l'employé demandeur
     * @param startDate  Début de la période
     * @param endDate    Fin de la période
     * @return {@link ConflictResult} avec le niveau de gravité
     */
    public ConflictResult detectConflictsForPeriod(int employeeId,
                                                    LocalDate startDate,
                                                    LocalDate endDate) {
        List<LeaveRequest> all = getAllLeaveRequests();
        return conflictDetectionService.detectConflictsForPeriod(employeeId, startDate, endDate, all);
    }

    /** Accès direct au service de détection de conflits (pour configuration). */
    public ConflictDetectionService getConflictDetectionService() {
        return conflictDetectionService;
    }

    /**
     * Récupérer toutes les demandes d'un employé
     */
    public List<LeaveRequest> getEmployeeLeaveRequests(int employeeId) {
        List<LeaveRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM leave_requests WHERE employee_id = ? ORDER BY request_date DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                requests.add(mapResultSetToLeaveRequest(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des demandes: " + e.getMessage());
            e.printStackTrace();
        }
        return requests;
    }

    /**
     * Récupérer toutes les demandes (pour RH)
     */
    public List<LeaveRequest> getAllLeaveRequests() {
        List<LeaveRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM leave_requests ORDER BY request_date DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                requests.add(mapResultSetToLeaveRequest(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de toutes les demandes: " + e.getMessage());
            e.printStackTrace();
        }
        return requests;
    }

    /**
     * Récupérer les demandes des employés gérés par un RH (rh_id dans la table employees)
     */
    public List<LeaveRequest> getLeaveRequestsByRH(int rhId) {
        List<LeaveRequest> requests = new ArrayList<>();
        String sql = "SELECT lr.* FROM leave_requests lr JOIN employees e ON lr.employee_id = e.id WHERE e.rh_id = ? ORDER BY lr.request_date DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rhId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                requests.add(mapResultSetToLeaveRequest(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des demandes par RH: " + e.getMessage());
            e.printStackTrace();
        }
        return requests;
    }


    /**
     * Récupérer les demandes par statut
     */
    public List<LeaveRequest> getLeaveRequestsByStatus(LeaveStatus status) {
        List<LeaveRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM leave_requests WHERE status = ? ORDER BY request_date DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                requests.add(mapResultSetToLeaveRequest(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération par statut: " + e.getMessage());
            e.printStackTrace();
        }
        return requests;
    }

    /**
     * Récupérer les demandes en attente
     */
    public List<LeaveRequest> getPendingLeaveRequests() {
        return getLeaveRequestsByStatus(LeaveStatus.ATTENTE);
    }

    /**
     * Récupérer les demandes acceptées
     */
    public List<LeaveRequest> getAcceptedLeaveRequests() {
        return getLeaveRequestsByStatus(LeaveStatus.ACCEPTE);
    }

    /**
     * Récupérer les demandes refusées
     */
    public List<LeaveRequest> getRefusedLeaveRequests() {
        return getLeaveRequestsByStatus(LeaveStatus.REFUSE);
    }

    /**
     * Récupérer une demande par ID
     */
    public LeaveRequest getLeaveRequestById(int requestId) {
        String sql = "SELECT * FROM leave_requests WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, requestId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToLeaveRequest(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la demande: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Approuver une demande (RH).
     * NORMALE → approve + déduction solde.
     * EXCEPTION → pré-approuve (workflow → ADMIN_PENDING), pas de déduction encore.
     * Mirrors web approveRequestByRh().
     */
    public boolean approveLeaveRequest(int requestId, String rhComment) {
        return approveLeaveRequest(requestId, rhComment, "RH");
    }

    public boolean approveLeaveRequest(int requestId, String rhComment, String rhActor) {
        autoFreezeExpiredExceptionalRequests();
        LeaveRequest request = getLeaveRequestById(requestId);
        if (request == null || request.getStatus() != LeaveStatus.ATTENTE) return false;

        if (CATEGORY_EXCEPTION.equals(request.getRequestCategory())) {
            if (!WORKFLOW_RH_PENDING.equals(request.getWorkflowStatus())) return false;
            request.appendAuditLog(rhActor, "RH_PRE_APPROVED", rhComment);
            String sql = "UPDATE leave_requests SET workflow_status=?, rh_comment=?, rh_decision_at=?, rh_decision_by=?, audit_log=? WHERE id=?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, WORKFLOW_ADMIN_PENDING);
                ps.setString(2, rhComment);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.setString(4, rhActor);
                ps.setString(5, request.getAuditLog());
                ps.setInt(6, requestId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { System.err.println("approveLeaveRequest(EXCEPTION): " + e.getMessage()); }
            return false;
        }

        // Normal: approve directly
        request.appendAuditLog(rhActor, "RH_APPROVED", rhComment);
        String sql = "UPDATE leave_requests SET status='ACCEPTE', rh_comment=?, rh_decision_at=?, rh_decision_by=?, audit_log=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rhComment);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, rhActor);
            ps.setString(4, request.getAuditLog());
            ps.setInt(5, requestId);
            if (ps.executeUpdate() > 0) {
                leaveBalanceService.deductLeave(request.getEmployeeId(), request.getDaysCount());
                return true;
            }
        } catch (SQLException e) { System.err.println("approveLeaveRequest(NORMAL): " + e.getMessage()); }
        return false;
    }

    /**
     * Refuser une demande (RH). Mirrors web rejectRequestByRh().
     */
    public boolean rejectLeaveRequest(int requestId, String rhComment) {
        return rejectLeaveRequest(requestId, rhComment, "RH");
    }

    public boolean rejectLeaveRequest(int requestId, String rhComment, String rhActor) {
        autoFreezeExpiredExceptionalRequests();
        LeaveRequest request = getLeaveRequestById(requestId);
        if (request == null || request.getStatus() != LeaveStatus.ATTENTE) return false;
        if (rhComment == null || rhComment.isBlank()) return false;

        String newWorkflow = CATEGORY_EXCEPTION.equals(request.getRequestCategory())
                ? WORKFLOW_RH_REJECTED : null;
        request.appendAuditLog(rhActor, CATEGORY_EXCEPTION.equals(request.getRequestCategory())
                ? "RH_REJECTED_EXCEPTION" : "RH_REJECTED", rhComment);
        String sql = "UPDATE leave_requests SET status='REFUSE', rh_comment=?, rh_decision_at=?, rh_decision_by=?, workflow_status=COALESCE(?,workflow_status), audit_log=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rhComment);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, rhActor);
            ps.setString(4, newWorkflow);
            ps.setString(5, request.getAuditLog());
            ps.setInt(6, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("rejectLeaveRequest: " + e.getMessage()); }
        return false;
    }

    // ─── ADMIN: exceptional leave workflow ───────────────────────────────────────

    /** Returns all exceptional leave requests visible to Admin. */
    public List<LeaveRequest> getAdminExceptionRequests() {
        autoFreezeExpiredExceptionalRequests();
        List<LeaveRequest> list = new ArrayList<>();
        String sql = """
            SELECT * FROM leave_requests
            WHERE request_category = 'EXCEPTION'
              AND workflow_status IN ('ADMIN_PENDING','ADMIN_APPROVED','ADMIN_REJECTED','FROZEN_UNPROCESSED')
            ORDER BY request_date DESC
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSetToLeaveRequest(rs));
        } catch (SQLException e) { System.err.println("getAdminExceptionRequests: " + e.getMessage()); }
        return list;
    }

    /** Admin final approval of an exceptional leave. Mirrors web approveExceptionByAdmin(). */
    public boolean approveExceptionByAdmin(int requestId, String adminComment, String adminActor) {
        autoFreezeExpiredExceptionalRequests();
        LeaveRequest req = getLeaveRequestById(requestId);
        if (req == null || !CATEGORY_EXCEPTION.equals(req.getRequestCategory())) return false;
        if (!WORKFLOW_ADMIN_PENDING.equals(req.getWorkflowStatus())) return false;

        req.appendAuditLog(adminActor, "ADMIN_APPROVED_EXCEPTION", adminComment);
        String sql = "UPDATE leave_requests SET status='ACCEPTE', workflow_status=?, admin_comment=?, admin_decision_at=?, admin_decision_by=?, audit_log=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, WORKFLOW_ADMIN_APPROVED);
            ps.setString(2, adminComment);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, adminActor);
            ps.setString(5, req.getAuditLog());
            ps.setInt(6, requestId);
            if (ps.executeUpdate() > 0) {
                leaveBalanceService.deductLeave(req.getEmployeeId(), req.getDaysCount());
                return true;
            }
        } catch (SQLException e) { System.err.println("approveExceptionByAdmin: " + e.getMessage()); }
        return false;
    }

    /** Admin rejection of an exceptional leave. Mirrors web rejectExceptionByAdmin(). */
    public boolean rejectExceptionByAdmin(int requestId, String adminComment, String adminActor) {
        autoFreezeExpiredExceptionalRequests();
        if (adminComment == null || adminComment.isBlank()) return false;
        LeaveRequest req = getLeaveRequestById(requestId);
        if (req == null || !CATEGORY_EXCEPTION.equals(req.getRequestCategory())) return false;
        if (!WORKFLOW_ADMIN_PENDING.equals(req.getWorkflowStatus())) return false;

        req.appendAuditLog(adminActor, "ADMIN_REJECTED_EXCEPTION", adminComment);
        String sql = "UPDATE leave_requests SET status='REFUSE', workflow_status=?, admin_comment=?, admin_decision_at=?, admin_decision_by=?, audit_log=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, WORKFLOW_ADMIN_REJECTED);
            ps.setString(2, adminComment);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, adminActor);
            ps.setString(5, req.getAuditLog());
            ps.setInt(6, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("rejectExceptionByAdmin: " + e.getMessage()); }
        return false;
    }

    /**
     * Freeze exceptional requests past their start_date without being processed.
     * Called automatically on every public read/write method.
     */
    private void autoFreezeExpiredExceptionalRequests() {
        if (freezeAlreadyRan) return;
        freezeAlreadyRan = true;
        List<Integer> ids = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        String query = """
            SELECT id, start_date FROM leave_requests
            WHERE request_category='EXCEPTION'
              AND status='ATTENTE'
              AND workflow_status IN ('RH_PENDING','ADMIN_PENDING')
              AND start_date < ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                    labels.add("Demande gelée automatiquement: non traitée avant le " + rs.getDate("start_date").toLocalDate());
                }
            }
        } catch (SQLException e) { System.err.println("autoFreeze query: " + e.getMessage()); return; }

        if (ids.isEmpty()) return;
        String update = "UPDATE leave_requests SET status='REFUSE', workflow_status='FROZEN_UNPROCESSED', rh_comment=COALESCE(rh_comment,?), audit_log=CONCAT(COALESCE(audit_log,''),' | SYSTEM | AUTO_FROZEN_UNPROCESSED') WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(update)) {
            for (int i = 0; i < ids.size(); i++) {
                ps.setString(1, labels.get(i));
                ps.setInt(2, ids.get(i));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) { System.err.println("autoFreeze update: " + e.getMessage()); }
    }

    /**
     * Supprimer une demande de congé
     */
    public boolean deleteLeaveRequest(int requestId, int employeeId) {
        LeaveRequest request = getLeaveRequestById(requestId);
        if (request == null) {
            System.err.println("Demande non trouvée");
            return false;
        }

        // Vérifier que c'est bien l'employé propriétaire
        if (request.getEmployeeId() != employeeId) {
            System.err.println("Vous n'êtes pas autorisé à supprimer cette demande");
            return false;
        }

        // On ne peut supprimer que les demandes en attente
        if (request.getStatus() != LeaveStatus.ATTENTE) {
            System.err.println("Impossible de supprimer une demande déjà traitée");
            return false;
        }

        String sql = "DELETE FROM leave_requests WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, requestId);
            boolean deleted = pstmt.executeUpdate() > 0;
            // Pas de remboursement nécessaire : les demandes en attente n'ont pas encore été déduites
            return deleted;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Supprimer une demande de congé par le RH (fonctionne pour ATTENTE et ACCEPTE).
     * Si la demande est acceptée, les jours sont remboursés au solde de l'employé.
     */
    public boolean rhDeleteLeaveRequest(int requestId) {
        LeaveRequest request = getLeaveRequestById(requestId);
        if (request == null) {
            System.err.println("Demande non trouvée");
            return false;
        }

        // Rembourser les jours si le congé était déjà accepté
        if (request.getStatus() == LeaveStatus.ACCEPTE) {
            boolean refunded = leaveBalanceService.refundLeave(request.getEmployeeId(), request.getDaysCount());
            if (!refunded) {
                System.err.println("Avertissement : impossible de rembourser le solde pour l'employé " + request.getEmployeeId());
            }
        }

        String sql = "DELETE FROM leave_requests WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, requestId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression RH: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Compter les demandes en attente
     */
    public int countPendingRequests() {
        String sql = "SELECT COUNT(*) as count FROM leave_requests WHERE status = 'ATTENTE'";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du comptage: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Obtenir le nombre total de jours de congé pour un employé
     */
    public int getTotalLeaveDaysForEmployee(int employeeId) {
        List<LeaveRequest> requests = getEmployeeLeaveRequests(employeeId);
        return requests.stream()
                .filter(r -> r.getStatus() == LeaveStatus.ACCEPTE || r.getStatus() == LeaveStatus.ATTENTE)
                .mapToInt(LeaveRequest::getDaysCount)
                .sum();
    }

    /**
     * Obtenir le nombre de jours de congé acceptés pour un employé
     */
    public int getAcceptedLeaveDaysForEmployee(int employeeId) {
        List<LeaveRequest> requests = getEmployeeLeaveRequests(employeeId);
        return requests.stream()
                .filter(r -> r.getStatus() == LeaveStatus.ACCEPTE)
                .mapToInt(LeaveRequest::getDaysCount)
                .sum();
    }

    /**
     * Vérifier s'il y a un chevauchement de dates pour un employé
     */
    public boolean hasDateOverlap(int employeeId, LocalDate startDate, LocalDate endDate) {
        List<LeaveRequest> requests = getEmployeeLeaveRequests(employeeId);
        
        return requests.stream()
                .filter(r -> r.getStatus() == LeaveStatus.ACCEPTE || r.getStatus() == LeaveStatus.ATTENTE)
                .anyMatch(r -> datesOverlap(r.getStartDate(), r.getEndDate(), startDate, endDate));
    }

    /**
     * Vérifie si deux périodes de dates se chevauchent
     */
    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }

    /**
     * Mapper ResultSet vers LeaveRequest (incl. extended workflow fields).
     */
    private LeaveRequest mapResultSetToLeaveRequest(ResultSet rs) throws SQLException {
        LeaveRequest r = new LeaveRequest();
        r.setId(rs.getInt("id"));
        r.setEmployeeId(rs.getInt("employee_id"));
        r.setEmployeeName(rs.getString("employee_name"));
        r.setStartDate(rs.getDate("start_date").toLocalDate());
        r.setEndDate(rs.getDate("end_date").toLocalDate());
        r.setLeaveType(rs.getString("leave_type"));
        r.setReason(rs.getString("reason"));
        r.setStatus(LeaveStatus.valueOf(rs.getString("status")));
        r.setRequestDate(rs.getDate("request_date").toLocalDate());
        r.setRhComment(rs.getString("rh_comment"));
        r.setDaysCount(rs.getInt("days_count"));
        // Extended fields — use tryGet to gracefully handle older DB schemas
        r.setRequestCategory(getStrSafe(rs, "request_category", LeaveRequest.CATEGORY_NORMAL));
        r.setWorkflowStatus(getStrSafe(rs, "workflow_status", null));
        r.setUrgencyLevel(getStrSafe(rs, "urgency_level", null));
        r.setAdminComment(getStrSafe(rs, "admin_comment", null));
        r.setRhDecisionBy(getStrSafe(rs, "rh_decision_by", null));
        r.setAdminDecisionBy(getStrSafe(rs, "admin_decision_by", null));
        r.setAuditLog(getStrSafe(rs, "audit_log", null));
        r.setAttachmentPath(getStrSafe(rs, "attachment_path", null));
        Timestamp rhDec = null;
        try { rhDec = rs.getTimestamp("rh_decision_at"); } catch (SQLException ignored) {}
        if (rhDec != null) r.setRhDecisionAt(rhDec.toLocalDateTime());
        Timestamp adminDec = null;
        try { adminDec = rs.getTimestamp("admin_decision_at"); } catch (SQLException ignored) {}
        if (adminDec != null) r.setAdminDecisionAt(adminDec.toLocalDateTime());
        return r;
    }

    private String getStrSafe(ResultSet rs, String col, String def) {
        try { String v = rs.getString(col); return v != null ? v : def; }
        catch (SQLException ignored) { return def; }
    }
}
