package org.example.services;

import org.example.utils.Mydb;

import java.sql.*;
import java.util.*;

/**
 * Service de gestion des feedbacks/avis sur les formations et sessions.
 * 
 * Fournit :
 * - Récupération des feedbacks par formation
 * - Calcul des moyennes de rating
 * - Gestion des avis utilisateurs
 */
public class SessionFeedbackService {

    private Connection connection;

    public SessionFeedbackService() {
        this.connection = Mydb.getInstance().getConnection();
    }

    /**
     * Récupère tous les feedbacks pour une formation
     * @param formationId ID de la formation
     * @return Liste des feedbacks
     */
    public List<?> getFeedbacksByFormation(int formationId) {
        List<Object> feedbacks = new ArrayList<>();
        
        String sql = "SELECT * FROM feedback_formation WHERE formation_id = ? ORDER BY created_at DESC";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, formationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Pour l'instant, retourner un simple objet avec les données
                    Map<String, Object> feedback = new HashMap<>();
                    feedback.put("id", rs.getInt("id"));
                    feedback.put("formationId", rs.getInt("formation_id"));
                    feedback.put("rating", rs.getInt("rating"));
                    feedback.put("comment", rs.getString("contenu_comment"));
                    feedback.put("createdAt", rs.getTimestamp("created_at"));
                    feedbacks.add(feedback);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération feedbacks pour formation " + formationId);
            e.printStackTrace();
        }
        
        return feedbacks;
    }

    /**
     * Calcule la moyenne de rating pour une formation
     * @param formationId ID de la formation
     * @return moyenne de rating (0 si aucun feedback)
     */
    public double getAverageRating(int formationId) {
        String sql = "SELECT AVG(rating) as avg_rating FROM feedback_formation WHERE formation_id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, formationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble("avg_rating");
                    return Double.isNaN(avg) ? 0.0 : Math.round(avg * 10.0) / 10.0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur calcul moyenne rating pour formation " + formationId);
            e.printStackTrace();
        }
        
        return 0.0;
    }

    /**
     * Récupère les moyennes de rating pour plusieurs formations
     * @param formationIds liste des IDs de formations
     * @return Map<formationId, moyenneRating>
     */
    public Map<Integer, Double> getAverageRatingsByFormationIds(List<Integer> formationIds) {
        Map<Integer, Double> ratingMap = new HashMap<>();
        
        if (formationIds == null || formationIds.isEmpty()) {
            return ratingMap;
        }
        
        // Construire la requête avec IN clause
        String placeholders = String.join(",", Collections.nCopies(formationIds.size(), "?"));
        String sql = "SELECT formation_id, AVG(rating) as avg_rating FROM feedback_formation " +
                    "WHERE formation_id IN (" + placeholders + ") " +
                    "GROUP BY formation_id";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < formationIds.size(); i++) {
                ps.setInt(i + 1, formationIds.get(i));
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int formationId = rs.getInt("formation_id");
                    double avgRating = rs.getDouble("avg_rating");
                    ratingMap.put(formationId, Double.isNaN(avgRating) ? 0.0 : Math.round(avgRating * 10.0) / 10.0);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur calcul moyennes ratings");
            e.printStackTrace();
        }
        
        return ratingMap;
    }

    /**
     * Ajoute un feedback pour une formation
     * @param formationId ID de la formation
     * @param rating note de 1 à 5
     * @param comment commentaire optionnel
     */
    public void addFeedback(int formationId, int rating, String comment) {
        String sql = "INSERT INTO feedback_formation (formation_id, rating, contenu_comment) VALUES (?, ?, ?)";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, formationId);
            ps.setInt(2, Math.max(1, Math.min(5, rating))); // Limiter entre 1 et 5
            ps.setString(3, comment);
            ps.executeUpdate();
            System.out.println("Feedback ajouté pour formation " + formationId);
        } catch (SQLException e) {
            System.err.println("Erreur ajout feedback");
            e.printStackTrace();
        }
    }

    /**
     * Récupère le nombre de feedbacks pour une formation
     * @param formationId ID de la formation
     * @return nombre de feedbacks
     */
    public int getFeedbackCount(int formationId) {
        String sql = "SELECT COUNT(*) as count FROM feedback_formation WHERE formation_id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, formationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur calcul nombre feedbacks");
            e.printStackTrace();
        }
        
        return 0;
    }

    /**
     * Récupère la distribution des ratings pour une formation (ex: 3 avis 5*, 2 avis 4*, etc.)
     * @param formationId ID de la formation
     * @return Map<rating, count>
     */
    public Map<Integer, Integer> getRatingDistribution(int formationId) {
        Map<Integer, Integer> distribution = new HashMap<>();
        
        // Initialiser avec 0 pour chaque rating
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0);
        }
        
        String sql = "SELECT rating, COUNT(*) as count FROM feedback_formation WHERE formation_id = ? GROUP BY rating";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, formationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int rating = rs.getInt("rating");
                    int count = rs.getInt("count");
                    distribution.put(rating, count);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur calcul distribution ratings");
            e.printStackTrace();
        }
        
        return distribution;
    }
}
