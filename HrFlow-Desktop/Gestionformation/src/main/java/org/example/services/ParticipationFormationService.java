package org.example.services;

import org.example.utils.Mydb;
import org.example.models.ParticipationFormation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service de gestion des participations aux formations
 * Utilise le singleton Mydb pour la connexion
 */
public class ParticipationFormationService {

    private Connection connection;

    public ParticipationFormationService() {
        // Utilisation du singleton Mydb
        this.connection = Mydb.getInstance().getConnection();
    }

    // 🔹 ADD
    public void addParticipation(ParticipationFormation p) {
        String sql = "INSERT INTO participation_formation " +
                "(id_session, id_utilisateur_id, date_inscription, statut_participation, token, created_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, p.getIdSession());
            ps.setInt(2, p.getIdEmployee());
            ps.setDate(3, Date.valueOf(p.getDateInscription()));
            ps.setString(4, p.getStatutParticipation());
            // Générer un token unique pour le lien de vérification (équivalent PHP : Uuid::v4())
            String token = p.getToken() != null && !p.getToken().isEmpty()
                    ? p.getToken()
                    : UUID.randomUUID().toString();
            ps.setString(5, token);

            ps.executeUpdate();
            System.out.println("Participation ajoutée avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur ajout participation");
            e.printStackTrace();
        }
    }

    // 🔹 GET ALL
    public List<ParticipationFormation> getAllParticipations() {
        List<ParticipationFormation> participations = new ArrayList<>();
        String sql = "SELECT p.*, e.first_name, e.last_name FROM participation_formation p " +
                "JOIN employees e ON p.id_utilisateur_id = e.id";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String nomComplet = rs.getString("first_name") + " " + rs.getString("last_name");
                ParticipationFormation p = new ParticipationFormation(
                        rs.getInt("id_participation"),
                        rs.getInt("id_session"),
                        rs.getInt("id_utilisateur_id"),
                        rs.getDate("date_inscription").toLocalDate(),
                        rs.getString("statut_participation"),
                        null,
                        nomComplet
                );
                participations.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération participations");
            e.printStackTrace();
        }
        return participations;
    }

    // 🔹 GET BY ID
    public ParticipationFormation getParticipationById(int id) {
        String sql = "SELECT * FROM participation_formation WHERE id_participation = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new ParticipationFormation(
                        rs.getInt("id_participation"),
                        rs.getInt("id_session"),
                        rs.getInt("id_utilisateur_id"),
                        rs.getDate("date_inscription").toLocalDate(),
                        rs.getString("statut_participation"),
                        null
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération participation par ID");
            e.printStackTrace();
        }
        return null;
    }

    // 🔹 GET BY SESSION
    public List<ParticipationFormation> getParticipationsBySession(int idSession) {
        List<ParticipationFormation> list = new ArrayList<>();
        String sql = "SELECT p.*, e.first_name, e.last_name FROM participation_formation p " +
                "JOIN employees e ON p.id_utilisateur_id = e.id " +
                "WHERE p.id_session = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idSession);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String nomComplet = rs.getString("first_name") + " " + rs.getString("last_name");

                list.add(new ParticipationFormation(
                        rs.getInt("id_participation"),
                        rs.getInt("id_session"),
                        rs.getInt("id_utilisateur_id"),
                        rs.getDate("date_inscription").toLocalDate(),
                        rs.getString("statut_participation"),
                        null,
                        nomComplet
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL dans getParticipationsBySession");
            e.printStackTrace();
        }
        return list;
    }

    // 🔹 UPDATE
    public void updateParticipation(ParticipationFormation p) {
        String sql = "UPDATE participation_formation SET " +
                "statut_participation = ?, resultat = ? " +
                "WHERE id_participation = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getStatutParticipation());
            ps.setString(2, p.getResultat());
            ps.setInt(3, p.getIdParticipation());

            ps.executeUpdate();
            System.out.println("Participation modifiée avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur modification participation");
            e.printStackTrace();
        }
    }

    /**
     * Récupère les participations pour un RH avec filtres
     * @param rhId ID du RH
     * @param statusFilter filtre statut ("" = tous, "Pending", "Approved", "Rejected")
     * @param formationId filtre formation (null = toutes)
     * @param priorityOnly si true, retourne seulement les prioritaires
     * @return Liste filtrée des participations
     */
    public List<ParticipationFormation> getRhParticipations(int rhId, String statusFilter, Integer formationId, boolean priorityOnly) {
        List<ParticipationFormation> participations = new ArrayList<>();

        String sql = "SELECT p.*, e.first_name, e.last_name FROM participation_formation p " +
                "JOIN session_formation sf ON p.id_session = sf.id_session " +
                "JOIN formation f ON sf.id_formation_id = f.id_formation " +
                "JOIN employees e ON p.id_utilisateur_id = e.id " +
                "WHERE f.id_rh = ?";

        // Ajouter les filtres dynamiquement
        if (statusFilter != null && !statusFilter.isEmpty()) {
            sql += " AND p.statut_participation = ?";
        }
        if (formationId != null) {
            sql += " AND f.id_formation = ?";
        }

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            ps.setInt(index++, rhId);

            if (statusFilter != null && !statusFilter.isEmpty()) {
                ps.setString(index++, statusFilter);
            }
            if (formationId != null) {
                ps.setInt(index++, formationId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nomComplet = rs.getString("first_name") + " " + rs.getString("last_name");
                    ParticipationFormation p = new ParticipationFormation(
                            rs.getInt("id_participation"),
                            rs.getInt("id_session"),
                            rs.getInt("id_utilisateur_id"),
                            rs.getDate("date_inscription").toLocalDate(),
                            rs.getString("statut_participation"),
                            null,
                            nomComplet
                    );

                    // Optionnel: charger la priorité si la colonne existe
                    try {
                        p.setPriority(rs.getBoolean("priority"));
                    } catch (SQLException ignored) {
                        // Colonne priority peut ne pas exister dans la version actuelle
                    }

                    if (!priorityOnly || p.isPriority()) {
                        participations.add(p);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération participations RH");
            e.printStackTrace();
        }

        return participations;
    }

    /**
     * Approuve une participation avec gestion de priorité
     * @param participationId ID de la participation
     * @return Map contenant "ok" (booléen) et "message" (String)
     */
    public java.util.Map<String, Object> approveWithPriority(int participationId) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        try {
            ParticipationFormation participation = getParticipationById(participationId);
            if (participation == null) {
                result.put("ok", false);
                result.put("message", "Participation non trouvée");
                return result;
            }

            participation.setStatutParticipation("Accepte");
            updateParticipation(participation);

            result.put("ok", true);
            result.put("message", "Participant accepté avec succès");
        } catch (Exception e) {
            result.put("ok", false);
            result.put("message", "Erreur lors de l'approbation : " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Met à jour le statut d'une participation avec motif optionnel
     * @param participationId ID de la participation
     * @param status nouveau statut
     * @param reason motif de refus (optionnel)
     */
    public void updateStatus(int participationId, String status, String reason) {
        String sql = "UPDATE participation_formation SET statut_participation = ?, resultat = ? WHERE id_participation = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, reason);
            ps.setInt(3, participationId);

            ps.executeUpdate();
            System.out.println("Statut participation mis à jour");
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour statut participation");
            e.printStackTrace();
        }
    }

    // 🔹 DELETE
    public void deleteParticipation(int idParticipation) {
        String sql = "DELETE FROM participation_formation WHERE id_participation = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idParticipation);
            ps.executeUpdate();
            System.out.println("Participation supprimée");
        } catch (SQLException e) {
            System.err.println("Erreur suppression participation");
            e.printStackTrace();
        }
    }
}

