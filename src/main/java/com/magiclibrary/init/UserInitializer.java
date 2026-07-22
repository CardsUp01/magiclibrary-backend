package com.magiclibrary.init;

// -----------------------------------------------------------------------------
// IMPORTS JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

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
 * INITIALISATION AUTOMATIQUE - UTILISATEUR ADMINISTRATEUR
 * =============================================================================
 *
 * <p>Cette configuration garantit la présence du compte administrateur initial
 * nécessaire à l'accès à l'espace d'administration de MagicLibrary.</p>
 *
 * <p>Contrairement aux initialiseurs strictement réservés à la démonstration,
 * cette classe reste active dans tous les environnements :</p>
 *
 * <ul>
 *     <li>DEV, pour permettre l'utilisation locale de l'application ;</li>
 *     <li>DEMO, pour préparer le compte administrateur recruteur ;</li>
 *     <li>PROD CLIENT, pour permettre l'administration de l'instance réelle.</li>
 * </ul>
 *
 * <h2>Isolation du marqueur DEMO</h2>
 *
 * <p>La création du compte administrateur est une responsabilité commune.
 * En revanche, l'attribution du marqueur
 * {@code DemoScenarioCodes.RECRUITER_DEMO_USERS} est strictement limitée à
 * l'environnement de démonstration.</p>
 *
 * <p>Le marqueur est appliqué uniquement lorsque les deux conditions
 * suivantes sont réunies :</p>
 *
 * <ul>
 *     <li>le profil Spring {@code demo} est explicitement actif ;</li>
 *     <li>la propriété {@code magiclibrary.demo.reset.enabled} vaut
 *     explicitement {@code true}.</li>
 * </ul>
 *
 * <p>Une instance CLIENT utilisant uniquement le profil {@code prod} crée donc
 * son administrateur initial sans jamais le marquer comme donnée DEMO.</p>
 *
 * <h2>Idempotence</h2>
 *
 * <p>Le compte est recherché par l'adresse configurée dans
 * {@code ADMIN_EMAIL} :</p>
 *
 * <ul>
 *     <li>s'il existe déjà, il n'est pas recréé ;</li>
 *     <li>son mot de passe, son rôle et son identité ne sont pas remplacés ;</li>
 *     <li>en environnement DEMO uniquement, le marqueur manquant est ajouté ;</li>
 *     <li>en environnement non DEMO, aucun marqueur n'est ajouté ou retiré.</li>
 * </ul>
 *
 * <h2>Sécurité des secrets</h2>
 *
 * <p>Le mot de passe initial provient exclusivement de
 * {@code ADMIN_PASSWORD}. Il n'est chargé que lorsqu'une création est
 * nécessaire, puis il est haché avec BCrypt avant persistance.</p>
 *
 * <p>Aucun mot de passe brut n'est enregistré ou écrit dans les journaux.</p>
 *
 * <h2>Ordre d'exécution</h2>
 *
 * <p>L'ordre {@code 2} garantit que {@code RoleInitializer} a déjà préparé le
 * rôle ADMIN avant la création éventuelle du compte.</p>
 */
@Configuration
public class UserInitializer {

    private static final Logger logger =
            LoggerFactory.getLogger(UserInitializer.class);

    private static final String DEFAULT_ADMIN_EMAIL =
            "admin@example.com";

    private static final String ADMIN_EMAIL_PROPERTY =
            "ADMIN_EMAIL";

    private static final String ADMIN_PASSWORD_PROPERTY =
            "ADMIN_PASSWORD";

    private static final String DEMO_PROFILE =
            "demo";

    private static final String DEMO_RESET_ENABLED_PROPERTY =
            "magiclibrary.demo.reset.enabled";

    private static final String ROLE_ADMIN =
            "ADMIN";

    /**
     * Initialise le compte administrateur commun à l'environnement courant.
     *
     * <p>La méthode distingue explicitement :</p>
     *
     * <ul>
     *     <li>la création commune du compte administrateur ;</li>
     *     <li>le marquage supplémentaire réservé à la DEMO.</li>
     * </ul>
     *
     * @param userRepository repository des utilisateurs
     * @param roleRepository repository des rôles
     * @param environment environnement Spring et propriétés de déploiement
     * @return runner exécuté après l'initialisation des rôles
     */
    @Bean
    @Order(2)
    public CommandLineRunner initAdmin(
            UserRepository userRepository,
            RoleRepository roleRepository,
            Environment environment
    ) {
        return args -> {
            String adminEmail = resolveAdminEmail(environment);
            boolean demoEnvironment = isDemoEnvironment(environment);

            Optional<User> existingAdminOptional =
                    userRepository.findByEmailUser(adminEmail);

            if (existingAdminOptional.isPresent()) {
                handleExistingAdmin(
                        userRepository,
                        existingAdminOptional.get(),
                        demoEnvironment
                );
                return;
            }

            String adminPassword = resolveRequiredAdminPassword(environment);
            Role adminRole = resolveAdminRole(roleRepository);

            BCryptPasswordEncoder passwordEncoder =
                    new BCryptPasswordEncoder();

            String hashedPassword =
                    passwordEncoder.encode(adminPassword);

            User admin = createAdmin(
                    adminRole,
                    adminEmail,
                    hashedPassword,
                    demoEnvironment
            );

            userRepository.save(admin);

            if (demoEnvironment) {
                logger.info(
                        "Compte administrateur initial DEMO créé avec succès."
                );
            } else {
                logger.info(
                        "Compte administrateur initial créé avec succès."
                );
            }
        };
    }

    /**
     * Résout et normalise l'adresse du compte administrateur.
     *
     * @param environment environnement Spring
     * @return adresse non vide
     */
    private String resolveAdminEmail(Environment environment) {
        String adminEmail = environment.getProperty(
                ADMIN_EMAIL_PROPERTY,
                DEFAULT_ADMIN_EMAIL
        );

        if (adminEmail.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL est vide. Impossible d'identifier le compte "
                            + "administrateur initial."
            );
        }

        return adminEmail.trim();
    }

    /**
     * Détermine si le marquage DEMO est explicitement autorisé.
     *
     * <p>Le profil et la propriété doivent être simultanément présents. Cette
     * double protection empêche l'activation du marquage sur une instance
     * CLIENT utilisant le profil {@code prod} seul.</p>
     *
     * @param environment environnement Spring
     * @return {@code true} uniquement pour l'instance DEMO autorisée
     */
    private boolean isDemoEnvironment(Environment environment) {
        boolean demoProfileActive = Arrays.stream(
                environment.getActiveProfiles()
        ).anyMatch(DEMO_PROFILE::equals);

        boolean demoResetEnabled = environment.getProperty(
                DEMO_RESET_ENABLED_PROPERTY,
                Boolean.class,
                Boolean.FALSE
        );

        return demoProfileActive && demoResetEnabled;
    }

    /**
     * Traite un administrateur déjà présent sans modifier ses données
     * fonctionnelles.
     *
     * <p>En DEMO, le marqueur officiel est ajouté s'il manque. Dans tous les
     * autres environnements, le compte reste strictement inchangé.</p>
     *
     * @param userRepository repository des utilisateurs
     * @param existingAdmin compte déjà présent
     * @param demoEnvironment indique si le marquage DEMO est autorisé
     */
    private void handleExistingAdmin(
            UserRepository userRepository,
            User existingAdmin,
            boolean demoEnvironment
    ) {
        if (!demoEnvironment) {
            logger.info(
                    "Compte administrateur initial déjà présent."
            );
            return;
        }

        if (DemoScenarioCodes.RECRUITER_DEMO_USERS.equals(
                existingAdmin.getDemoScenarioCode()
        )) {
            logger.info(
                    "Compte administrateur DEMO déjà présent et correctement "
                            + "marqué."
            );
            return;
        }

        existingAdmin.setDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_USERS
        );

        userRepository.save(existingAdmin);

        logger.info(
                "Compte administrateur déjà présent et marqué comme compte "
                        + "socle DEMO."
        );
    }

    /**
     * Charge le mot de passe administrateur requis pour une création.
     *
     * @param environment environnement Spring
     * @return mot de passe brut non vide, destiné uniquement au hachage immédiat
     */
    private String resolveRequiredAdminPassword(
            Environment environment
    ) {
        String adminPassword = environment.getProperty(
                ADMIN_PASSWORD_PROPERTY
        );

        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD est manquant. Impossible de créer le "
                            + "compte administrateur initial."
            );
        }

        return adminPassword;
    }

    /**
     * Résout le rôle ADMIN créé préalablement par {@code RoleInitializer}.
     *
     * @param roleRepository repository des rôles
     * @return rôle ADMIN persistant
     */
    private Role resolveAdminRole(RoleRepository roleRepository) {
        return roleRepository.findByLabelRole(ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "Le rôle ADMIN est introuvable. "
                                + "L'initialisation des rôles est incomplète."
                ));
    }

    /**
     * Construit le compte administrateur initial.
     *
     * <p>Le marqueur DEMO est renseigné uniquement lorsque l'environnement
     * courant a été validé comme instance de démonstration.</p>
     *
     * @param adminRole rôle ADMIN
     * @param adminEmail adresse configurée
     * @param hashedPassword mot de passe haché
     * @param demoEnvironment indique si le marqueur DEMO doit être appliqué
     * @return administrateur prêt à être persisté
     */
    private User createAdmin(
            Role adminRole,
            String adminEmail,
            String hashedPassword,
            boolean demoEnvironment
    ) {
        User admin = new User(
                adminRole,
                "M",
                "Admin",
                "System",
                adminEmail,
                hashedPassword,
                Boolean.TRUE,
                Boolean.TRUE,
                LocalDateTime.now()
        );

        admin.setEmailVerifiedUser(Boolean.TRUE);
        admin.setDepositUser(Boolean.TRUE);

        if (demoEnvironment) {
            admin.setDemoScenarioCode(
                    DemoScenarioCodes.RECRUITER_DEMO_USERS
            );
        }

        return admin;
    }
}