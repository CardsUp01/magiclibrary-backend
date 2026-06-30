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
 *  INITIALISATION AUTOMATIQUE - UTILISATEURS MEMBRES DE DÉMONSTRATION
 * =============================================================================
 *
 *  🎯 OBJECTIF :
 *  -----------------------------------------------------------------------------
 *  Cette classe initialise automatiquement les comptes MEMBRE nécessaires à la
 *  démonstration publique de MagicLibrary.
 *
 *  Elle permet de garantir qu'une base Railway vide dispose immédiatement :
 *      - d'un compte administrateur créé par UserInitializer ;
 *      - de comptes membres utilisables pour tester les parcours utilisateur.
 *
 * =============================================================================
 *
 *  🧠 LOGIQUE MÉTIER :
 *  -----------------------------------------------------------------------------
 *  - Vérifie si les comptes membres de démonstration existent déjà
 *  - Récupère le rôle MEMBRE créé par RoleInitializer
 *  - Lit le mot de passe commun des comptes démo depuis l'environnement
 *  - Hash le mot de passe avec BCrypt
 *  - Crée les utilisateurs membres manquants
 *  - Sauvegarde les comptes en base MariaDB
 *
 * =============================================================================
 *
 *  ☁️ CONTEXTE DÉPLOIEMENT (RAILWAY) :
 *  -----------------------------------------------------------------------------
 *  Le mot de passe des comptes membres de démonstration doit être fourni via :
 *
 *      DEMO_MEMBER_PASSWORD
 *
 *  Cette variable doit être configurée dans Railway, onglet Variables.
 *
 *  Les emails des comptes membres correspondent aux identifiants documentés dans
 *  le README public du projet afin de permettre une démo recruteur immédiate.
 *
 * =============================================================================
 *
 *  🔒 SÉCURITÉ :
 *  -----------------------------------------------------------------------------
 *  - Aucun mot de passe n'est hardcodé dans le code source
 *  - Aucun mot de passe n'est affiché dans les logs
 *  - Le mot de passe est hashé avec BCrypt avant sauvegarde
 *  - L'initialisation est idempotente
 *  - Si les comptes existent déjà, aucun secret n'est requis au démarrage
 *
 * =============================================================================
 */
@Configuration
public class MemberInitializer {

    private static final Logger logger = LoggerFactory.getLogger(MemberInitializer.class);

    private static final String ROLE_MEMBRE = "MEMBRE";

    private static final String DEMO_MEMBER_PASSWORD_PROPERTY = "DEMO_MEMBER_PASSWORD";

    private static final String MEMBER_ONE_EMAIL = "lucas.demo@magiclibrary.fr";
    private static final String MEMBER_TWO_EMAIL = "sarah.demo@magiclibrary.fr";

    @Bean
    @Order(3) // 👉 Exécuté après RoleInitializer et UserInitializer
    public CommandLineRunner initDemoMembers(
            UserRepository userRepository,
            RoleRepository roleRepository,
            Environment environment
    ) {

        return args -> {

            // -----------------------------------------------------------------
            // 1) CHECK D'EXISTENCE DES COMPTES MEMBRES
            // -----------------------------------------------------------------
            // Les comptes membres de démonstration sont recherchés par email afin
            // de conserver l'idempotence historique de l'initializer.
            //
            // Architecture DEMO :
            // Si Lucas ou Sarah existent déjà mais ne sont pas encore marqués
            // comme comptes socles de démonstration, le marqueur est ajouté sans
            // modifier le mot de passe, le rôle ou les autres données du compte.
            // -----------------------------------------------------------------
            Optional<User> memberOneOptional = userRepository.findByEmailUser(MEMBER_ONE_EMAIL);
            Optional<User> memberTwoOptional = userRepository.findByEmailUser(MEMBER_TWO_EMAIL);

            boolean memberOneExists = memberOneOptional.isPresent();
            boolean memberTwoExists = memberTwoOptional.isPresent();

            if (memberOneExists) {
                markDemoMemberIfNeeded(userRepository, memberOneOptional.get(), "1");
            }

            if (memberTwoExists) {
                markDemoMemberIfNeeded(userRepository, memberTwoOptional.get(), "2");
            }

            if (memberOneExists && memberTwoExists) {
                logger.info("Comptes membres de démonstration déjà présents.");
                return;
            }

            // -----------------------------------------------------------------
            // 2) RÉCUPÉRATION DU MOT DE PASSE DÉMO DEPUIS L'ENVIRONNEMENT
            // -----------------------------------------------------------------
            // Le mot de passe est un secret.
            // Il ne doit JAMAIS être présent en dur dans le code source.
            //
            // En production Railway :
            //   DEMO_MEMBER_PASSWORD doit être défini dans l'onglet Variables.
            //
            // Si au moins un compte membre doit être créé et que la variable est
            // absente, l'application échoue volontairement au démarrage afin
            // d'éviter la création de comptes faibles ou incohérents.
            // -----------------------------------------------------------------
            String demoMemberPassword = environment.getProperty(DEMO_MEMBER_PASSWORD_PROPERTY);

            if (demoMemberPassword == null || demoMemberPassword.isBlank()) {
                throw new IllegalStateException(
                        "DEMO_MEMBER_PASSWORD est manquant. Impossible de créer les comptes membres de démonstration."
                );
            }

            // -----------------------------------------------------------------
            // 3) RÉCUPÉRATION DU ROLE MEMBRE
            // -----------------------------------------------------------------
            // Ce rôle DOIT exister en base via RoleInitializer.
            // Sinon → erreur volontaire pour signaler une initialisation DB incomplète.
            // -----------------------------------------------------------------
            Role memberRole = roleRepository.findByLabelRole(ROLE_MEMBRE)
                    .orElseThrow(() ->
                            new RuntimeException("ROLE MEMBRE introuvable en base !")
                    );

            // -----------------------------------------------------------------
            // 4) HASH DU MOT DE PASSE
            // -----------------------------------------------------------------
            // BCrypt = standard sécurité Spring Security.
            // Le mot de passe brut n'est jamais sauvegardé ni affiché.
            // -----------------------------------------------------------------
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String hashedPassword = encoder.encode(demoMemberPassword);

            // -----------------------------------------------------------------
            // 5) CRÉATION CONDITIONNELLE DU MEMBRE 1
            // -----------------------------------------------------------------
            // Compte membre utilisé pour tester les fonctionnalités utilisateur :
            // consultation catalogue, emprunts, profil et parcours non admin.
            // -----------------------------------------------------------------
            if (!memberOneExists) {
                User memberOne = new User(
                        memberRole,
                        "M",
                        "Lucas",
                        "Demo",
                        MEMBER_ONE_EMAIL,
                        hashedPassword,
                        true,
                        true,
                        LocalDateTime.now()
                );

                memberOne.setEmailVerifiedUser(true);
                memberOne.setDepositUser(true);
                memberOne.setDemoScenarioCode(DemoScenarioCodes.RECRUITER_DEMO_USERS);

                userRepository.save(memberOne);

                logger.info("Compte membre de démonstration 1 créé avec succès.");
            }

            // -----------------------------------------------------------------
            // 6) CRÉATION CONDITIONNELLE DU MEMBRE 2
            // -----------------------------------------------------------------
            // Second compte membre permettant de tester plusieurs profils sans
            // utiliser le compte administrateur.
            // -----------------------------------------------------------------
            if (!memberTwoExists) {
                User memberTwo = new User(
                        memberRole,
                        "Mme",
                        "Sarah",
                        "Demo",
                        MEMBER_TWO_EMAIL,
                        hashedPassword,
                        true,
                        true,
                        LocalDateTime.now()
                );

                memberTwo.setEmailVerifiedUser(true);
                memberTwo.setDepositUser(true);
                memberTwo.setDemoScenarioCode(DemoScenarioCodes.RECRUITER_DEMO_USERS);

                userRepository.save(memberTwo);

                logger.info("Compte membre de démonstration 2 créé avec succès.");
            }
        };
    }

    // -------------------------------------------------------------------------
    // MARQUAGE DES COMPTES SOCLES DE DÉMONSTRATION
    // -------------------------------------------------------------------------
    /**
     * Ajoute le marqueur de démonstration à un compte membre existant si celui-ci
     * n'est pas encore aligné avec l'architecture demoScenarioCode.
     *
     * Cette mise à niveau est volontairement non destructive :
     *      - aucun mot de passe n'est modifié ;
     *      - aucun rôle n'est modifié ;
     *      - aucune donnée personnelle du compte n'est modifiée ;
     *      - aucun compte n'est supprimé.
     */
    private void markDemoMemberIfNeeded(
            UserRepository userRepository,
            User member,
            String memberNumber
    ) {
        if (!DemoScenarioCodes.RECRUITER_DEMO_USERS.equals(member.getDemoScenarioCode())) {
            member.setDemoScenarioCode(DemoScenarioCodes.RECRUITER_DEMO_USERS);
            userRepository.save(member);
            logger.info("Compte membre de démonstration {} déjà présent et marqué comme compte de démonstration.", memberNumber);
        }
    }
}