package org.example.services;

import org.example.models.Formation;
import org.example.models.SessionFormation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service de notification des changements de formations et sessions.
 * 
 * Gère :
 * - Notifications lors de modifications/suppressions de formations
 * - Notifications lors de modifications/suppressions de sessions
 * - Listeners pour les événements de changement
 * 
 * Ce service prépare l'infrastructure pour les événements, prêt pour intégration
 * avec un système de notification (email, SMS, push, etc.).
 */
public class FormationChangeNotificationService {

    private List<Consumer<NotificationEvent>> listeners = new ArrayList<>();

    /**
     * Type d'événement de notification
     */
    public enum EventType {
        FORMATION_CREATED,
        FORMATION_UPDATED,
        FORMATION_DELETED,
        SESSION_CREATED,
        SESSION_UPDATED,
        SESSION_DELETED,
        PARTICIPATION_APPROVED,
        PARTICIPATION_REJECTED
    }

    /**
     * Classe interne représentant un événement de notification
     */
    public static class NotificationEvent {
        public EventType type;
        public Object entity;
        public long timestamp;
        public String message;

        public NotificationEvent(EventType type, Object entity, String message) {
            this.type = type;
            this.entity = entity;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Enregistre un listener pour les événements de notification
     * 
     * @param listener le listener à enregistrer
     */
    public void addListener(Consumer<NotificationEvent> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Notifie tous les listeners d'un événement
     * 
     * @param event l'événement à notifier
     */
    private void notifyListeners(NotificationEvent event) {
        for (Consumer<NotificationEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                System.err.println("Erreur lors de la notification : " + e.getMessage());
            }
        }
    }

    /**
     * Notifie que une formation a été modifiée
     * 
     * @param formation la formation modifiée
     * @return nombre de notifications envoyées
     */
    public int notifyFormationUpdated(Formation formation) {
        if (formation == null) return 0;
        
        NotificationEvent event = new NotificationEvent(
            EventType.FORMATION_UPDATED,
            formation,
            "La formation '" + formation.getTitre() + "' a été mise à jour"
        );
        
        notifyListeners(event);
        System.out.println("[NOTIFICATION] Formation mise à jour : " + formation.getTitre());
        
        // Retourne 1 pour signifier qu'une notification a été traitée
        return 1;
    }

    /**
     * Notifie que une formation a été supprimée
     * 
     * @param formation la formation supprimée
     * @return nombre de notifications envoyées
     */
    public int notifyFormationDeleted(Formation formation) {
        if (formation == null) return 0;
        
        NotificationEvent event = new NotificationEvent(
            EventType.FORMATION_DELETED,
            formation,
            "La formation '" + formation.getTitre() + "' a été supprimée"
        );
        
        notifyListeners(event);
        System.out.println("[NOTIFICATION] Formation supprimée : " + formation.getTitre());
        
        return 1;
    }

    /**
     * Notifie que une formation a été créée
     * 
     * @param formation la formation créée
     * @return nombre de notifications envoyées
     */
    public int notifyFormationCreated(Formation formation) {
        if (formation == null) return 0;
        
        NotificationEvent event = new NotificationEvent(
            EventType.FORMATION_CREATED,
            formation,
            "Une nouvelle formation '" + formation.getTitre() + "' a été créée"
        );
        
        notifyListeners(event);
        System.out.println("[NOTIFICATION] Formation créée : " + formation.getTitre());
        
        return 1;
    }

    /**
     * Notifie que une session a été modifiée
     * 
     * @param session la session modifiée
     * @return nombre de notifications envoyées
     */
    public int notifySessionUpdated(SessionFormation session) {
        if (session == null) return 0;
        
        NotificationEvent event = new NotificationEvent(
            EventType.SESSION_UPDATED,
            session,
            "Une session de formation a été mise à jour (ID: " + session.getIdSession() + ")"
        );
        
        notifyListeners(event);
        System.out.println("[NOTIFICATION] Session mise à jour : " + session.getIdSession());
        
        return 1;
    }

    /**
     * Notifie que une session a été supprimée
     * 
     * @param session la session supprimée
     * @return nombre de notifications envoyées
     */
    public int notifySessionDeleted(SessionFormation session) {
        if (session == null) return 0;
        
        NotificationEvent event = new NotificationEvent(
            EventType.SESSION_DELETED,
            session,
            "Une session de formation a été supprimée (ID: " + session.getIdSession() + ")"
        );
        
        notifyListeners(event);
        System.out.println("[NOTIFICATION] Session supprimée : " + session.getIdSession());
        
        return 1;
    }

    /**
     * Notifie que une session a été créée
     * 
     * @param session la session créée
     * @return nombre de notifications envoyées
     */
    public int notifySessionCreated(SessionFormation session) {
        if (session == null) return 0;
        
        NotificationEvent event = new NotificationEvent(
            EventType.SESSION_CREATED,
            session,
            "Une nouvelle session de formation a été créée (ID: " + session.getIdSession() + ")"
        );
        
        notifyListeners(event);
        System.out.println("[NOTIFICATION] Session créée : " + session.getIdSession());
        
        return 1;
    }
}

