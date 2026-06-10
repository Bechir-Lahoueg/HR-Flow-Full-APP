package org.example.models;

import java.time.LocalDateTime;

public class Formation {

    private int idFormation;
    private String titre;
    private String description;
    private String type;
    private int duree;
    private String organisme;
    private String objectifs;
    private Integer idRh;
    private double moyenneRating; // Champ calculé (non présent en table mais utile)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String image;

    public Formation() {
    }

    // Constructeur pour insertion (sans ID ni timestamps automatiques)
    public Formation(String titre, String description, String type, int duree,
                     String organisme, String objectifs, Integer idRh, String image) {
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.duree = duree;
        this.organisme = organisme;
        this.objectifs = objectifs;
        this.idRh = idRh;
        this.image = image;
    }

    public Formation(int idFormation, String titre, String description, String type,
                     int duree, String organisme, String objectifs, Integer idRh,
                     LocalDateTime createdAt, LocalDateTime updatedAt, String image) {
        this.idFormation = idFormation;
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.duree = duree;
        this.organisme = organisme;
        this.objectifs = objectifs;
        this.idRh = idRh;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.image = image;
    }

    // --- Getters et Setters ---

    public int getIdFormation() { return idFormation; }
    public void setIdFormation(int idFormation) { this.idFormation = idFormation; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }

    public String getOrganisme() { return organisme; }
    public void setOrganisme(String organisme) { this.organisme = organisme; }

    public String getObjectifs() { return objectifs; }
    public void setObjectifs(String objectifs) { this.objectifs = objectifs; }

    public Integer getIdRh() { return idRh; }
    public void setIdRh(Integer idRh) { this.idRh = idRh; }

    public double getMoyenneRating() { return moyenneRating; }
    public void setMoyenneRating(double moyenneRating) { this.moyenneRating = moyenneRating; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    @Override
    public String toString() {
        return "Formation{" +
                "idFormation=" + idFormation +
                ", titre='" + titre + '\'' +
                ", type='" + type + '\'' +
                ", organisme='" + organisme + '\'' +
                ", idRh=" + idRh +
                ", image='" + image + '\'' +
                '}';
    }
}