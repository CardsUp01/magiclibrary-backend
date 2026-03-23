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
 *  INITIALISATION DE L'UTILISATEUR ADMINISTRATEUR
 * =============================================================================
 *  Crée automatiquement le compte administrateur si absent.
 * =============================================================================
 */
@Configuration
@Order(2) // ⬅️ APRÈS RoleInitializer
public class UserInitializer {

    @Bean
    public CommandLineRunner initAdmin(
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        return args -> {

            // 1) Ne rien faire si l'admin existe déjà
            if (userRepository.findByEmailUser("admin@example.com").isPresent()) {
                return;
            }

            // 2) Récupération du rôle ADMIN (garanti par RoleInitializer)
            Role adminRole = roleRepository.findByLabelRole("ADMIN")
                    .orElseThrow(() ->
                            new RuntimeException("ROLE ADMIN introuvable en base !")
                    );

            // 3) Hash du mot de passe
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String hashedPassword = encoder.encode("Admin123!");

            // 4) Création de l'utilisateur ADMIN
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

            // 5) Champs optionnels sécurisés
            admin.setEmailVerifiedUser(true);
            admin.setDepositUser(true);

            // 6) Sauvegarde
            userRepository.save(admin);

            System.out.println(">>> ADMIN créé : admin@example.com / Admin123!");
        };
    }
}
