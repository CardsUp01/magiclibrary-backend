package com.magiclibrary.repositories.interfaces;

// -----------------------------------------------------------------------------
// IMPORTS JPA / SPRING DATA JPA
// -----------------------------------------------------------------------------
// JpaRepository pour opérations CRUD et gestion des entités
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
// Entité Role
import com.magiclibrary.entities.Role;

/**
 * =============================================================================
 * REPOSITORY : ROLE
 * =============================================================================
 * Interface d’accès aux données pour l’entité Role dans l’application MagicLibrary.
 *
 * Caractéristiques :
 *      - Repository = interface uniquement
 *      - Séparation complète persistance / service / contrôleur
 *      - Aucune logique applicative ici
 *
 * Hérite de JpaRepository<Role, Integer> pour :
 *      - opérations CRUD complètes
 *      - gestion native des identifiants auto-incrémentés
 *      - support des méthodes dérivées Spring Data JPA
 *
 * Utilisation :
 *      - initialisation automatique des rôles (RoleInitializer)
 *      - résolution de rôle dans CustomUserDetailsService
 *      - cohérence SQL / entité / enum ERole
 *
 * Conforme au dictionnaire de données, MCD/MLD/MPD et exigences MVP.
 */
public interface RoleRepository extends JpaRepository<Role, Integer> {

    // -------------------------------------------------------------------------
    // MÉTHODES DE RECHERCHE PERSONNALISÉES
    // -------------------------------------------------------------------------

    /**
     * Recherche un rôle selon son libellé exact.
     *
     * @param labelRole libellé du rôle (ADMIN, MEMBRE, INVITE)
     * @return Optional contenant le rôle si présent
     */
    Optional<Role> findByLabelRole(String labelRole);

    /**
     * Vérifie l’existence d’un rôle selon son libellé.
     *
     * Utilisée pour l’initialisation idempotente des rôles.
     *
     * @param labelRole libellé du rôle
     * @return true si le rôle existe déjà, false sinon
     */
    boolean existsByLabelRole(String labelRole);
}