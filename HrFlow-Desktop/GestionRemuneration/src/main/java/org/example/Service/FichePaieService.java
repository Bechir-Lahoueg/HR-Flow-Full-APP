package org.example.Service;

import org.example.Entity.FichePaie;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

/**
 * Service CRUD pour la table `fiches_paie`.
 * Table et colonnes alignées avec le modèle Symfony / web :
 *   id | employee_id | mois (SMALLINT 1-12) | annee | salaire_brut
 *   total_primes | total_deductions | salaire_net | notes
 *   statut_paiement (TINYINT 0/1) | created_at | updated_at
 */
public class FichePaieService {

    private final Connection connection;

    public FichePaieService(Connection connection) {
        this.connection = connection;
    }

    // ==============================
    // VALIDATION
    // ==============================

    private void validateFiche(FichePaie fiche) {

        if (fiche == null) {
            throw new IllegalArgumentException("La fiche de paie ne peut pas être null.");
        }

        if (fiche.getMois() < 1 || fiche.getMois() > 12) {
            throw new IllegalArgumentException("Le mois doit être compris entre 1 et 12.");
        }

        if (fiche.getAnnee() < 2000 || fiche.getAnnee() > 2100) {
            throw new IllegalArgumentException("Année invalide.");
        }

        if (fiche.getSalaireBrut() == null || fiche.getSalaireBrut().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le salaire brut doit être positif.");
        }

        if (fiche.getTotalPrimes() == null || fiche.getTotalPrimes().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le total des primes doit être positif.");
        }

        if (fiche.getTotalDeductions() == null || fiche.getTotalDeductions().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le total des déductions doit être positif.");
        }

        if (fiche.getSalaireNet() == null || fiche.getSalaireNet().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le salaire net doit être positif.");
        }

        if (fiche.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("ID employé invalide.");
        }

        // Vérification logique métier
        BigDecimal expectedNet = fiche.getSalaireBrut()
                .add(fiche.getTotalPrimes())
                .subtract(fiche.getTotalDeductions());

        if (fiche.getSalaireNet().compareTo(expectedNet) != 0) {
            throw new IllegalArgumentException(
                    "Le salaire net doit être égal à Brut + Primes - Déductions."
            );
        }
    }

    // ==============================
    // CREATE
    // ==============================

    public void addFiche(FichePaie fiche) throws SQLException {

        validateFiche(fiche);

        String sql = "INSERT INTO fiches_paie "
                + "(employee_id, mois, annee, salaire_brut, total_primes, total_deductions, "
                + "salaire_net, notes, statut_paiement, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, fiche.getEmployeeId());
            stmt.setInt(2, fiche.getMois());
            stmt.setInt(3, fiche.getAnnee());
            stmt.setBigDecimal(4, fiche.getSalaireBrut());
            stmt.setBigDecimal(5, fiche.getTotalPrimes());
            stmt.setBigDecimal(6, fiche.getTotalDeductions());
            stmt.setBigDecimal(7, fiche.getSalaireNet());
            stmt.setString(8, fiche.getNotes());
            stmt.setBoolean(9, fiche.isStatutPaiement());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                fiche.setId(rs.getInt(1));
            }
        }
    }

    // ==============================
    // READ BY ID
    // ==============================

    public FichePaie getFicheById(int id) throws SQLException {

        String sql = "SELECT * FROM fiches_paie WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSet(rs);
            }
        }

        return null;
    }

    // ==============================
    // READ ALL
    // ==============================

    public List<FichePaie> getAllFiches() throws SQLException {

        String sql = "SELECT * FROM fiches_paie ORDER BY annee DESC, mois DESC";
        List<FichePaie> fiches = new ArrayList<>();

        try (Statement stmt = connection.createStatement()) {

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                fiches.add(mapResultSet(rs));
            }
        }

        return fiches;
    }

    // ==============================
    // READ BY EMPLOYE
    // ==============================

    public List<FichePaie> getFichesByEmploye(int employeeId) throws SQLException {

        String sql = "SELECT * FROM fiches_paie WHERE employee_id = ? ORDER BY annee DESC, mois DESC";
        List<FichePaie> fiches = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                fiches.add(mapResultSet(rs));
            }
        }

        return fiches;
    }

    /** Récupère les fiches d'un employé pour un mois/année donné. */
    public List<FichePaie> getFichesByEmployeAndPeriod(int employeeId, int mois, int annee) throws SQLException {
        String sql = "SELECT * FROM fiches_paie WHERE employee_id = ? AND mois = ? AND annee = ?";
        List<FichePaie> fiches = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            stmt.setInt(2, mois);
            stmt.setInt(3, annee);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) fiches.add(mapResultSet(rs));
        }
        return fiches;
    }

    // ==============================
    // UPDATE
    // ==============================

    public void updateFiche(FichePaie fiche) throws SQLException {

        validateFiche(fiche);

        String sql = "UPDATE fiches_paie SET employee_id=?, mois=?, annee=?, salaire_brut=?, "
                + "total_primes=?, total_deductions=?, salaire_net=?, notes=?, updated_at=NOW() "
                + "WHERE id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, fiche.getEmployeeId());
            stmt.setInt(2, fiche.getMois());
            stmt.setInt(3, fiche.getAnnee());
            stmt.setBigDecimal(4, fiche.getSalaireBrut());
            stmt.setBigDecimal(5, fiche.getTotalPrimes());
            stmt.setBigDecimal(6, fiche.getTotalDeductions());
            stmt.setBigDecimal(7, fiche.getSalaireNet());
            stmt.setString(8, fiche.getNotes());
            stmt.setInt(9, fiche.getId());

            stmt.executeUpdate();
        }
    }

    // ==============================
    // TOGGLE STATUT PAIEMENT
    // ==============================

    /**
     * Bascule le statut de paiement d'une fiche (payé / non payé).
     * Équivalent de l'action toggleStatutPaiement dans le web.
     */
    public void toggleStatutPaiement(int id) throws SQLException {
        String sql = "UPDATE fiches_paie SET statut_paiement = NOT statut_paiement, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ==============================
    // DELETE
    // ==============================

    public void deleteFiche(int id) throws SQLException {

        String sql = "DELETE FROM fiches_paie WHERE id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ==============================
    // STATISTIQUES
    // ==============================

    /**
     * Retourne des statistiques agrégées pour les employés d'un RH.
     * Clés : totalFiches, totalBrut, totalNet, masseSalariale
     */
    public Map<String, Object> getStatsByEmployeeIds(List<Integer> employeeIds) throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFiches", 0);
        stats.put("totalBrut", BigDecimal.ZERO);
        stats.put("totalNet", BigDecimal.ZERO);
        stats.put("masseSalariale", BigDecimal.ZERO);

        if (employeeIds == null || employeeIds.isEmpty()) return stats;

        StringBuilder inClause = new StringBuilder("?");
        for (int i = 1; i < employeeIds.size(); i++) inClause.append(",?");

        String sql = "SELECT COUNT(*) AS total_fiches, "
                + "SUM(salaire_brut) AS total_brut, "
                + "SUM(salaire_net) AS total_net "
                + "FROM fiches_paie WHERE employee_id IN (" + inClause + ")";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < employeeIds.size(); i++) {
                stmt.setInt(i + 1, employeeIds.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                stats.put("totalFiches", rs.getInt("total_fiches"));
                stats.put("totalBrut", rs.getBigDecimal("total_brut") != null ? rs.getBigDecimal("total_brut") : BigDecimal.ZERO);
                BigDecimal net = rs.getBigDecimal("total_net");
                if (net == null) net = BigDecimal.ZERO;
                stats.put("totalNet", net);
                stats.put("masseSalariale", net);
            }
        }
        return stats;
    }

    // ==============================
    // MAPPING
    // ==============================

    private FichePaie mapResultSet(ResultSet rs) throws SQLException {
        FichePaie f = new FichePaie(
                rs.getInt("id"),
                rs.getInt("mois"),
                rs.getInt("annee"),
                rs.getBigDecimal("salaire_brut"),
                rs.getBigDecimal("total_primes"),
                rs.getBigDecimal("total_deductions"),
                rs.getBigDecimal("salaire_net"),
                rs.getInt("employee_id")
        );
        f.setNotes(rs.getString("notes"));
        f.setStatutPaiement(rs.getBoolean("statut_paiement"));
        if (rs.getTimestamp("created_at") != null)
            f.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        if (rs.getTimestamp("updated_at") != null)
            f.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return f;
    }
}