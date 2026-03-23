package com.magiclibrary.init;

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
 *  INITIALISATION DES RÔLES
 * =============================================================================
 *  Garantit l'existence des rôles fondamentaux :
 *      - ADMIN
 *      - MEMBRE
 *      - INVITE
 *
 *  Initialisation idempotente exécutée AU DÉMARRAGE de l'application.
 * =============================================================================
 */
@Configuration
@Order(1) // ⬅️ OBLIGATOIRE : les rôles avant tout le reste
public class RoleInitializer {

    @Bean
    public CommandLineRunner initializeRoles(RoleRepository roleRepository) {
        return args -> {
            createRoleIfMissing(roleRepository, "ADMIN");
            createRoleIfMissing(roleRepository, "MEMBRE");
            createRoleIfMissing(roleRepository, "INVITE");
        };
    }

    private void createRoleIfMissing(RoleRepository repository, String labelRole) {
        if (!repository.existsByLabelRole(labelRole)) {
            repository.save(new Role(labelRole));
        }
    }
}
