package com.magiclibrary.init;

// -----------------------------------------------------------------------------
// IMPORTS JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;

// -----------------------------------------------------------------------------
// IMPORTS LOGGING
// -----------------------------------------------------------------------------
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// -----------------------------------------------------------------------------
// IMPORTS SPRING
// -----------------------------------------------------------------------------
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// -----------------------------------------------------------------------------
// IMPORTS MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.entities.Role;
import com.magiclibrary.entities.User;
import com.magiclibrary.repositories.interfaces.RoleRepository;
import com.magiclibrary.repositories.interfaces.UserRepository;

/**
 * =============================================================================
 *  INITIALISATION AUTOMATIQUE - UTILISATEUR ADMINISTRATEUR
 * =============================================================================
 *
 *  🔐 OBJECTIF :
 *  -----------------------------------------------------------------------------
 *  Cette classe initialise automatiquement un compte administrateur au démarrage
 *  de l'application Spring Boot, uniquement si ce compte n'existe pas encore.
 *
 *  Le compte admin initial permet d'accéder à l'espace d'administration après
 *  un premier déploiement sur une base Railway vide.
 *
 * =============================================================================
 *
 *  🧠 LOGIQUE MÉTIER :
 *  -----------------------------------------------------------------------------
 *  - Lit l'email administrateur depuis les variables d'environnement
 *  - Vérifie si le compte admin existe déjà
 *  - Lit obligatoirement le mot de passe depuis les variables d'environnement
 *  - Récupère le rôle ADMIN créé par RoleInitializer
 *  - Hash le mot de passe avec BCrypt
 *  - Crée l'utilisateur administrateur initial
 *  - Sauvegarde en base MariaDB
 *
 * =============================================================================
 *
 *  ☁️ CONTEXTE DÉPLOIEMENT (RAILWAY) :
 *  -----------------------------------------------------------------------------
 *  Les secrets ne doivent jamais être hardcodés dans le code source.
 *
 *  En production Railway, le mot de passe initial doit être fourni via :
 *  → ADMIN_PASSWORD
 *
 *  L'email administrateur peut être fourni via :
 *  → ADMIN_EMAIL
 *
 *  Si ADMIN_EMAIL est absent, une valeur par défaut non sensible est utilisée.
 *
 * =============================================================================
 *
 *  🔒 SÉCURITÉ :
 *  -----------------------------------------------------------------------------
 *  - Aucun mot de passe n'est présent dans le code source
 *  - Aucun mot de passe n'est affiché dans les logs
 *  - Le mot de passe est hashé avec BCrypt avant sauvegarde
 *  - La création est idempotente pour éviter les doublons au redémarrage
 *  - Si le compte existe déjà, aucun secret n'est requis au démarrage
 *
 * =============================================================================
 */
@Configuration
@Order(2) // 👉 Exécuté après RoleInitializer (ordre critique pour dépendances)
public class UserInitializer {

    private static final Logger logger = LoggerFactory.getLogger(UserInitializer.class);

    private static final String DEFAULT_ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_EMAIL_PROPERTY = "ADMIN_EMAIL";
    private static final String ADMIN_PASSWORD_PROPERTY = "ADMIN_PASSWORD";

    @Bean
    public CommandLineRunner initAdmin(
            UserRepository userRepository,
            RoleRepository roleRepository,
            Environment environment
    ) {

        return args -> {

            // -----------------------------------------------------------------
            // 1) RÉCUPÉRATION DE L'EMAIL ADMINISTRATEUR
            // -----------------------------------------------------------------
            // L'email peut être injecté via variable d'environnement Railway.
            // Si aucune variable n'est fournie, une valeur par défaut est utilisée.
            // Cette valeur n'est pas sensible et peut rester visible dans le code.
            // -----------------------------------------------------------------
            String adminEmail = environment.getProperty(
                    ADMIN_EMAIL_PROPERTY,
                    DEFAULT_ADMIN_EMAIL
            );

            // -----------------------------------------------------------------
            // 2) CHECK D'EXISTENCE - ÉVITE DUPLICATION ADMIN
            // -----------------------------------------------------------------
            // Si l'admin existe déjà → aucune action.
            // Sécurité : évite une double insertion en cas de restart Railway.
            //
            // Important :
            // Le check est effectué AVANT la lecture du mot de passe.
            // Ainsi, si le compte existe déjà, l'application peut démarrer sans
            // nécessiter ADMIN_PASSWORD.
            // -----------------------------------------------------------------
            if (userRepository.findByEmailUser(adminEmail).isPresent()) {
                logger.info("Compte administrateur initial déjà présent.");
                return;
            }

            // -----------------------------------------------------------------
            // 3) RÉCUPÉRATION DU MOT DE PASSE ADMIN DEPUIS L'ENVIRONNEMENT
            // -----------------------------------------------------------------
            // Le mot de passe initial est un secret.
            // Il ne doit JAMAIS être hardcodé dans le code source.
            //
            // En production Railway :
            //   ADMIN_PASSWORD doit être défini dans l'onglet Variables.
            //
            // Si la variable est absente, l'application échoue volontairement au
            // démarrage afin d'éviter la création d'un compte administrateur faible
            // ou prévisible.
            // -----------------------------------------------------------------
            String adminPassword = environment.getProperty(ADMIN_PASSWORD_PROPERTY);

            if (adminPassword == null || adminPassword.isBlank()) {
                throw new IllegalStateException(
                        "ADMIN_PASSWORD est manquant. Impossible de créer le compte administrateur initial."
                );
            }

            // -----------------------------------------------------------------
            // 4) RÉCUPÉRATION DU ROLE ADMIN
            // -----------------------------------------------------------------
            // Ce rôle DOIT exister en base via RoleInitializer.
            // Sinon → erreur volontaire pour signaler une initialisation DB incomplète.
            // -----------------------------------------------------------------
            Role adminRole = roleRepository.findByLabelRole("ADMIN")
                    .orElseThrow(() ->
                            new RuntimeException("ROLE ADMIN introuvable en base !")
                    );

            // -----------------------------------------------------------------
            // 5) HASH DU MOT DE PASSE
            // -----------------------------------------------------------------
            // BCrypt = standard sécurité Spring Security.
            // Le mot de passe brut n'est jamais sauvegardé ni affiché.
            // -----------------------------------------------------------------
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String hashedPassword = encoder.encode(adminPassword);

            // -----------------------------------------------------------------
            // 6) CRÉATION DE L'UTILISATEUR ADMIN
            // -----------------------------------------------------------------
            // Compte technique initial utilisé pour accéder à l'administration.
            // Les données non sensibles restent volontairement explicites.
            // -----------------------------------------------------------------
            User admin = new User(
                    adminRole,
                    "M",
                    "Admin",
                    "System",
                    adminEmail,
                    hashedPassword,
                    true,
                    true,
                    LocalDateTime.now()
            );

            // -----------------------------------------------------------------
            // 7) FLAGS MÉTIER
            // -----------------------------------------------------------------
            // Activation email + dépôt autorisé.
            // Cela simplifie l'utilisation immédiate en mode démo / production.
            // -----------------------------------------------------------------
            admin.setEmailVerifiedUser(true);
            admin.setDepositUser(true);

            // -----------------------------------------------------------------
            // 8) SAUVEGARDE EN BASE
            // -----------------------------------------------------------------
            // Insertion en MariaDB via JPA Repository.
            // L'opération est exécutée uniquement si le compte n'existe pas déjà.
            // -----------------------------------------------------------------
            userRepository.save(admin);

            // -----------------------------------------------------------------
            // 9) LOG DE CONFIRMATION SÉCURISÉ
            // -----------------------------------------------------------------
            // Le mot de passe n'est jamais affiché.
            // L'email n'est pas loggé afin de conserver des logs sobres en production.
            // -----------------------------------------------------------------
            logger.info("Compte administrateur initial créé avec succès.");
        };
    }
}