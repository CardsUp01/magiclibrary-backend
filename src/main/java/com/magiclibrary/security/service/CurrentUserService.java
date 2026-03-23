package com.magiclibrary.security.service;

// ============================================================================
// IMPORTS SPRING SECURITY
// ============================================================================
// Gestion du contexte d’authentification Spring Security
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// ============================================================================
// IMPORTS INTERNES MAGICLIBRARY
// ============================================================================
// Entité User représentant l’utilisateur authentifié
import com.magiclibrary.entities.User;

/**
 * ============================================================================
 * SERVICE : CURRENT USER
 * ============================================================================
 * Interface dédiée à la récupération de l’utilisateur actuellement authentifié.
 *
 * Objectifs :
 *      - éviter la répétition de SecurityContextHolder dans les contrôleurs et services
 *      - améliorer la lisibilité et la centralisation de l’accès à l’utilisateur courant
 *      - gérer proprement les cas où aucun utilisateur n’est connecté
 *      - préparer le code aux évolutions futures (notifications, statistiques, espace membre enrichi)
 *
 * Remarques :
 *      - ce service ne contient aucune logique métier
 *      - il expose uniquement l’accès au contexte de sécurité Spring Security
 */
public interface CurrentUserService {

    // -------------------------------------------------------------------------
    // RÉCUPÉRATION DE L’UTILISATEUR COURANT
    // -------------------------------------------------------------------------

    /**
     * Retourne l’entité User correspondant à l’utilisateur actuellement
     * authentifié dans Spring Security.
     *
     * @return User si un utilisateur est authentifié, sinon null
     */
    User getCurrentUser();

    /**
     * Indique si un utilisateur est actuellement authentifié dans le contexte
     * Spring Security.
     *
     * @return true si un utilisateur est connecté, false sinon
     */
    boolean isAuthenticated();
}