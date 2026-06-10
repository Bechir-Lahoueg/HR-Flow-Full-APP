package org.example.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SessionFormation {

    private int idSession;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String lieu;
    private String mode; // Présentiel / Distanciel
    private int capaciteMax;
    private String statut; // Planifiée / EnCours / Terminée / Annulée
    private String lienOnline; // URL Jitsi Meet pour sessions distancielles (nullable)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int idFormationId;
    private int placesDisponibles; // Champ calculé

    // Constructeur par défaut
    public SessionFormation() {}

    // Constructeur pour l'insertion (sans ID ni timestamps)
    public SessionFormation(LocalDate dateDebut, LocalDate dateFin, String lieu,
                            String mode, int capaciteMax, String statut, int idFormationId) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.mode = mode;
        this.capaciteMax = capaciteMax;
        this.statut = statut;
        this.idFormationId = idFormationId;
    }

    // Constructeur simplifié appelé depuis le contrôleur RH (idSession, idFormation, dates, lieu, mode, capacite, statut)
    public SessionFormation(int idSession, int idFormation, LocalDate dateDebut, LocalDate dateFin,
                            String lieu, String mode, int capaciteMax, String statut) {
        this.idSession = idSession;
        this.idFormationId = idFormation;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.mode = mode;
        this.capaciteMax = capaciteMax;
        this.statut = statut;
    }

    // Constructeur complet (Récupération DB)
    public SessionFormation(int idSession, LocalDate dateDebut, LocalDate dateFin,
                            String lieu, String mode, int capaciteMax, String statut,
                            LocalDateTime createdAt, LocalDateTime updatedAt, int idFormationId) {
        this.idSession = idSession;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.mode = mode;
        this.capaciteMax = capaciteMax;
        this.statut = statut;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.idFormationId = idFormationId;
    }

    // --- Getters & Setters ---

    public int getIdSession() { return idSession; }
    public void setIdSession(int idSession) { this.idSession = idSession; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public int getCapaciteMax() { return capaciteMax; }
    public void setCapaciteMax(int capaciteMax) { this.capaciteMax = capaciteMax; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getLienOnline() { return lienOnline; }
    public void setLienOnline(String lienOnline) { this.lienOnline = lienOnline; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getIdFormationId() { return idFormationId; }
    public void setIdFormationId(int idFormationId) { this.idFormationId = idFormationId; }

    // Alias pour cohérence avec le contrôleur RH
    public int getIdFormation() { return idFormationId; }
    public void setIdFormation(int idFormation) { this.idFormationId = idFormation; }

    public int getPlacesDisponibles() { return placesDisponibles; }
    public void setPlacesDisponibles(int placesDisponibles) { this.placesDisponibles = placesDisponibles; }

    @Override
    public String toString() {
        return "SessionFormation{" +
                "idSession=" + idSession +
                ", dateDebut=" + dateDebut +
                ", mode='" + mode + '\'' +
                ", statut='" + statut + '\'' +
                ", idFormationId=" + idFormationId +
                '}';
    }
}