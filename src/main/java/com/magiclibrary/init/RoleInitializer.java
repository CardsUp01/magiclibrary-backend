package com.magiclibrary.init;

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

// -----------------------------------------------------------------------------
// IMPORTS MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.entities.Role;
import com.magiclibrary.repositories.interfaces.RoleRepository;

/**
 * =============================================================================
 *  INITIALISATION AUTOMATIQUE - RÔLES APPLICATIFS
 * =============================================================================
 *
 *  🎯 OBJECTIF :
 *  -----------------------------------------------------------------------------
 *  Cette classe garantit l'existence des rôles fondamentaux de l'application :
 *      - ADMIN
 *      - MEMBRE
 *      - INVITE
 *
 *  Ces rôles sont indispensables au fonctionnement de Spring Security,
 *  de la gestion des droits et des initialiseurs utilisateurs.
 *
 * =============================================================================
 *
 *  🧠 LOGIQUE MÉTIER :
 *  -----------------------------------------------------------------------------
 *  - Vérifie si chaque rôle existe déjà en base
 *  - Crée uniquement les rôles manquants
 *  - Ne modifie jamais un rôle existant
 *  - Garantit une initialisation idempotente au démarrage
 *
 * =============================================================================
 *
 *  ☁️ CONTEXTE DÉPLOIEMENT (RAILWAY) :
 *  -----------------------------------------------------------------------------
 *  Sur une base Railway vide, cette classe initialise les rôles avant la création
 *  des comptes administrateur et membres de démonstration.
 *
 *  L'ordre d'exécution est donc critique :
 *      1) RoleInitializer
 *      2) UserInitializer
 *      3) MemberInitializer
 *
 * =============================================================================
 *
 *  🔒 SÉCURITÉ :
 *  -----------------------------------------------------------------------------
 *  - Aucun secret n'est manipulé dans cette classe
 *  - Aucun mot de passe n'est créé ici
 *  - Les rôles sont centralisés sous forme de constantes
 *  - L'initialisation est stable et sans effet de bord
 *
 * =============================================================================
 */
@Configuration
@Order(1) // 👉 OBLIGATOIRE : les rôles doivent exister avant les utilisateurs
public class RoleInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RoleInitializer.class);

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MEMBRE = "MEMBRE";
    private static final String ROLE_INVITE = "INVITE";

    @Bean
    public CommandLineRunner initializeRoles(RoleRepository roleRepository) {
        return args -> {

            // -----------------------------------------------------------------
            // INITIALISATION DES RÔLES FONDAMENTAUX
            // -----------------------------------------------------------------
            // Les rôles sont créés uniquement s'ils sont absents.
            // Aucun doublon n'est généré lors des redémarrages Railway.
            // -----------------------------------------------------------------
            createRoleIfMissing(roleRepository, ROLE_ADMIN);
            createRoleIfMissing(roleRepository, ROLE_MEMBRE);
            createRoleIfMissing(roleRepository, ROLE_INVITE);
        };
    }

    private void createRoleIfMissing(RoleRepository repository, String labelRole) {

        // ---------------------------------------------------------------------
        // CHECK D'EXISTENCE - INITIALISATION IDEMPOTENTE
        // ---------------------------------------------------------------------
        // Si le rôle existe déjà, aucune modification n'est effectuée.
        // ---------------------------------------------------------------------
        if (repository.existsByLabelRole(labelRole)) {
            logger.info("Rôle déjà présent : {}", labelRole);
            return;
        }

        // ---------------------------------------------------------------------
        // CRÉATION DU RÔLE MANQUANT
        // ---------------------------------------------------------------------
        // Sauvegarde du rôle applicatif indispensable au système d'autorisation.
        // ---------------------------------------------------------------------
        repository.save(new Role(labelRole));

        logger.info("Rôle créé avec succès : {}", labelRole);
    }
}