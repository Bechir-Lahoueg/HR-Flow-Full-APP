package org.example.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Properties;

/**
 * AI-powered comment/justification generator for leave requests.
 * Uses the Groq API (llama-3.1-8b-instant) — mirrors web AiService.php leave methods.
 */
public class LeaveAiService {

    private static final String FALLBACK =
            "Suggestion IA indisponible. Rédigez votre commentaire manuellement.";

    private final String apiKey;
    private final String apiUrl;
    private final String model;

    public LeaveAiService() {
        Properties props = new Properties();
        String key = null, url = null, mdl = null;
        try (InputStream in = LeaveAiService.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
                key  = props.getProperty("groq.api.key");
                url  = props.getProperty("groq.api.url");
                mdl  = props.getProperty("groq.api.model");
            }
        } catch (IOException ignored) {}
        this.apiKey = (key  != null) ? key  : "";
        this.apiUrl = (url  != null) ? url  : "https://api.groq.com/openai/v1/chat/completions";
        this.model  = (mdl  != null) ? mdl  : "llama-3.1-8b-instant";
    }

    // ─── Public API (mirrors web AiService.php) ────────────────────────────────

    /**
     * Generates a professional employee leave justification letter (French).
     * Mirrors web generateEmployeeLeaveJustification().
     */
    public String generateEmployeeLeaveJustification(String leaveType, LocalDate startDate,
                                                      LocalDate endDate, String urgencyLevel,
                                                      String reason) {
        String urgLabel = urgencyLabel(urgencyLevel);
        String prompt = String.format(
            "Tu es un assistant RH professionnel. Rédige une justification professionnelle et concise " +
            "pour une demande de congé exceptionnelle en français. " +
            "La justification doit être entre 40 et 90 mots, formelle et convaincante. " +
            "Détails de la demande: Type de congé: %s, Période: du %s au %s, " +
            "Urgence: %s, Motif initial: %s. " +
            "Réponds uniquement avec la justification, sans titre ni introduction.",
            leaveType, startDate, endDate, urgLabel, reason);
        return callGroq(prompt);
    }

    /**
     * Generates an RH decision comment (French).
     * Mirrors web generateRhLeaveDecisionComment().
     */
    public String generateRhLeaveDecisionComment(String action, String employeeName,
                                                  String leaveType, LocalDate startDate,
                                                  LocalDate endDate, int daysCount,
                                                  String urgencyLevel, String reason) {
        String urgLabel = urgencyLabel(urgencyLevel);
        String prompt = String.format(
            "Tu es un responsable RH. Rédige un commentaire professionnel de décision RH en français " +
            "pour une demande de congé exceptionnelle. " +
            "Le commentaire doit être entre 25 et 70 mots, clair et professionnel. " +
            "Action: %s, Employé: %s, Type: %s, Période: %s au %s (%d jours ouvrables), " +
            "Urgence: %s, Motif: %s. " +
            "Réponds uniquement avec le commentaire, sans titre.",
            action, employeeName, leaveType, startDate, endDate, daysCount,
            urgLabel, reason);
        return callGroq(prompt);
    }

    /**
     * Generates an Admin final decision comment (French).
     * Mirrors web generateAdminLeaveDecisionComment().
     */
    public String generateAdminLeaveDecisionComment(String action, String employeeName,
                                                     String leaveType, LocalDate startDate,
                                                     LocalDate endDate, int daysCount,
                                                     String urgencyLevel, String reason,
                                                     String rhComment) {
        String urgLabel = urgencyLabel(urgencyLevel);
        String prompt = String.format(
            "Tu es un directeur administratif. Rédige un commentaire de décision finale en français " +
            "pour une demande de congé exceptionnelle pré-approuvée par le RH. " +
            "Le commentaire doit être entre 25 et 80 mots, formel et définitif. " +
            "Action: %s, Employé: %s, Type: %s, Période: %s au %s (%d jours ouvrables), " +
            "Urgence: %s, Motif: %s, Commentaire RH: %s. " +
            "Réponds uniquement avec le commentaire, sans titre.",
            action, employeeName, leaveType, startDate, endDate, daysCount,
            urgLabel, reason, (rhComment != null ? rhComment : "Aucun"));
        return callGroq(prompt);
    }

    // ─── Internal ──────────────────────────────────────────────────────────────

    private String callGroq(String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) return FALLBACK;
        try {
            String body = buildJsonBody(userPrompt);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return extractContent(response.body());
            } else {
                System.err.println("Groq API error " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            System.err.println("LeaveAiService: " + e.getMessage());
        }
        return FALLBACK;
    }

    private String buildJsonBody(String prompt) {
        // Manual JSON — no external library needed
        String escaped = escapeJson(prompt);
        String escapedModel = escapeJson(model);
        return "{\"model\":\"" + escapedModel + "\",\"messages\":[{\"role\":\"user\",\"content\":\"" + escaped + "\"}],\"max_tokens\":300,\"temperature\":0.7}";
    }

    /** Extracts the content string from Groq's JSON response (no JSON library). */
    private String extractContent(String json) {
        // Look for "content":"<value>"
        int idx = json.indexOf("\"content\"");
        if (idx < 0) return FALLBACK;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return FALLBACK;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return FALLBACK;
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default  -> { sb.append('\\'); sb.append(c); }
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        String result = sb.toString().trim();
        return result.isBlank() ? FALLBACK : result;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String urgencyLabel(String urgencyLevel) {
        if (urgencyLevel == null) return "Non défini";
        return switch (urgencyLevel.toUpperCase()) {
            case "LOW"    -> "Faible";
            case "MEDIUM" -> "Moyen";
            case "HIGH"   -> "Haute";
            default       -> urgencyLevel;
        };
    }
}
