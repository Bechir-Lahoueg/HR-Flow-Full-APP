package org.example.services;

import org.example.utils.Mydb;
import org.example.models.Formation;
import org.example.models.FeedbackFormation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;

public class FormationService {

    private Connection connection;

    private FeedbackFormationService feedbackService = new FeedbackFormationService();

    public FormationService() {
        // Utilisation du singleton Mydb
        this.connection = Mydb.getInstance().getConnection();
    }

    public void addFormation(Formation f) {
        String sql = "INSERT INTO formation (titre, description, type, duree, organisme, objectifs, id_rh) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, f.getTitre());
            ps.setString(2, f.getDescription());
            ps.setString(3, f.getType());
            ps.setInt(4, f.getDuree());
            ps.setString(5, f.getOrganisme());
            ps.setString(6, f.getObjectifs());
            ps.setObject(7, f.getIdRh());
            ps.executeUpdate();
            System.out.println("Formation ajoutée");
        } catch (SQLException e) {
            System.err.println("Erreur d'ajout de formation");
            e.printStackTrace();
        }
    }

    public List<Formation> getAllFormations() {
        List<Formation> formations = new ArrayList<>();
        String sql = "SELECT * FROM formation";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Formation f = new Formation();
                f.setIdFormation(rs.getInt("id_formation"));
                f.setTitre(rs.getString("titre"));
                f.setDescription(rs.getString("description"));
                f.setType(rs.getString("type"));
                f.setDuree(rs.getInt("duree"));
                f.setOrganisme(rs.getString("organisme"));
                f.setObjectifs(rs.getString("objectifs"));
                f.setImage(rs.getString("image"));
                f.setIdRh((Integer) rs.getObject("id_rh"));
                if (rs.getTimestamp("created_at") != null) {
                    f.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
                if (rs.getTimestamp("updated_at") != null) {
                    f.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }

                double moyenne = feedbackService.getAverageRating(f.getIdFormation());
                f.setMoyenneRating(moyenne);

                formations.add(f);
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération formations");
            e.printStackTrace();
        }
        return formations;
    }

    // Nouvelle méthode
    public Formation getFormationById(int idFormation) {
        for (Formation f : getAllFormations()) {
            if (f.getIdFormation() == idFormation) {
                return f;
            }
        }
        return null;
    }
    /**
     * Récupère les formations créées par un RH spécifique
     * @param idRh ID du RH
     * @return Liste des formations du RH
     */
    public List<Formation> getFormationsByRh(Integer idRh) {
        List<Formation> formations = new ArrayList<>();
        String sql = "SELECT * FROM formation WHERE id_rh = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idRh);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Formation f = new Formation();
                f.setIdFormation(rs.getInt("id_formation"));
                f.setTitre(rs.getString("titre"));
                f.setDescription(rs.getString("description"));
                f.setType(rs.getString("type"));
                f.setDuree(rs.getInt("duree"));
                f.setOrganisme(rs.getString("organisme"));
                f.setObjectifs(rs.getString("objectifs"));
                f.setImage(rs.getString("image"));
                f.setIdRh((Integer) rs.getObject("id_rh"));
                if (rs.getTimestamp("created_at") != null) {
                    f.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
                if (rs.getTimestamp("updated_at") != null) {
                    f.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }

                double moyenne = feedbackService.getAverageRating(f.getIdFormation());
                f.setMoyenneRating(moyenne);

                formations.add(f);
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération formations RH");
            e.printStackTrace();
        }
        return formations;
    }


    public void updateFormation(Formation f) {
        String sql = "UPDATE formation SET titre=?, description=?, type=?, duree=?, organisme=?, objectifs=?, id_rh=? WHERE id_formation=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, f.getTitre());
            ps.setString(2, f.getDescription());
            ps.setString(3, f.getType());
            ps.setInt(4, f.getDuree());
            ps.setString(5, f.getOrganisme());
            ps.setString(6, f.getObjectifs());
            ps.setObject(7, f.getIdRh());
            ps.setInt(8, f.getIdFormation());
            ps.executeUpdate();
            System.out.println("Formation modifiée");
        } catch (SQLException e) {
            System.err.println("Erreur modification formation");
            e.printStackTrace();
        }
    }

    public void deleteFormation(int id) {
        // 1. On prépare les requêtes pour nettoyer les tables liées
        // Note : si tu as aussi des 'sessions' liées à cette formation, il faudra les supprimer aussi ici !
        String deleteFeedbacksSql = "DELETE FROM feedback_formation WHERE formation_id=?";
        String deleteFormationSql = "DELETE FROM formation WHERE id_formation=?";

        try {
            // Optionnel mais recommandé : Utiliser une transaction pour tout supprimer d'un coup
            connection.setAutoCommit(false);

            // Supprimer les feedbacks d'abord
            try (PreparedStatement psFeed = connection.prepareStatement(deleteFeedbacksSql)) {
                psFeed.setInt(1, id);
                psFeed.executeUpdate();
            }

            // Enfin, supprimer la formation
            try (PreparedStatement psForm = connection.prepareStatement(deleteFormationSql)) {
                psForm.setInt(1, id);
                psForm.executeUpdate();
            }

            connection.commit();
            System.out.println("Formation et ses feedbacks supprimés avec succès");

        } catch (SQLException e) {
            try {
                connection.rollback(); // Annule tout si une étape échoue
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("Erreur lors de la suppression de la formation");
        }
    }

    /**
     * Récupère les formations d'un RH avec filtres et tri
     * @param rhId ID du RH
     * @param search Texte de recherche (titre ou description)
     * @param type Filtre sur le type
     * @param sort Champ de tri ("titre", "duree", "type")
     * @return Liste filtrée et triée
     */
    public List<Formation> getFormationsByRhFiltered(int rhId, String search, String type, String sort) {
        List<Formation> formations = getFormationsByRh(rhId);

        // Filtrer par recherche
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            formations = formations.stream()
                    .filter(f -> f.getTitre().toLowerCase().contains(searchLower) ||
                            f.getDescription().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }

        // Filtrer par type
        if (type != null && !type.isEmpty()) {
            formations = formations.stream()
                    .filter(f -> f.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        // Trier
        if (sort != null && !sort.isEmpty()) {
            switch (sort) {
                case "Titre A→Z":
                    formations.sort(Comparator.comparing(Formation::getTitre));
                    break;
                case "Titre Z→A":
                    formations.sort(Comparator.comparing(Formation::getTitre).reversed());
                    break;
                case "Durée croissante":
                    formations.sort(Comparator.comparing(Formation::getDuree));
                    break;
                case "Durée décroissante":
                    formations.sort(Comparator.comparing(Formation::getDuree).reversed());
                    break;
                case "Mieux notées":
                    formations.sort(Comparator.comparing(Formation::getMoyenneRating).reversed());
                    break;
            }
        }

        return formations;
    }

    /**
     * Récupère les top formations et top formateurs pour un RH
     * @param rhId ID du RH
     * @return Map contenant "topFormation" et "topFormateur"
     */
    public java.util.Map<String, Object> getTopInsightsByRhId(int rhId) {
        java.util.Map<String, Object> insights = new java.util.HashMap<>();
        List<Formation> formations = getFormationsByRh(rhId);

        if (formations.isEmpty()) {
            insights.put("topFormation", "N/A");
            insights.put("topFormateur", "N/A");
            return insights;
        }

        // Top formation (by average rating)
        Formation topFormation = formations.stream()
                .max(Comparator.comparing(Formation::getMoyenneRating))
                .orElse(formations.get(0));
        insights.put("topFormation", topFormation.getTitre() + " (" + topFormation.getMoyenneRating() + "/5)");

        // Top organisme (most common)
        String topOrganisme = formations.stream()
                .collect(Collectors.groupingBy(Formation::getOrganisme, Collectors.counting()))
                .entrySet().stream()
                .max(Comparator.comparing(java.util.Map.Entry::getValue))
                .map(java.util.Map.Entry::getKey)
                .orElse("N/A");
        insights.put("topFormateur", topOrganisme);

        return insights;
    }

    /**
     * Supprime une formation avec toutes ses relations (sessions, participations, feedbacks, présences)
     * @param idFormation ID de la formation à supprimer
     */
    public void deleteFormationWithRelations(int idFormation) {
        try {
            connection.setAutoCommit(false);

            // Supprimer les présences des sessions
            String deletePresencesSql = "DELETE FROM presence_formation WHERE id_participation IN " +
                    "(SELECT id_participation FROM participation_formation WHERE id_session IN " +
                    "(SELECT id_session FROM session_formation WHERE id_formation = ?))";
            try (PreparedStatement ps = connection.prepareStatement(deletePresencesSql)) {
                ps.setInt(1, idFormation);
                ps.executeUpdate();
            }

            // Supprimer les participations des sessions
            String deleteParticipationsSql = "DELETE FROM participation_formation WHERE id_session IN " +
                    "(SELECT id_session FROM session_formation WHERE id_formation = ?)";
            try (PreparedStatement ps = connection.prepareStatement(deleteParticipationsSql)) {
                ps.setInt(1, idFormation);
                ps.executeUpdate();
            }

            // Supprimer les sessions
            String deleteSessionsSql = "DELETE FROM session_formation WHERE id_formation = ?";
            try (PreparedStatement ps = connection.prepareStatement(deleteSessionsSql)) {
                ps.setInt(1, idFormation);
                ps.executeUpdate();
            }

            // Supprimer les feedbacks
            String deleteFeedbacksSql = "DELETE FROM feedback_formation WHERE formation_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(deleteFeedbacksSql)) {
                ps.setInt(1, idFormation);
                ps.executeUpdate();
            }

            // Supprimer la formation
            String deleteFormationSql = "DELETE FROM formation WHERE id_formation = ?";
            try (PreparedStatement ps = connection.prepareStatement(deleteFormationSql)) {
                ps.setInt(1, idFormation);
                ps.executeUpdate();
            }

            connection.commit();
            System.out.println("Formation supprimée avec toutes ses relations");
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("Erreur lors de la suppression complète de la formation");
            e.printStackTrace();
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
