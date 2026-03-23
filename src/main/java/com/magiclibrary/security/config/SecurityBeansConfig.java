package com.magiclibrary.security.config;

// -----------------------------------------------------------------------------
// IMPORTS SPRING SECURITY / SPRING CORE
// -----------------------------------------------------------------------------
// Déclaration de beans Spring et gestion du hachage des mots de passe
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * =============================================================================
 * SECURITY BEANS CONFIG
 * =============================================================================
 * Classe dédiée à la déclaration des beans techniques nécessaires
 * au fonctionnement global de la sécurité dans MagicLibrary.
 *
 * Rôle principal :
 *      • fournir un PasswordEncoder unique (BCrypt)
 *      • éviter la duplication de beans dans SecurityConfig
 *
 * Motivation :
 *      • respect strict du principe de responsabilité unique (SRP)
 *      • lisibilité améliorée (séparation config HTTP / beans globaux)
 *      • cohérence avec les bonnes pratiques Spring Security 2024+
 *
 * Utilisation :
 *      • PasswordEncoder injecté dans :
 *            - AuthServiceImpl (hash et vérification des credentials)
 *            - SecurityConfig (AuthenticationProvider)
 *
 * Remarque :
 *      • aucune logique métier ici
 *      • uniquement définition de beans réutilisables
 */
@Configuration
public class SecurityBeansConfig {

    // -------------------------------------------------------------------------
    // PASSWORD ENCODER — BCryptPasswordEncoder
    // -------------------------------------------------------------------------

    /**
     * Fournit l’encodeur de mots de passe utilisé dans toute l’application.
     *
     * Caractéristiques :
     *      - BCrypt : algorithme de hachage lent et sécurisé
     *      - résistant aux attaques par brute force
     *      - standard recommandé par Spring Security
     *
     * Impact :
     *      - hachage des mots de passe lors de la création utilisateur
     *      - vérification des credentials au login
     *
     * @return encodeur BCrypt configuré
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}