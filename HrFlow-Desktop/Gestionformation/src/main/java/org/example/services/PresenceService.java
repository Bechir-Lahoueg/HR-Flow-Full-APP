package org.example.services;

import org.example.models.Presence;
import org.example.utils.Mydb;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des présences aux sessions de formation.
 *
 * Traduit depuis le PresenceService Symfony PHP :
 *  - getPresencesBySession()          → findBySession()
 *  - savePresences()                  → savePresences() avec transaction
 *  - savePresence()                   → méthode unitaire (appelée depuis le contrôleur JavaFX)
 *  - getPresencesBySessionAndDate()   → pour filtrer par date dans l'UI
 *  - getAttendancePercentage()        → taux de présence d'un participant
 */
public class PresenceService {

    private Connection connection;

    public PresenceService() {
        this.connection = Mydb.getInstance().getConnection();
    }

    // =========================================================
    //  Équivalent de getPresencesBySession() — findBySession()
    // =========================================================

    /**
     * Retourne toutes les présences enregistrées pour une session donnée.
     * Équivalent PHP : presenceRepository->findBySession($sessionId)
     */
    public List<Presence> getPresencesBySession(int sessionId) {
        List<Presence> presences = new ArrayList<>();

        String sql = """
                SELECT pf.*
                FROM presence_formation pf
                JOIN participation_formation part ON pf.id_participation_id = part.id_participation
                WHERE part.id_session = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, sessionId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                presences.add(mapRow(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return presences;
    }

    // =========================================================
    //  Filtrage par session + date (utilisé dans le contrôleur)
    // =========================================================

    /**
     * Utilisé dans l'UI JavaFX pour pré-remplir les boutons Présent/Absent.
     */
    public List<Presence> getPresencesBySessionAndDate(int sessionId, LocalDate date) {
        List<Presence> presences = new ArrayList<>();

        String sql = """
                SELECT pf.*
                FROM presence_formation pf
                JOIN participation_formation part ON pf.id_participation_id = part.id_participation
                WHERE part.id_session = ?
                  AND pf.date_presence = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, sessionId);
            ps.setDate(2, Date.valueOf(date));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                presences.add(mapRow(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return presences;
    }

    // =========================================================
    //  Équivalent de savePresences() — avec transaction
    // =========================================================

    /**
     * Sauvegarde en lot les présences pour une date donnée.
     *
     * Équivalent PHP :
     *   savePresences(string $date, array<int,string> $presencesData)
     *
     * @param sessionId      ID de la session
     * @param date           Date de présence
     * @param presencesData  Map<idParticipation, statut>  ("Present" ou "Absent")
     */
    public void savePresences(int sessionId, LocalDate date, java.util.Map<Integer, String> presencesData) {
        String sqlFind = """
                SELECT id_presence FROM presence_formation
                WHERE id_participation_id = ? AND date_presence = ?
                """;

        String sqlUpdate = """
                UPDATE presence_formation SET statut = ?
                WHERE id_presence = ?
                """;

        String sqlInsert = """
                INSERT INTO presence_formation (id_participation_id, date_presence, statut)
                VALUES (?, ?, ?)
                """;

        // Équivalent du beginTransaction / commit / rollback Symfony
        try {
            connection.setAutoCommit(false);

            try {
                for (java.util.Map.Entry<Integer, String> entry : presencesData.entrySet()) {
                    int participationId = entry.getKey();
                    String statut = entry.getValue();

                    // Vérifier si une présence existe déjà — findOneByParticipationAndDate
                    Integer existingId = null;
                    try (PreparedStatement psFind = connection.prepareStatement(sqlFind)) {
                        psFind.setInt(1, participationId);
                        psFind.setDate(2, Date.valueOf(date));
                        ResultSet rs = psFind.executeQuery();
                        if (rs.next()) {
                            existingId = rs.getInt("id_presence");
                        }
                    }

                    if (existingId != null) {
                        // Mise à jour — existing->setStatut($statut)
                        try (PreparedStatement psUpdate = connection.prepareStatement(sqlUpdate)) {
                            psUpdate.setString(1, statut);
                            psUpdate.setInt(2, existingId);
                            psUpdate.executeUpdate();
                        }
                    } else {
                        // Insertion — new PresenceFormation()
                        try (PreparedStatement psInsert = connection.prepareStatement(sqlInsert)) {
                            psInsert.setInt(1, participationId);
                            psInsert.setDate(2, Date.valueOf(date));
                            psInsert.setString(3, statut);
                            psInsert.executeUpdate();
                        }
                    }
                }

                connection.commit(); // Équivalent em->flush() + em->commit()

            } catch (SQLException e) {
                connection.rollback(); // Équivalent em->rollback()
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    //  Version unitaire (appelée depuis le contrôleur JavaFX)
    // =========================================================

    /**
     * Sauvegarde la présence d'un seul participant pour une date.
     * Raccourci pratique utilisé dans buildPresenceRow() du contrôleur.
     *
     * Délègue à savePresences() pour réutiliser la logique transactionnelle.
     */
    public void savePresence(int sessionId, int participationId, LocalDate date, String statut) {
        savePresences(sessionId, date, java.util.Map.of(participationId, statut));
    }

    // =========================================================
    //  Calcul du taux de présence (percentage)
    // =========================================================

    /**
     * Calcule le taux de présence d'un participant (en %).
     *
     * Équivalent PHP :
     *   getAttendancePercentage(int $participationId): float
     *
     * @return pourcentage arrondi à 2 décimales, ou 0.0 si aucune donnée
     */
    public double getAttendancePercentage(int participationId) {
        String sqlTotal = "SELECT COUNT(*) FROM presence_formation WHERE id_participation_id = ?";
        String sqlPresent = "SELECT COUNT(*) FROM presence_formation WHERE id_participation_id = ? AND statut = 'Present'";

        try {

            int total = 0;
            try (PreparedStatement ps = connection.prepareStatement(sqlTotal)) {
                ps.setInt(1, participationId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) total = rs.getInt(1);
            }

            if (total == 0) return 0.0; // Équivalent PHP : if ($recordedDays === 0) return 0.0

            int present = 0;
            try (PreparedStatement ps = connection.prepareStatement(sqlPresent)) {
                ps.setInt(1, participationId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) present = rs.getInt(1);
            }

            // Équivalent PHP : round(($presentDays / $recordedDays) * 100, 2)
            double percentage = ((double) present / total) * 100.0;
            return Math.round(percentage * 100.0) / 100.0;

        } catch (SQLException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    // =========================================================
    //  Mapping ResultSet → Presence
    // =========================================================

    /**
     * Convertit une ligne SQL en objet Presence.
     * Équivalent de l'hydratation Doctrine dans Symfony.
     */
    private Presence mapRow(ResultSet rs) throws SQLException {
        Presence p = new Presence();
        p.setIdPresence(rs.getInt("id_presence"));
        p.setIdParticipation(rs.getInt("id_participation_id"));
        p.setDatePresence(rs.getDate("date_presence").toLocalDate());
        p.setStatut(rs.getString("statut"));
        return p;
    }
}


