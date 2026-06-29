package com.magiclibrary.init;

// -----------------------------------------------------------------------------
// IMPORTS JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;

// -----------------------------------------------------------------------------
// IMPORTS SPRING
// -----------------------------------------------------------------------------
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
 *  Cette classe initialise automatiquement un compte administrateur
 *  lors du démarrage de l'application Spring Boot.
 *
 *  Elle est exécutée via un CommandLineRunner, ce qui signifie :
 *  → exécution au lancement de l'application
 *  → après initialisation du contexte Spring
 *
 * =============================================================================
 *
 *  🧠 LOGIQUE MÉTIER :
 *  -----------------------------------------------------------------------------
 *  - Vérifie si l'admin existe déjà en base
 *  - Récupère le rôle ADMIN
 *  - Crée un utilisateur admin par défaut
 *  - Hash le mot de passe avec BCrypt
 *  - Sauvegarde en base MariaDB
 *
 * =============================================================================
 *
 *  ☁️ CONTEXTE DEPLOIEMENT (RAILWAY) :
 *  -----------------------------------------------------------------------------
 *  ⚠️ IMPORTANT :
 *  - Ce code dépend fortement de la disponibilité des tables SQL
 *  - Si Hibernate ne crée pas les tables (ddl-auto=none), crash possible
 *  - Une base vide sur Railway entraîne un échec immédiat ici
 *
 * =============================================================================
 *
 *  ⚠️ RISQUE CONNU :
 *  -----------------------------------------------------------------------------
 *  Si la table `user` n'existe pas :
 *  → userRepository.findByEmailUser() déclenche une exception SQL
 *  → l'application Spring Boot CRASH au démarrage
 *
 * =============================================================================
 */
@Configuration
@Order(2) // 👉 Exécuté après RoleInitializer (ordre critique pour dépendances)
public class UserInitializer {

    @Bean
    public CommandLineRunner initAdmin(
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {

        return args -> {

            // -----------------------------------------------------------------
            // 1) CHECK D'EXISTENCE - ÉVITE DUPLICATION ADMIN
            // -----------------------------------------------------------------
            // Si un admin existe déjà → aucune action
            // Sécurité : évite double insertion en cas de restart Railway
            // -----------------------------------------------------------------
            if (userRepository.findByEmailUser("admin@example.com").isPresent()) {
                return;
            }

            // -----------------------------------------------------------------
            // 2) RÉCUPÉRATION DU ROLE ADMIN
            // -----------------------------------------------------------------
            // Ce rôle DOIT exister en base via RoleInitializer
            // Sinon → RuntimeException (erreur volontaire pour signaler DB incomplète)
            // -----------------------------------------------------------------
            Role adminRole = roleRepository.findByLabelRole("ADMIN")
                    .orElseThrow(() ->
                            new RuntimeException("ROLE ADMIN introuvable en base !")
                    );

            // -----------------------------------------------------------------
            // 3) HASH DU MOT DE PASSE
            // -----------------------------------------------------------------
            // BCrypt = standard sécurité Spring Security
            // Mot de passe initial (démo uniquement)
            // -----------------------------------------------------------------
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String hashedPassword = encoder.encode("Admin123!");

            // -----------------------------------------------------------------
            // 4) CRÉATION DE L'UTILISATEUR ADMIN
            // -----------------------------------------------------------------
            // ⚠️ Données système (compte technique)
            // Utilisé uniquement pour accès initial application
            // -----------------------------------------------------------------
            User admin = new User(
                    adminRole,
                    "M",
                    "Admin",
                    "System",
                    "admin@example.com",
                    hashedPassword,
                    true,
                    true,
                    LocalDateTime.now()
            );

            // -----------------------------------------------------------------
            // 5) FLAGS MÉTIER
            // -----------------------------------------------------------------
            // Activation email + dépôt autorisé
            // → simplifie utilisation en mode démo
            // -----------------------------------------------------------------
            admin.setEmailVerifiedUser(true);
            admin.setDepositUser(true);

            // -----------------------------------------------------------------
            // 6) SAUVEGARDE EN BASE
            // -----------------------------------------------------------------
            // Insertion en MariaDB via JPA Repository
            // ⚠️ Échoue si table USER inexistante sur Railway
            // -----------------------------------------------------------------
            userRepository.save(admin);

            // -----------------------------------------------------------------
            // 7) LOG DE CONFIRMATION
            // -----------------------------------------------------------------
            System.out.println(">>> ADMIN créé : admin@example.com / Admin123!");
        };
    }
}