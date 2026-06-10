package org.example.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeaveRequest {

    // ─── Workflow constants (mirrors web) ────────────────────────────────────────
    public static final String CATEGORY_NORMAL    = "NORMAL";
    public static final String CATEGORY_EXCEPTION = "EXCEPTION";

    public static final String WORKFLOW_NORMAL            = "NORMAL";
    public static final String WORKFLOW_RH_PENDING        = "RH_PENDING";
    public static final String WORKFLOW_ADMIN_PENDING     = "ADMIN_PENDING";
    public static final String WORKFLOW_ADMIN_APPROVED    = "ADMIN_APPROVED";
    public static final String WORKFLOW_RH_REJECTED       = "RH_REJECTED";
    public static final String WORKFLOW_ADMIN_REJECTED    = "ADMIN_REJECTED";
    public static final String WORKFLOW_FROZEN_UNPROCESSED = "FROZEN_UNPROCESSED";

    // ─── Fields ──────────────────────────────────────────────────────────────────
    private int id;
    private int employeeId;
    private String employeeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String leaveType;
    private String reason;
    private LeaveStatus status;
    private LocalDate requestDate;
    private String rhComment;
    private int daysCount;

    // Extended fields matching web entity
    private String requestCategory = CATEGORY_NORMAL;
    private String workflowStatus  = WORKFLOW_NORMAL;
    private String urgencyLevel;          // LOW / MEDIUM / HIGH
    private String adminComment;
    private LocalDateTime rhDecisionAt;
    private String rhDecisionBy;
    private LocalDateTime adminDecisionAt;
    private String adminDecisionBy;
    private String auditLog;
    private String attachmentPath;

    public enum LeaveStatus {
        ATTENTE("En attente"),
        ACCEPTE("Accepté"),
        REFUSE("Refusé");

        private final String displayName;
        LeaveStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // ─── Constructors ─────────────────────────────────────────────────────────────
    public LeaveRequest() {
        this.status = LeaveStatus.ATTENTE;
        this.requestDate = LocalDate.now();
    }

    public LeaveRequest(int employeeId, String employeeName, LocalDate startDate,
                        LocalDate endDate, String leaveType, String reason) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.leaveType = leaveType;
        this.reason = reason;
        this.status = LeaveStatus.ATTENTE;
        this.requestDate = LocalDate.now();
        this.daysCount = calculateDaysCount();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────
    private int calculateDaysCount() {
        if (startDate != null && endDate != null)
            return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
        return 0;
    }

    public boolean isException() {
        return CATEGORY_EXCEPTION.equals(requestCategory);
    }

    public boolean isRhPending() {
        return WORKFLOW_RH_PENDING.equals(workflowStatus);
    }

    public boolean isAdminPending() {
        return WORKFLOW_ADMIN_PENDING.equals(workflowStatus);
    }

    public void appendAuditLog(String actor, String action, String detail) {
        String entry = java.time.LocalDateTime.now() + " | " + actor + " | " + action
                + (detail != null && !detail.isBlank() ? " | " + detail : "");
        if (auditLog == null || auditLog.isBlank())
            auditLog = entry;
        else
            auditLog = auditLog + "\n" + entry;
    }

    public void appendAuditLog(String actor, String action) {
        appendAuditLog(actor, action, null);
    }

    /** Human-readable workflow label for display in JavaFX. */
    public String getWorkflowStatusLabel() {
        if (workflowStatus == null) return "";
        return switch (workflowStatus) {
            case WORKFLOW_NORMAL          -> "Normal";
            case WORKFLOW_RH_PENDING      -> "⏳ En attente RH";
            case WORKFLOW_ADMIN_PENDING   -> "⏳ En attente Admin";
            case WORKFLOW_ADMIN_APPROVED  -> "✅ Approuvé Admin";
            case WORKFLOW_RH_REJECTED     -> "❌ Refusé RH";
            case WORKFLOW_ADMIN_REJECTED  -> "❌ Refusé Admin";
            case WORKFLOW_FROZEN_UNPROCESSED -> "🧊 Gelé (non traité)";
            default -> workflowStatus;
        };
    }

    /** Human-readable urgency for display. */
    public String getUrgencyLabel() {
        if (urgencyLevel == null) return "";
        return switch (urgencyLevel) {
            case "LOW"    -> "🟢 Faible";
            case "MEDIUM" -> "🟡 Moyen";
            case "HIGH"   -> "🔴 Haute";
            default -> urgencyLevel;
        };
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        this.daysCount = calculateDaysCount();
    }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        this.daysCount = calculateDaysCount();
    }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LeaveStatus getStatus() { return status; }
    public void setStatus(LeaveStatus status) { this.status = status; }

    public LocalDate getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }

    public String getRhComment() { return rhComment; }
    public void setRhComment(String rhComment) { this.rhComment = rhComment; }

    public int getDaysCount() { return daysCount; }
    public void setDaysCount(int daysCount) { this.daysCount = daysCount; }

    public String getRequestCategory() { return requestCategory; }
    public void setRequestCategory(String requestCategory) { this.requestCategory = requestCategory; }

    public String getWorkflowStatus() { return workflowStatus; }
    public void setWorkflowStatus(String workflowStatus) { this.workflowStatus = workflowStatus; }

    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }

    public String getAdminComment() { return adminComment; }
    public void setAdminComment(String adminComment) { this.adminComment = adminComment; }

    public LocalDateTime getRhDecisionAt() { return rhDecisionAt; }
    public void setRhDecisionAt(LocalDateTime rhDecisionAt) { this.rhDecisionAt = rhDecisionAt; }

    public String getRhDecisionBy() { return rhDecisionBy; }
    public void setRhDecisionBy(String rhDecisionBy) { this.rhDecisionBy = rhDecisionBy; }

    public LocalDateTime getAdminDecisionAt() { return adminDecisionAt; }
    public void setAdminDecisionAt(LocalDateTime adminDecisionAt) { this.adminDecisionAt = adminDecisionAt; }

    public String getAdminDecisionBy() { return adminDecisionBy; }
    public void setAdminDecisionBy(String adminDecisionBy) { this.adminDecisionBy = adminDecisionBy; }

    public String getAuditLog() { return auditLog; }
    public void setAuditLog(String auditLog) { this.auditLog = auditLog; }

    public String getAttachmentPath() { return attachmentPath; }
    public void setAttachmentPath(String attachmentPath) { this.attachmentPath = attachmentPath; }

    @Override
    public String toString() {
        return "LeaveRequest{id=" + id + ", employeeName='" + employeeName + '\''
                + ", startDate=" + startDate + ", endDate=" + endDate
                + ", leaveType='" + leaveType + '\'' + ", status=" + status
                + ", category=" + requestCategory + ", workflow=" + workflowStatus + '}';
    }
}
