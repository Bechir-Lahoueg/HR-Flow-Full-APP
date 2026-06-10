package org.example.services;

import java.util.UUID;

/**
 * Service de gestion des liens Jitsi Meet pour les sessions en ligne.
 * 
 * Fournit :
 * - Génération automatique de liens de réunion Jitsi
 * - Validation d'URLs
 * - Gestion des configurations de session distancielle
 */
public class JitsiMeetService {

    private static final String JITSI_DOMAIN = "meet.jit.si";
    private static final String JITSI_BASE_URL = "https://" + JITSI_DOMAIN + "/";

    /**
     * Génère un lien Jitsi Meet unique pour une session de formation
     * 
     * @param formationId l'ID de la formation
     * @param sessionId l'ID de la session
     * @return URL Jitsi complète
     */
    public String generateFormationSessionLink(int formationId, int sessionId) {
        // Génère un room ID unique basé sur formation_session_id + UUID court
        String roomId = "formation_" + formationId + "_session_" + sessionId + "_" + generateShortUuid();
        return JITSI_BASE_URL + roomId;
    }

    /**
     * Génère un lien Jitsi Meet avec configuration personnalisée
     * 
     * @param formationTitle le titre de la formation
     * @param sessionDate la date de la session
     * @return URL Jitsi avec room ID basé sur le titre et la date
     */
    public String generateFormationSessionLinkWithTitle(String formationTitle, String sessionDate) {
        String cleanTitle = formationTitle
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        String roomId = cleanTitle + "_" + sessionDate + "_" + generateShortUuid();
        return JITSI_BASE_URL + roomId;
    }

    /**
     * Valide si une URL est un lien Jitsi Meet valide
     * 
     * @param url l'URL à valider
     * @return true si l'URL est valide, false sinon
     */
    public boolean isValidJitsiUrl(String url) {
        if (url == null || url.isBlank()) return false;
        return url.startsWith(JITSI_BASE_URL) && url.length() > JITSI_BASE_URL.length();
    }

    /**
     * Valide si une URL est un lien de réunion en ligne valide (Teams, Zoom, Meet, Jitsi, etc.)
     * 
     * @param url l'URL à valider
     * @return true si l'URL est valide, false sinon
     */
    public boolean isValidOnlineMeetingUrl(String url) {
        if (url == null || url.isBlank()) return false;
        
        // Accepte les URLs HTTP/HTTPS standard
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false;
        
        // Vérifie que ce sont des domaines de réunion connus
        String[] validDomains = {
            "meet.jit.si",
            "teams.microsoft.com",
            "zoom.us",
            "meet.google.com",
            "whereby.com",
            "appears.in"
        };
        
        for (String domain : validDomains) {
            if (url.contains(domain)) return true;
        }
        
        // Accepte toute URL HTTPS comme fallback (pour d'autres services)
        return url.startsWith("https://");
    }

    /**
     * Extrait l'ID de room d'une URL Jitsi
     * 
     * @param url l'URL Jitsi
     * @return l'ID de room, ou null si pas une URL Jitsi valide
     */
    public String extractRoomId(String url) {
        if (!isValidJitsiUrl(url)) return null;
        return url.substring(JITSI_BASE_URL.length());
    }

    /**
     * Génère un UUID court (8 caractères)
     * 
     * @return UUID court
     */
    private String generateShortUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}

