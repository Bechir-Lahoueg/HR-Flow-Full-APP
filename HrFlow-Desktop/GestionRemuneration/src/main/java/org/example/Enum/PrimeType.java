package org.example.Enum;

/**
 * PrimeType Enum - Tous les types de primes/bonus valides
 * 
 * Aligné avec l'Enum PHP: App\Enum\PrimeType
 */
public enum PrimeType {
    BONUS("Bonus"),
    PRIME("Prime"),
    PRIME_EXCEPTIONNELLE("Prime Exceptionnelle"),
    INDEMNITE("Indemnité"),
    ALLOCATION_FAMILIALE("Allocation Familiale"),
    PRIME_RENDEMENT("Prime de Rendement"),
    PRIME_PERFORMANCE("Prime de performance"),
    PRIME_ANCIENNETE("Prime d'Ancienneté"),
    GRATIFICATION("Gratification"),
    AUTRE("Autre Prime");

    private final String label;

    PrimeType(String label) {
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
     * Cherche un PrimeType par son label
     */
    public static PrimeType fromLabel(String label) {
        if (label == null) {
            throw new IllegalArgumentException("Label ne peut pas être null");
        }
        for (PrimeType type : PrimeType.values()) {
            if (type.label.equalsIgnoreCase(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type de prime invalide: " + label);
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
