package org.example.Entity;

import org.example.Enum.DeductionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mappe la table `deductions` (colonnes : id, employee_id, type_deduction,
 * montant, date_deduction, motif, created_at, updated_at).
 */
public class Deduction {

    private int id;
    private int employeeId;
    private DeductionType typeDeduction;
    private BigDecimal montant;
    private LocalDate dateDeduction;
    private String motif;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Deduction() {}

    /** Constructeur complet (depuis DB). */
    public Deduction(int id, DeductionType typeDeduction, BigDecimal montant,
                     LocalDate dateDeduction, int employeeId) {
        this.id = id;
        this.typeDeduction = typeDeduction;
        this.montant = montant;
        this.dateDeduction = dateDeduction;
        this.employeeId = employeeId;
    }

    /** Constructeur pour la création (sans id). */
    public Deduction(DeductionType typeDeduction, BigDecimal montant,
                     LocalDate dateDeduction, int employeeId) {
        this.typeDeduction = typeDeduction;
        this.montant = montant;
        this.dateDeduction = dateDeduction;
        this.employeeId = employeeId;
    }

    /** Constructeur depuis String (pour compatibilité DB). */
    public Deduction(int id, String typeDeductionStr, BigDecimal montant,
                     LocalDate dateDeduction, int employeeId) {
        this.id = id;
        this.typeDeduction = DeductionType.fromLabel(typeDeductionStr);
        this.montant = montant;
        this.dateDeduction = dateDeduction;
        this.employeeId = employeeId;
    }

    // ── Accesseurs principaux ─────────────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public DeductionType getTypeDeduction() { return typeDeduction; }
    public void setTypeDeduction(DeductionType typeDeduction) { this.typeDeduction = typeDeduction; }
    
    public void setTypeDeductionFromString(String typeDeductionStr) {
        this.typeDeduction = DeductionType.fromLabel(typeDeductionStr);
    }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDate getDateDeduction() { return dateDeduction; }
    public void setDateDeduction(LocalDate dateDeduction) { this.dateDeduction = dateDeduction; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ── Aliases rétro-compatibles ──────────────────────────────────────────
    /** @deprecated Utiliser {@link #getId()} */
    @Deprecated public int getIdDeduction() { return id; }
    /** @deprecated Utiliser {@link #setId(int)} */
    @Deprecated public void setIdDeduction(int id) { this.id = id; }

    /** @deprecated Utiliser {@link #getEmployeeId()} */
    @Deprecated public int getIdEmploye() { return employeeId; }
    /** @deprecated Utiliser {@link #setEmployeeId(int)} */
    @Deprecated public void setIdEmploye(int employeeId) { this.employeeId = employeeId; }

    @Override
    public String toString() {
        return "Deduction{id=" + id + ", typeDeduction='" + typeDeduction + '\'' +
                ", montant=" + montant + ", dateDeduction=" + dateDeduction +
                ", employeeId=" + employeeId + '}';
    }
}
