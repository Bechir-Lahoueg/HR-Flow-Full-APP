package org.example.Enum;

/**
 * DeductionType Enum - Tous les types de déductions valides
 * 
 * Aligné avec l'Enum PHP: App\Enum\DeductionType
 */
public enum DeductionType {
    IRPP("Impôt sur le Revenu (IRPP)"),
    CNSS("Cotisation CNSS"),
    MUTUELLE("Assurance Mutuelle"),
    CHARGE_SYNDICALE("Charge Syndicale"),
    AVANCE_SALAIRE("Avance sur Salaire"),
    CREDIT_REMBOURSEMENT("Crédit/Remboursement"),
    RETENUE_DISCIPLINAIRE("Retenue Disciplinaire"),
    IMPOT_LOCAL("Impôt Local"),
    ASSURANCE_MALADIE("Assurance Maladie"),
    AUTRE("Autre Déduction");

    private final String label;

    DeductionType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé human-readable
     */
    public String label() {
        return this.label;
    }

    /**
     * Retourne la valeur String (utile pour les requêtes SQL)
     */
    public String value() {
        return this.label;
    }

    /**
     * Cherche un DeductionType par son label
     */
    public static DeductionType fromLabel(String label) {
        if (label == null) {
            throw new IllegalArgumentException("Label ne peut pas être null");
        }
        for (DeductionType type : DeductionType.values()) {
            if (type.label.equalsIgnoreCase(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type de déduction invalide: " + label);
    }

    /**
     * Vérifie si une valeur est valide
     */
    public static boolean isValid(String value) {
        if (value == null) return false;
        try {
            fromLabel(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return this.label;
    }
}
