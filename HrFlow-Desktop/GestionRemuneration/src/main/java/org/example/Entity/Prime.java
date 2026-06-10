package org.example.Entity;

import org.example.Enum.PrimeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mappe la table `primes` (colonnes : id, employee_id, type_prime,
 * montant, date_attribution, motif, created_at, updated_at).
 */
public class Prime {

    private int id;
    private int employeeId;
    private PrimeType typePrime;
    private BigDecimal montant;
    private LocalDate dateAttribution;
    private String motif;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Prime() {}

    /** Constructeur complet (depuis DB). */
    public Prime(int id, PrimeType typePrime, BigDecimal montant,
                 LocalDate dateAttribution, int employeeId) {
        this.id = id;
        this.typePrime = typePrime;
        this.montant = montant;
        this.dateAttribution = dateAttribution;
        this.employeeId = employeeId;
    }

    /** Constructeur pour la création (sans id). */
    public Prime(PrimeType typePrime, BigDecimal montant,
                 LocalDate dateAttribution, int employeeId) {
        this.typePrime = typePrime;
        this.montant = montant;
        this.dateAttribution = dateAttribution;
        this.employeeId = employeeId;
    }

    /** Constructeur depuis String (pour compatibilité DB). */
    public Prime(int id, String typePrimeStr, BigDecimal montant,
                 LocalDate dateAttribution, int employeeId) {
        this.id = id;
        this.typePrime = PrimeType.fromLabel(typePrimeStr);
        this.montant = montant;
        this.dateAttribution = dateAttribution;
        this.employeeId = employeeId;
    }

    // ── Accesseurs principaux ─────────────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public PrimeType getTypePrime() { return typePrime; }
    public void setTypePrime(PrimeType typePrime) { this.typePrime = typePrime; }
    
    public void setTypePrimeFromString(String typePrimeStr) {
        this.typePrime = PrimeType.fromLabel(typePrimeStr);
    }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDate getDateAttribution() { return dateAttribution; }
    public void setDateAttribution(LocalDate dateAttribution) { this.dateAttribution = dateAttribution; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ── Aliases rétro-compatibles ──────────────────────────────────────────
    /** @deprecated Utiliser {@link #getId()} */
    @Deprecated public int getIdPrime() { return id; }
    /** @deprecated Utiliser {@link #setId(int)} */
    @Deprecated public void setIdPrime(int id) { this.id = id; }

    /** @deprecated Utiliser {@link #getEmployeeId()} */
    @Deprecated public int getIdEmploye() { return employeeId; }
    /** @deprecated Utiliser {@link #setEmployeeId(int)} */
    @Deprecated public void setIdEmploye(int employeeId) { this.employeeId = employeeId; }

    @Override
    public String toString() {
        return "Prime{id=" + id + ", typePrime='" + typePrime + '\'' +
                ", montant=" + montant + ", dateAttribution=" + dateAttribution +
                ", employeeId=" + employeeId + '}';
    }
}

