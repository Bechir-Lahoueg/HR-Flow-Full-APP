package org.example.Service;

import org.example.Entity.Prime;
import org.example.Enum.PrimeType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Service CRUD pour la table `primes`.
 * Table et colonnes alignées avec le modèle Symfony / web :
 *   id | employee_id | type_prime | montant | date_attribution
 *   motif | created_at | updated_at
 */
public class PrimeService {

    private final Connection connection;

    public PrimeService(Connection connection) {
        this.connection = connection;
    }

    // ==============================
    // VALIDATION
    // ==============================

    private void validatePrime(Prime prime) {

        if (prime == null) {
            throw new IllegalArgumentException("La prime ne peut pas être null.");
        }

        if (prime.getTypePrime() == null) {
            throw new IllegalArgumentException("Le type de prime est obligatoire.");
        }

        if (prime.getMontant() == null) {
            throw new IllegalArgumentException("Le montant est obligatoire.");
        }

        if (prime.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à 0.");
        }

        if (prime.getDateAttribution() == null) {
            throw new IllegalArgumentException("La date d'attribution est obligatoire.");
        }

        if (prime.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("ID employé invalide.");
        }
    }

    // ==============================
    // CREATE
    // ==============================

    public void addPrime(Prime prime) throws SQLException {

        validatePrime(prime);

        String sql = "INSERT INTO primes (employee_id, type_prime, montant, date_attribution, motif, created_at) "
                + "VALUES (?, ?, ?, ?, ?, NOW())";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, prime.getEmployeeId());
            stmt.setString(2, prime.getTypePrime().value());
            stmt.setBigDecimal(3, prime.getMontant());
            stmt.setDate(4, Date.valueOf(prime.getDateAttribution()));
            stmt.setString(5, prime.getMotif());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                prime.setId(rs.getInt(1));
            }
        }
    }

    // ==============================
    // READ BY ID
    // ==============================

    public Prime getPrimeById(int id) throws SQLException {

        String sql = "SELECT * FROM primes WHERE id = ?";

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

    public List<Prime> getAllPrimes() throws SQLException {

        String sql = "SELECT * FROM primes ORDER BY date_attribution DESC";
        List<Prime> primes = new ArrayList<>();

        try (Statement stmt = connection.createStatement()) {

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                primes.add(mapResultSet(rs));
            }
        }

        return primes;
    }

    // ==============================
    // READ BY EMPLOYE
    // ==============================

    public List<Prime> getPrimesByEmployee(int employeeId) throws SQLException {
        String sql = "SELECT * FROM primes WHERE employee_id = ? ORDER BY date_attribution DESC";
        List<Prime> primes = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) primes.add(mapResultSet(rs));
        }
        return primes;
    }

    /** Récupère les primes d'un employé pour une période (mois + année). */
    public List<Prime> getPrimesByEmployeeAndPeriod(int employeeId, int mois, int annee) throws SQLException {
        String sql = "SELECT * FROM primes WHERE employee_id = ? "
                + "AND MONTH(date_attribution) = ? AND YEAR(date_attribution) = ?";
        List<Prime> primes = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            stmt.setInt(2, mois);
            stmt.setInt(3, annee);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) primes.add(mapResultSet(rs));
        }
        return primes;
    }

    // ==============================
    // UPDATE
    // ==============================

    public void updatePrime(Prime prime) throws SQLException {

        validatePrime(prime);

        String sql = "UPDATE primes SET employee_id=?, type_prime=?, montant=?, "
                + "date_attribution=?, motif=?, updated_at=NOW() WHERE id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, prime.getEmployeeId());
            stmt.setString(2, prime.getTypePrime().value());
            stmt.setBigDecimal(3, prime.getMontant());
            stmt.setDate(4, Date.valueOf(prime.getDateAttribution()));
            stmt.setString(5, prime.getMotif());
            stmt.setInt(6, prime.getId());

            stmt.executeUpdate();
        }
    }

    // ==============================
    // DELETE
    // ==============================

    public void deletePrime(int id) throws SQLException {

        String sql = "DELETE FROM primes WHERE id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ==============================
    // MAPPING
    // ==============================

    private Prime mapResultSet(ResultSet rs) throws SQLException {

        Date sqlDate = rs.getDate("date_attribution");
        LocalDate date = sqlDate != null ? sqlDate.toLocalDate() : null;

        Prime p = new Prime(
                rs.getInt("id"),
                PrimeType.fromLabel(rs.getString("type_prime")),
                rs.getBigDecimal("montant"),
                date,
                rs.getInt("employee_id")
        );
        p.setMotif(rs.getString("motif"));
        if (rs.getTimestamp("created_at") != null)
            p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        if (rs.getTimestamp("updated_at") != null)
            p.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return p;
    }
}