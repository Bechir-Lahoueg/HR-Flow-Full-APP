package org.example.models;

import java.time.LocalDate;
import java.util.Date;

public class Presence {

    private int idPresence;
    private Date datePresence;
    private String statut;
    private int idParticipationId; // Clé étrangère vers la table participation

    // Constructeur par défaut
    public Presence() {
    }

    // Constructeur sans ID (utile pour les opérations d'insertion)
    public Presence(Date datePresence, String statut, int idParticipationId) {
        this.datePresence = datePresence;
        this.statut = statut;
        this.idParticipationId = idParticipationId;
    }

    // Constructeur avec ID et LocalDate
    public Presence(int idPresence, LocalDate datePresence, String statut, int idParticipationId) {
        this.idPresence = idPresence;
        this.statut = statut;
        this.idParticipationId = idParticipationId;
    }

    // Constructeur avec ID (pour la récupération depuis la base de données)
    public Presence(int idPresence, Date datePresence, String statut, int idParticipationId) {
        this.idPresence = idPresence;
        this.datePresence = datePresence;
        this.statut = statut;
        this.idParticipationId = idParticipationId;
    }

    // Getters et Setters

    public int getIdPresence() {
        return idPresence;
    }

    public void setIdPresence(int idPresence) {
        this.idPresence = idPresence;
    }

    public Date getDatePresence() {
        return datePresence;
    }

    public void setDatePresence(Date datePresence) {
        this.datePresence = datePresence;
    }

    public void setDatePresence(LocalDate datePresence) {
        if (datePresence != null) {
            this.datePresence = java.sql.Date.valueOf(datePresence);
        }
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getIdParticipationId() {
        return idParticipationId;
    }

    // Alias pour cohérence avec le contrôleur
    public int getIdParticipation() {
        return idParticipationId;
    }

    public void setIdParticipation(int idParticipation) {
        this.idParticipationId = idParticipation;
    }

    public void setIdParticipationId(int idParticipationId) {
        this.idParticipationId = idParticipationId;
    }

    // Méthode toString pour le débogage
    @Override
    public String toString() {
        return "Presence{" +
                "idPresence=" + idPresence +
                ", datePresence=" + datePresence +
                ", statut='" + statut + '\'' +
                ", idParticipationId=" + idParticipationId +
                '}';
    }
}