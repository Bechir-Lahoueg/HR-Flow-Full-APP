package org.example.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de notifications de congés — stockage purement en mémoire.
 * La table leave_notifications (créée côté web) a un schéma différent ;
 * toutes les opérations DB ont été retirées pour éviter les erreurs SQL.
 * Les notifications sont gérées en mémoire via InAppNotificationService.
 */
public class LeaveNotificationService {

    public LeaveNotificationService() {
        // pas d'init DB
    }

    /** No-op : la persistance est gérée en mémoire par InAppNotificationService. */
    public void saveNotification(int employeeId, String message, String type) {
        // intentionnellement vide
    }

    /** Retourne une liste vide — les notifications viennent de InAppNotificationService. */
    public List<NotificationRecord> getNotifications(int employeeId) {
        return new ArrayList<>();
    }

    /** Retourne 0 — le compteur est géré par InAppNotificationService. */
    public long countUnread(int employeeId) {
        return 0L;
    }

    /** No-op. */
    public void markAllRead(int employeeId) {
        // intentionnellement vide
    }

    // ─── DTO interne ─────────────────────────────────────────────────────────────

    public static class NotificationRecord {
        public final int           id;
        public final String        message;
        public final String        type;
        public final LocalDateTime createdAt;
        public final boolean       read;

        public NotificationRecord(int id, String message, String type,
                                  LocalDateTime createdAt, boolean read) {
            this.id        = id;
            this.message   = message;
            this.type      = type;
            this.createdAt = createdAt;
            this.read      = read;
        }
    }
}
