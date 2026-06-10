package org.example.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ParticipationFormation {

    private int idParticipation;
    private LocalDate dateInscription;
    private String statutParticipation;
    private boolean certificatObtenu; // tinyint en SQL est souvent mappé en boolean ou int
    private LocalDateTime createdAt;
    private int idUtilisateurId;
    private int idSession;
    private String token;
    private Integer quizScore; // Utilisation de Integer pour gérer le NULL
    private boolean quizPassed;
    private LocalDateTime quizAttemptedAt;
    private int quizCorrectCount;
    private int quizTotalQuestions;
    
    // Champs additionnels pour la vue RH
    private int idEmployee;
    private String nomEmployee;
    private String resultat;
    private String motifRefus;
    private boolean priority;
    // Constructeur par défaut
    public ParticipationFormation() {}

    // Constructeur complet (pour la récupération de données)
    public ParticipationFormation(int idParticipation, LocalDate dateInscription, String statutParticipation,
                                  boolean certificatObtenu, LocalDateTime createdAt, int idUtilisateurId,
                                  int idSession, String token, Integer quizScore, boolean quizPassed,
                                  LocalDateTime quizAttemptedAt, int quizCorrectCount, int quizTotalQuestions) {
        this.idParticipation = idParticipation;
        this.dateInscription = dateInscription;
        this.statutParticipation = statutParticipation;
        this.certificatObtenu = certificatObtenu;
        this.createdAt = createdAt;
        this.idUtilisateurId = idUtilisateurId;
        this.idSession = idSession;
        this.token = token;
        this.quizScore = quizScore;
        this.quizPassed = quizPassed;
        this.quizAttemptedAt = quizAttemptedAt;
        this.quizCorrectCount = quizCorrectCount;
        this.quizTotalQuestions = quizTotalQuestions;
    }

    // Constructeur pour l'insertion (sans ID et sans champs automatiques comme createdAt)
    public ParticipationFormation(LocalDate dateInscription, String statutParticipation, int idUtilisateurId, int idSession, String token) {
        this.dateInscription = dateInscription;
        this.statutParticipation = statutParticipation;
        this.idUtilisateurId = idUtilisateurId;
        this.idSession = idSession;
        this.token = token;
    }

    // --- Getters et Setters ---
    // Constructeur simplifié appelé depuis les services avec nom d'employé
    public ParticipationFormation(int idParticipation, int idSession, int idUtilisateurId, LocalDate dateInscription,
                                  String statutParticipation, String resultat, String nomEmployee) {
        this.idParticipation = idParticipation;
        this.idSession = idSession;
        this.idUtilisateurId = idUtilisateurId;
        this.idEmployee = idUtilisateurId;
        this.dateInscription = dateInscription;
        this.statutParticipation = statutParticipation;
        this.resultat = resultat;
        this.nomEmployee = nomEmployee;
    }

    // Constructeur simplifié pour getAllParticipations (6 paramètres sans nom)
    public ParticipationFormation(int idParticipation, int idSession, int idUtilisateurId, LocalDate dateInscription,
                                  String statutParticipation, String resultat) {
        this.idParticipation = idParticipation;
        this.idSession = idSession;
        this.idUtilisateurId = idUtilisateurId;
        this.idEmployee = idUtilisateurId;
        this.dateInscription = dateInscription;
        this.statutParticipation = statutParticipation;
        this.resultat = resultat;
    }


    public int getIdParticipation() { return idParticipation; }
    public void setIdParticipation(int idParticipation) { this.idParticipation = idParticipation; }

    public LocalDate getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDate dateInscription) { this.dateInscription = dateInscription; }

    public String getStatutParticipation() { return statutParticipation; }
    public void setStatutParticipation(String statutParticipation) { this.statutParticipation = statutParticipation; }

    public boolean isCertificatObtenu() { return certificatObtenu; }
    public void setCertificatObtenu(boolean certificatObtenu) { this.certificatObtenu = certificatObtenu; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getIdUtilisateurId() { return idUtilisateurId; }
    public void setIdUtilisateurId(int idUtilisateurId) { this.idUtilisateurId = idUtilisateurId; }

    public int getIdSession() { return idSession; }
    public void setIdSession(int idSession) { this.idSession = idSession; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Integer getQuizScore() { return quizScore; }
    public void setQuizScore(Integer quizScore) { this.quizScore = quizScore; }

    public boolean isQuizPassed() { return quizPassed; }
    public void setQuizPassed(boolean quizPassed) { this.quizPassed = quizPassed; }

    public LocalDateTime getQuizAttemptedAt() { return quizAttemptedAt; }
    public void setQuizAttemptedAt(LocalDateTime quizAttemptedAt) { this.quizAttemptedAt = quizAttemptedAt; }

    public int getQuizCorrectCount() { return quizCorrectCount; }
    public void setQuizCorrectCount(int quizCorrectCount) { this.quizCorrectCount = quizCorrectCount; }

    public int getQuizTotalQuestions() { return quizTotalQuestions; }
    public void setQuizTotalQuestions(int quizTotalQuestions) { this.quizTotalQuestions = quizTotalQuestions; }

    // Nouveaux getters/setters pour les champs additionnels
    public int getIdEmployee() { return idEmployee; }
    public void setIdEmployee(int idEmployee) { this.idEmployee = idEmployee; }

    public String getNomEmployee() { return nomEmployee; }
    public void setNomEmployee(String nomEmployee) { this.nomEmployee = nomEmployee; }

    public String getResultat() { return resultat; }
    public void setResultat(String resultat) { this.resultat = resultat; }

    public String getMotifRefus() { return motifRefus; }
    public void setMotifRefus(String motifRefus) { this.motifRefus = motifRefus; }

    public boolean isPriority() { return priority; }
    public void setPriority(boolean priority) { this.priority = priority; }

    @Override
    public String toString() {
        return "ParticipationFormation{" +
                "idParticipation=" + idParticipation +
                ", statutParticipation='" + statutParticipation + '\'' +
                ", idUtilisateurId=" + idUtilisateurId +
                ", idSession=" + idSession +
                ", score=" + quizScore + "/" + quizTotalQuestions +
                '}';
    }
}