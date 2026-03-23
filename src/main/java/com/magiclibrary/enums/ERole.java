package com.magiclibrary.enums;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.exceptions.custom.UnauthorizedException;

/**
 * =============================================================================
 *  ENUM : ERole
 * =============================================================================
 *  Représentation typée des rôles applicatifs MagicLibrary.
 * =============================================================================
 */
public enum ERole {

    ADMIN("Administrateur"),
    MEMBRE("Membre"),
    INVITE("Invité");

    private final String label;

    ERole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    // -------------------------------------------------------------------------
    // MÉTHODE UTILITAIRE
    // -------------------------------------------------------------------------

    /**
     * Convertit un libellé de rôle issu de la base de données en ERole.
     *
     * @param value libellé du rôle (ADMIN, MEMBRE, INVITE)
     * @return ERole correspondant
     * @throws UnauthorizedException si le rôle est invalide ou inconnu
     */
    public static ERole fromString(String value) {

        if (value == null) {
            throw new UnauthorizedException("Rôle utilisateur absent.");
        }

        try {
            return ERole.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new UnauthorizedException(
                    "Rôle utilisateur invalide : " + value
            );
        }
    }
}