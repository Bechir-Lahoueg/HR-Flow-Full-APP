package service;

import models.Project;
import models.ProjectTask;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service de notifications par email via Gmail SMTP.
 * Utilise le compte hr.flow28@gmail.com (app password).
 */
public class EmailNotificationService {

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURATION GMAIL SMTP
    // ═══════════════════════════════════════════════════════════════

    private static final String SMTP_HOST     = "smtp.gmail.com";
    private static final int    SMTP_PORT     = 587;
    private static final String FROM_EMAIL    = "hr.flow28@gmail.com";
    private static final String SMTP_PASSWORD = "your_gmail_app_password_here";
    private static final String FROM_NAME     = "RH Flow Platform";

    // ═══════════════════════════════════════════════════════════════
    // MÉTHODES D'ENVOI
    // ═══════════════════════════════════════════════════════════════

    /**
     * Envoie un email via Gmail SMTP (STARTTLS).
     */
    private boolean sendEmail(String toEmail, String toName, String subject, String htmlBody) {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, SMTP_PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail, toName));
            message.setSubject(subject, "UTF-8");
            message.setContent(htmlBody, "text/html; charset=UTF-8");

            Transport.send(message);
            System.out.println("✅ Email envoyé via Gmail SMTP à " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi Gmail: " + e.getMessage());
            return false;
        }
    }
    public boolean sendTestEmail(String toEmail, String toName) {
        String subject = "✅ Test Email - RH Flow";

        String htmlBody = buildEmailTemplate(
                "Test de Configuration Email",
                toName,
                "<div style='background-color: #e8f5e9; padding: 20px; border-left: 4px solid #2ecc71; margin: 20px 0;'>" +
                        "<h2 style='color: #27ae60;'>✅ Email envoyé avec succès !</h2>" +
                        "<p>Votre configuration email fonctionne correctement.</p>" +
                        "<p>Le système de notification RH Flow est opérationnel.</p>" +
                        "</div>"
        );

        return sendEmail(toEmail, toName, subject, htmlBody);
    }
    // ═══════════════════════════════════════════════════════════════
    // NOTIFICATIONS - TÂCHES EN RETARD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Notifie un employé qu'une tâche est en retard
     */
    public boolean notifyTaskOverdue(ProjectTask task, String employeeEmail, String employeeName,
                                     String projectName) {
        long daysOverdue = ChronoUnit.DAYS.between(task.getDueDate().toLocalDate(), LocalDate.now());

        String subject = "🔴 URGENT: Tâche en retard - " + task.getTitle();

        String htmlBody = buildEmailTemplate(
                "Tâche en Retard",
                employeeName,
                "<div style='background-color: #fee; padding: 20px; border-left: 4px solid #e74c3c; margin: 20px 0;'>" +
                        "<h2 style='color: #e74c3c; margin: 0 0 10px 0;'>⚠️ Tâche en Retard</h2>" +
                        "<p><strong>Projet:</strong> " + projectName + "</p>" +
                        "<p><strong>Tâche:</strong> " + task.getTitle() + "</p>" +
                        "<p><strong>Échéance:</strong> " + task.getDueDate() + "</p>" +
                        "<p style='color: #e74c3c; font-weight: bold; font-size: 18px;'>Retard: " + daysOverdue + " jour(s)</p>" +
                        "<p><strong>Priorité:</strong> " + task.getPriorityEmoji() + " " + task.getPriority().name() + "</p>" +
                        "</div>" +
                        "<p>Cette tâche nécessite votre attention <strong>immédiate</strong>.</p>" +
                        "<p>Veuillez vous connecter à la plateforme pour mettre à jour son statut.</p>"
        );

        return sendEmail(employeeEmail, employeeName, subject, htmlBody);
    }

    /**
     * Notifie le RH de toutes les tâches en retard dans ses projets
     */
    public boolean notifyRHOverdueTasks(String rhEmail, String rhName,
                                        List<TaskAlert> overdueTasks) {
        if (overdueTasks.isEmpty()) return false;

        String subject = "🔴 Rapport: " + overdueTasks.size() + " tâche(s) en retard dans vos projets";

        StringBuilder tasksList = new StringBuilder();
        tasksList.append("<table style='width: 100%; border-collapse: collapse; margin: 20px 0;'>");
        tasksList.append("<tr style='background-color: #f8f9fa;'>");
        tasksList.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: left;'>Projet</th>");
        tasksList.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: left;'>Tâche</th>");
        tasksList.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: left;'>Assigné à</th>");
        tasksList.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: left;'>Retard</th>");
        tasksList.append("</tr>");

        for (TaskAlert alert : overdueTasks) {
            tasksList.append("<tr>");
            tasksList.append("<td style='padding: 10px; border: 1px solid #ddd;'>").append(alert.projectName).append("</td>");
            tasksList.append("<td style='padding: 10px; border: 1px solid #ddd;'>").append(alert.taskTitle).append("</td>");
            tasksList.append("<td style='padding: 10px; border: 1px solid #ddd;'>").append(alert.employeeName).append("</td>");
            tasksList.append("<td style='padding: 10px; border: 1px solid #ddd; color: #e74c3c; font-weight: bold;'>")
                    .append(alert.daysOverdue).append(" jour(s)</td>");
            tasksList.append("</tr>");
        }
        tasksList.append("</table>");

        String htmlBody = buildEmailTemplate(
                "Tâches en Retard",
                rhName,
                "<div style='background-color: #fee; padding: 20px; border-left: 4px solid #e74c3c; margin: 20px 0;'>" +
                        "<h2 style='color: #e74c3c; margin: 0 0 10px 0;'>🔴 " + overdueTasks.size() + " Tâche(s) en Retard</h2>" +
                        "</div>" +
                        "<p>Les tâches suivantes sont en retard dans vos projets:</p>" +
                        tasksList.toString() +
                        "<p><strong>Action recommandée:</strong> Vérifiez avec les employés concernés et réajustez les priorités si nécessaire.</p>"
        );

        return sendEmail(rhEmail, rhName, subject, htmlBody);
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTIFICATIONS - ÉCHÉANCE PROCHE (1 JOUR)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Notifie un employé qu'une tâche arrive à échéance dans 1 jour
     */
    public boolean notifyTaskDueSoon(ProjectTask task, String employeeEmail, String employeeName,
                                     String projectName) {
        String subject = "⏰ RAPPEL: Tâche à rendre demain - " + task.getTitle();

        String htmlBody = buildEmailTemplate(
                "Échéance Imminente",
                employeeName,
                "<div style='background-color: #fff3cd; padding: 20px; border-left: 4px solid #f39c12; margin: 20px 0;'>" +
                        "<h2 style='color: #f39c12; margin: 0 0 10px 0;'>⏰ Échéance Demain</h2>" +
                        "<p><strong>Projet:</strong> " + projectName + "</p>" +
                        "<p><strong>Tâche:</strong> " + task.getTitle() + "</p>" +
                        "<p><strong>Échéance:</strong> " + task.getDueDate() + "</p>" +
                        "<p style='color: #f39c12; font-weight: bold; font-size: 18px;'>⏰ À rendre: Demain</p>" +
                        "<p><strong>Priorité:</strong> " + task.getPriorityEmoji() + " " + task.getPriority().name() + "</p>" +
                        "<p><strong>Heures estimées:</strong> " +
                        (task.getEstimatedHours() != null ? task.getEstimatedHours() + "h" : "Non défini") + "</p>" +
                        "</div>" +
                        "<p>Cette tâche doit être terminée <strong>demain</strong>.</p>" +
                        "<p>Si vous pensez avoir besoin de plus de temps, contactez votre RH dès maintenant.</p>"
        );

        return sendEmail(employeeEmail, employeeName, subject, htmlBody);
    }

    /**
     * Notifie le RH des tâches qui arrivent à échéance dans 1 jour
     */
    public boolean notifyRHTasksDueSoon(String rhEmail, String rhName,
                                        List<TaskAlert> dueSoonTasks) {
        if (dueSoonTasks.isEmpty()) return false;

        String subject = "⏰ " + dueSoonTasks.size() + " tâche(s) à échéance demain";

        StringBuilder tasksList = new StringBuilder();
        tasksList.append("<table style='width: 100%; border-collapse: collapse; margin: 20px 0;'>");
        tasksList.append("<tr style='background-color: #f8f9fa;'>");
        tasksList.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: left;'>Projet</th>");
        tasksList.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: left;'>Tâche</th>");
        tasksList.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: left;'>Assigné à</th>");
        tasksList.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: left;'>Statut</th>");
        tasksList.append("</tr>");

        for (TaskAlert alert : dueSoonTasks) {
            String statusColor = alert.taskStatus.equals("done") ? "#27ae60" : "#f39c12";
            tasksList.append("<tr>");
            tasksList.append("<td style='padding: 10px; border: 1px solid #ddd;'>").append(alert.projectName).append("</td>");
            tasksList.append("<td style='padding: 10px; border: 1px solid #ddd;'>").append(alert.taskTitle).append("</td>");
            tasksList.append("<td style='padding: 10px; border: 1px solid #ddd;'>").append(alert.employeeName).append("</td>");
            tasksList.append("<td style='padding: 10px; border: 1px solid #ddd; color: ").append(statusColor)
                    .append("; font-weight: bold;'>").append(alert.taskStatus).append("</td>");
            tasksList.append("</tr>");
        }
        tasksList.append("</table>");

        String htmlBody = buildEmailTemplate(
                "Échéances Demain",
                rhName,
                "<div style='background-color: #fff3cd; padding: 20px; border-left: 4px solid #f39c12; margin: 20px 0;'>" +
                        "<h2 style='color: #f39c12; margin: 0 0 10px 0;'>⏰ " + dueSoonTasks.size() + " Tâche(s) à Échéance Demain</h2>" +
                        "</div>" +
                        "<p>Les tâches suivantes doivent être rendues <strong>demain</strong>:</p>" +
                        tasksList.toString() +
                        "<p><strong>Conseil:</strong> Vérifiez l'avancement de ces tâches et assurez-vous que les employés sont en mesure de les terminer à temps.</p>"
        );

        return sendEmail(rhEmail, rhName, subject, htmlBody);
    }

    // ═══════════════════════════════════════════════════════════════
    // TEMPLATE HTML EMAIL
    // ═══════════════════════════════════════════════════════════════

    private String buildEmailTemplate(String title, String recipientName, String content) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "</head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;'>" +

                "<!-- Header -->" +
                "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;'>" +
                "<h1 style='color: white; margin: 0; font-size: 24px;'>📊 " + title + "</h1>" +
                "</div>" +

                "<!-- Body -->" +
                "<div style='background-color: #ffffff; padding: 30px; border: 1px solid #ddd; border-top: none;'>" +
                "<p style='font-size: 16px;'>Bonjour <strong>" + recipientName + "</strong>,</p>" +
                content +
                "</div>" +

                "<!-- Footer -->" +
                "<div style='background-color: #f8f9fa; padding: 20px; text-align: center; border-radius: 0 0 10px 10px; font-size: 12px; color: #666;'>" +
                "<p style='margin: 5px 0;'>Ceci est un email automatique de <strong>Workforce Platform</strong></p>" +
                "<p style='margin: 5px 0;'>Système de Gestion de Projets</p>" +
                "<p style='margin: 5px 0;'>© 2026 Tous droits réservés</p>" +
                "</div>" +

                "</body>" +
                "</html>";
    }

    // ═══════════════════════════════════════════════════════════════
    // CLASSE HELPER
    // ═══════════════════════════════════════════════════════════════

    public static class TaskAlert {
        public String projectName;
        public String taskTitle;
        public String employeeName;
        public String employeeEmail;
        public long daysOverdue;
        public String taskStatus;

        public TaskAlert(String projectName, String taskTitle, String employeeName,
                         String employeeEmail, long daysOverdue, String taskStatus) {
            this.projectName = projectName;
            this.taskTitle = taskTitle;
            this.employeeName = employeeName;
            this.employeeEmail = employeeEmail;
            this.daysOverdue = daysOverdue;
            this.taskStatus = taskStatus;
        }
    }
}