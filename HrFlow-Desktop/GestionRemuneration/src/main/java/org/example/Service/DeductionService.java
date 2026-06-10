package org.example.Service;

import org.example.Entity.Deduction;
import org.example.Enum.DeductionType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Service CRUD pour la table `deductions`.
 * Table et colonnes alignées avec le modèle Symfony / web :
 *   id | employee_id | type_deduction | montant | date_deduction
 *   motif | created_at | updated_at
 */
public class DeductionService {

    private final Connection connection;

    public DeductionService(Connection connection) {
        this.connection = connection;
    }

    // ==============================
    // VALIDATION
    // ==============================

    private void validateDeduction(Deduction deduction) {

        if (deduction == null) {
            throw new IllegalArgumentException("La déduction ne peut pas être null.");
        }

        if (deduction.getTypeDeduction() == null) {
            throw new IllegalArgumentException("Le type de déduction est obligatoire.");
        }

        if (deduction.getMontant() == null) {
            throw new IllegalArgumentException("Le montant est obligatoire.");
        }

        if (deduction.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à 0.");
        }

        if (deduction.getDateDeduction() == null) {
            throw new IllegalArgumentException("La date de déduction est obligatoire.");
        }

        if (deduction.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("ID employé invalide.");
        }
    }

    // ==============================
    // CREATE
    // ==============================

    public void addDeduction(Deduction deduction) throws SQLException {

        validateDeduction(deduction);

        String sql = "INSERT INTO deductions (employee_id, type_deduction, montant, date_deduction, motif, created_at) "
                + "VALUES (?, ?, ?, ?, ?, NOW())";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, deduction.getEmployeeId());
            stmt.setString(2, deduction.getTypeDeduction().value());
            stmt.setBigDecimal(3, deduction.getMontant());
            stmt.setDate(4, Date.valueOf(deduction.getDateDeduction()));
            stmt.setString(5, deduction.getMotif());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                deduction.setId(rs.getInt(1));
            }
        }
    }

    // ==============================
    // READ BY ID
    // ==============================

    public Deduction getDeductionById(int id) throws SQLException {

        String sql = "SELECT * FROM deductions WHERE id = ?";

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

    public List<Deduction> getAllDeductions() throws SQLException {

        String sql = "SELECT * FROM deductions ORDER BY date_deduction DESC";
        List<Deduction> deductions = new ArrayList<>();

        try (Statement stmt = connection.createStatement()) {

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                deductions.add(mapResultSet(rs));
            }
        }

        return deductions;
    }

    // ==============================
    // READ BY EMPLOYE
    // ==============================

    public List<Deduction> getDeductionsByEmployee(int employeeId) throws SQLException {
        String sql = "SELECT * FROM deductions WHERE employee_id = ? ORDER BY date_deduction DESC";
        List<Deduction> deductions = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) deductions.add(mapResultSet(rs));
        }
        return deductions;
    }

    /** Récupère les déductions d'un employé pour une période (mois + année). */
    public List<Deduction> getDeductionsByEmployeeAndPeriod(int employeeId, int mois, int annee) throws SQLException {
        String sql = "SELECT * FROM deductions WHERE employee_id = ? "
                + "AND MONTH(date_deduction) = ? AND YEAR(date_deduction) = ?";
        List<Deduction> deductions = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            stmt.setInt(2, mois);
            stmt.setInt(3, annee);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) deductions.add(mapResultSet(rs));
        }
        return deductions;
    }

    // ==============================
    // UPDATE
    // ==============================

    public void updateDeduction(Deduction deduction) throws SQLException {

        validateDeduction(deduction);

        String sql = "UPDATE deductions SET employee_id=?, type_deduction=?, montant=?, "
                + "date_deduction=?, motif=?, updated_at=NOW() WHERE id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, deduction.getEmployeeId());
            stmt.setString(2, deduction.getTypeDeduction().value());
            stmt.setBigDecimal(3, deduction.getMontant());
            stmt.setDate(4, Date.valueOf(deduction.getDateDeduction()));
            stmt.setString(5, deduction.getMotif());
            stmt.setInt(6, deduction.getId());

            stmt.executeUpdate();
        }
    }

    // ==============================
    // DELETE
    // ==============================

    public void deleteDeduction(int id) throws SQLException {

        String sql = "DELETE FROM deductions WHERE id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ==============================
    // MAPPING
    // ==============================

    private Deduction mapResultSet(ResultSet rs) throws SQLException {

        Date sqlDate = rs.getDate("date_deduction");
        LocalDate date = sqlDate != null ? sqlDate.toLocalDate() : null;

        Deduction d = new Deduction(
                rs.getInt("id"),
                DeductionType.fromLabel(rs.getString("type_deduction")),
                rs.getBigDecimal("montant"),
                date,
                rs.getInt("employee_id")
        );
        d.setMotif(rs.getString("motif"));
        if (rs.getTimestamp("created_at") != null)
            d.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        if (rs.getTimestamp("updated_at") != null)
            d.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return d;
    }
}