package org.example.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mappe la table `fiches_paie` (colonnes : id, employee_id, mois SMALLINT 1-12,
 * annee, salaire_brut, total_primes, total_deductions, salaire_net,
 * notes, statut_paiement, created_at, updated_at).
 */
public class FichePaie {

    // ── Champs DB ────────────────────────────────────────────────────────
    private int id;
    private int employeeId;
    /** Numéro du mois (1 = Janvier … 12 = Décembre) */
    private int mois;
    private int annee;
    private BigDecimal salaireBrut;
    private BigDecimal totalPrimes;
    private BigDecimal totalDeductions;
    private BigDecimal salaireNet;
    private String notes;
    private boolean statutPaiement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private static final String[] MOIS_NOMS = {
        "", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
        "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    // ── Constructeurs ─────────────────────────────────────────────────────
    public FichePaie() {}

    /** Constructeur complet (utilisé lors du chargement depuis la DB). */
    public FichePaie(int id, int mois, int annee, BigDecimal salaireBrut,
                     BigDecimal totalPrimes, BigDecimal totalDeductions,
                     BigDecimal salaireNet, int employeeId) {
        this.id = id;
        this.mois = mois;
        this.annee = annee;
        this.salaireBrut = salaireBrut;
        this.totalPrimes = totalPrimes;
        this.totalDeductions = totalDeductions;
        this.salaireNet = salaireNet;
        this.employeeId = employeeId;
    }

    /** Constructeur pour la création (sans id). */
    public FichePaie(int mois, int annee, BigDecimal salaireBrut,
                     BigDecimal totalPrimes, BigDecimal totalDeductions,
                     BigDecimal salaireNet, int employeeId) {
        this.mois = mois;
        this.annee = annee;
        this.salaireBrut = salaireBrut;
        this.totalPrimes = totalPrimes;
        this.totalDeductions = totalDeductions;
        this.salaireNet = salaireNet;
        this.employeeId = employeeId;
    }

    // ── Accesseurs principaux ─────────────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    /** Retourne le numéro du mois (1-12). */
    public int getMois() { return mois; }
    public void setMois(int mois) { this.mois = mois; }

    /** Retourne le nom français du mois (ex. "Janvier"). */
    public String getMoisNom() {
        return (mois >= 1 && mois <= 12) ? MOIS_NOMS[mois] : String.valueOf(mois);
    }

    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }

    public BigDecimal getSalaireBrut() { return salaireBrut; }
    public void setSalaireBrut(BigDecimal salaireBrut) { this.salaireBrut = salaireBrut; }

    public BigDecimal getTotalPrimes() { return totalPrimes; }
    public void setTotalPrimes(BigDecimal totalPrimes) { this.totalPrimes = totalPrimes; }

    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }

    public BigDecimal getSalaireNet() { return salaireNet; }
    public void setSalaireNet(BigDecimal salaireNet) { this.salaireNet = salaireNet; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isStatutPaiement() { return statutPaiement; }
    public void setStatutPaiement(boolean statutPaiement) { this.statutPaiement = statutPaiement; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ── Aliases rétro-compatibles (ancien nommage) ────────────────────────
    /** @deprecated Utiliser {@link #getId()} */
    @Deprecated public int getIdFiche() { return id; }
    /** @deprecated Utiliser {@link #setId(int)} */
    @Deprecated public void setIdFiche(int id) { this.id = id; }

    /** @deprecated Utiliser {@link #getEmployeeId()} */
    @Deprecated public int getIdEmployees() { return employeeId; }
    /** @deprecated Utiliser {@link #setEmployeeId(int)} */
    @Deprecated public void setIdEmployees(int employeeId) { this.employeeId = employeeId; }

    @Override
    public String toString() {
        return "FichePaie{id=" + id + ", mois=" + mois + ", annee=" + annee +
                ", salaireBrut=" + salaireBrut + ", totalPrimes=" + totalPrimes +
                ", totalDeductions=" + totalDeductions + ", salaireNet=" + salaireNet +
                ", employeeId=" + employeeId + ", statut=" + statutPaiement + '}';
    }
}

