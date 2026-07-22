package com.magiclibrary.init;

// -----------------------------------------------------------------------------
// IMPORTS JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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
import com.magiclibrary.services.demo.DemoScenarioDefinition;

/**
 * =============================================================================
 * INITIALISATION AUTOMATIQUE - UTILISATEURS MEMBRES DE DÉMONSTRATION
 * =============================================================================
 *
 * <p>Cette configuration garantit la présence des comptes membres nécessaires
 * aux parcours de démonstration publique de MagicLibrary :</p>
 *
 * <ul>
 *     <li>Lucas, utilisé pour le scénario d'emprunt actif ;</li>
 *     <li>Sarah, utilisée pour le scénario d'emprunt en retard.</li>
 * </ul>
 *
 * <p>Les comptes constituent des données socles permanentes. Ils peuvent être
 * créés ou marqués comme données DEMO, mais ils ne sont jamais supprimés par
 * le mécanisme de reconstruction automatique.</p>
 *
 * <h2>Isolation de l'environnement</h2>
 *
 * <p>La configuration est chargée uniquement lorsque les deux conditions
 * suivantes sont réunies :</p>
 *
 * <ul>
 *     <li>le profil Spring {@code demo} est actif ;</li>
 *     <li>la propriété {@code magiclibrary.demo.reset.enabled} vaut
 *     explicitement {@code true}.</li>
 * </ul>
 *
 * <p>Une instance utilisant uniquement le profil {@code prod}, notamment la
 * future instance CLIENT, ne crée donc jamais Lucas ou Sarah et ne marque
 * aucun compte existant comme donnée de démonstration.</p>
 *
 * <h2>Idempotence</h2>
 *
 * <p>Les comptes sont recherchés par leur email fonctionnel stable :</p>
 *
 * <ul>
 *     <li>un compte absent est créé avec le rôle MEMBRE ;</li>
 *     <li>un compte existant n'est pas recréé ;</li>
 *     <li>un compte existant non marqué reçoit uniquement le marqueur DEMO ;</li>
 *     <li>son mot de passe, son rôle et son identité ne sont pas remplacés.</li>
 * </ul>
 *
 * <h2>Sécurité des mots de passe</h2>
 *
 * <p>Le mot de passe commun des membres de démonstration provient de la
 * propriété d'environnement {@code DEMO_MEMBER_PASSWORD}. Il n'est jamais
 * codé en dur, journalisé ou conservé en clair.</p>
 *
 * <p>Cette propriété est exigée uniquement lorsqu'au moins un compte doit être
 * créé. Lorsque les deux comptes existent déjà, aucun secret n'est requis par
 * cet initialiseur.</p>
 *
 * <h2>Ordre d'exécution</h2>
 *
 * <p>L'ordre {@code 3} garantit l'exécution après :</p>
 *
 * <ol>
 *     <li>{@code RoleInitializer}, qui crée le rôle MEMBRE ;</li>
 *     <li>{@code UserInitializer}, qui prépare le compte administrateur.</li>
 * </ol>
 *
 * <p>Le déclencheur global de reconstruction DEMO s'exécute ensuite à
 * l'ordre {@code 4}.</p>
 */
@Configuration
@Profile("demo")
@ConditionalOnProperty(
        prefix = "magiclibrary.demo.reset",
        name = "enabled",
        havingValue = "true"
)
public class MemberInitializer {

    private static final Logger logger =
            LoggerFactory.getLogger(MemberInitializer.class);

    private static final String ROLE_MEMBRE = "MEMBRE";

    private static final String DEMO_MEMBER_PASSWORD_PROPERTY =
            "DEMO_MEMBER_PASSWORD";

    /**
     * Initialise les comptes membres socles de démonstration.
     *
     * <p>La méthode conserve une logique strictement idempotente. Elle valide
     * d'abord l'existence des deux comptes, marque les comptes déjà présents,
     * puis ne charge le mot de passe et le rôle que lorsqu'une création est
     * réellement nécessaire.</p>
     *
     * @param userRepository repository des comptes utilisateurs
     * @param roleRepository repository des rôles
     * @param environment environnement Spring contenant le secret DEMO
     * @return runner exécuté après les initialiseurs des rôles et de l'admin
     */
    @Bean
    @Order(3)
    public CommandLineRunner initDemoMembers(
            UserRepository userRepository,
            RoleRepository roleRepository,
            Environment environment
    ) {
        return args -> {
            Optional<User> lucasOptional = userRepository.findByEmailUser(
                    DemoScenarioDefinition.LUCAS_EMAIL
            );

            Optional<User> sarahOptional = userRepository.findByEmailUser(
                    DemoScenarioDefinition.SARAH_EMAIL
            );

            boolean lucasExists = lucasOptional.isPresent();
            boolean sarahExists = sarahOptional.isPresent();

            lucasOptional.ifPresent(
                    user -> markDemoMemberIfNeeded(
                            userRepository,
                            user,
                            DemoScenarioDefinition.LUCAS_DISPLAY_NAME
                    )
            );

            sarahOptional.ifPresent(
                    user -> markDemoMemberIfNeeded(
                            userRepository,
                            user,
                            DemoScenarioDefinition.SARAH_DISPLAY_NAME
                    )
            );

            if (lucasExists && sarahExists) {
                logger.info(
                        "Comptes membres socles de démonstration déjà présents."
                );
                return;
            }

            String demoMemberPassword = environment.getProperty(
                    DEMO_MEMBER_PASSWORD_PROPERTY
            );

            if (demoMemberPassword == null
                    || demoMemberPassword.isBlank()) {
                throw new IllegalStateException(
                        "DEMO_MEMBER_PASSWORD est manquant. Impossible de "
                                + "créer les comptes membres de démonstration."
                );
            }

            Role memberRole = roleRepository.findByLabelRole(ROLE_MEMBRE)
                    .orElseThrow(() -> new IllegalStateException(
                            "Le rôle MEMBRE est introuvable. "
                                    + "L'initialisation des rôles est incomplète."
                    ));

            BCryptPasswordEncoder passwordEncoder =
                    new BCryptPasswordEncoder();

            String hashedPassword = passwordEncoder.encode(
                    demoMemberPassword
            );

            if (!lucasExists) {
                User lucas = createDemoMember(
                        memberRole,
                        "M",
                        "Lucas",
                        "Demo",
                        DemoScenarioDefinition.LUCAS_EMAIL,
                        hashedPassword
                );

                userRepository.save(lucas);

                logger.info(
                        "Compte membre de démonstration Lucas créé avec succès."
                );
            }

            if (!sarahExists) {
                User sarah = createDemoMember(
                        memberRole,
                        "Mme",
                        "Sarah",
                        "Demo",
                        DemoScenarioDefinition.SARAH_EMAIL,
                        hashedPassword
                );

                userRepository.save(sarah);

                logger.info(
                        "Compte membre de démonstration Sarah créé avec succès."
                );
            }
        };
    }

    /**
     * Construit un compte membre socle avec les indicateurs nécessaires aux
     * parcours de démonstration.
     *
     * @param role rôle MEMBRE
     * @param civility civilité
     * @param firstName prénom
     * @param lastName nom
     * @param email email fonctionnel stable
     * @param hashedPassword mot de passe BCrypt
     * @return compte complet prêt à être persisté
     */
    private User createDemoMember(
            Role role,
            String civility,
            String firstName,
            String lastName,
            String email,
            String hashedPassword
    ) {
        User member = new User(
                role,
                civility,
                firstName,
                lastName,
                email,
                hashedPassword,
                Boolean.TRUE,
                Boolean.TRUE,
                LocalDateTime.now()
        );

        member.setEmailVerifiedUser(Boolean.TRUE);
        member.setDepositUser(Boolean.TRUE);
        member.setDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_USERS
        );

        return member;
    }

    /**
     * Ajoute le marqueur officiel à un compte membre existant uniquement
     * lorsque celui-ci n'est pas encore identifié comme compte socle DEMO.
     *
     * <p>Cette mise à niveau est non destructive :</p>
     *
     * <ul>
     *     <li>aucun mot de passe n'est modifié ;</li>
     *     <li>aucun rôle n'est modifié ;</li>
     *     <li>aucune information d'identité n'est modifiée ;</li>
     *     <li>aucun compte n'est supprimé.</li>
     * </ul>
     *
     * @param userRepository repository des utilisateurs
     * @param member compte existant
     * @param displayName nom utilisé dans le journal applicatif
     */
    private void markDemoMemberIfNeeded(
            UserRepository userRepository,
            User member,
            String displayName
    ) {
        if (DemoScenarioCodes.RECRUITER_DEMO_USERS.equals(
                member.getDemoScenarioCode()
        )) {
            return;
        }

        member.setDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_USERS
        );

        userRepository.save(member);

        logger.info(
                "Compte membre {} déjà présent et marqué comme compte DEMO.",
                displayName
        );
    }
}