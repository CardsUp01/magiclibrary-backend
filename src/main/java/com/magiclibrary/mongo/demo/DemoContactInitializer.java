package com.magiclibrary.mongo.demo;

import com.magiclibrary.entities.User;
import com.magiclibrary.enums.ContactStatus;
import com.magiclibrary.mongo.documents.ContactDocument;
import com.magiclibrary.mongo.repositories.ContactMongoRepository;
import com.magiclibrary.repositories.interfaces.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * =============================================================================
 * INITIALISATION AUTOMATIQUE - CONTACTS DE DÉMONSTRATION MONGODB
 * =============================================================================
 *
 * Objectif :
 *      Créer automatiquement des messages de contact réalistes dans MongoDB
 *      afin d'alimenter le module CONTACT de démonstration MagicLibrary.
 *
 * Principes :
 *      - aucun identifiant technique MongoDB n'est utilisé ;
 *      - les utilisateurs SQL sont retrouvés dynamiquement par email ;
 *      - l'idempotence repose sur une clé métier naturelle :
 *        email + sujet + origine ;
 *      - aucun doublon n'est créé lors des redémarrages ;
 *      - aucune notification automatique n'est générée ;
 *      - le service métier ContactService n'est pas appelé ;
 *      - l'initializer ne bloque jamais le démarrage de l'application.
 *
 * Rôle dans la démonstration :
 *      - fournir une collection CONTACT crédible pour les recruteurs ;
 *      - montrer des messages nouveaux et déjà répondus ;
 *      - illustrer le lien entre MongoDB et les utilisateurs MariaDB ;
 *      - conserver une démonstration stable, lisible et maintenable.
 *
 * =============================================================================
 */
@Configuration
public class DemoContactInitializer {

    // -------------------------------------------------------------------------
    // LOGGER
    // -------------------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(DemoContactInitializer.class);

    // -------------------------------------------------------------------------
    // CONSTANTES MÉTIER DE DÉMONSTRATION
    // -------------------------------------------------------------------------
    private static final String ORIGIN_WEB_FORM = "formulaire-web";

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String LUCAS_EMAIL = "lucas.demo@magiclibrary.fr";
    private static final String SARAH_EMAIL = "sarah.demo@magiclibrary.fr";

    // -------------------------------------------------------------------------
    // POINT D'ENTRÉE SPRING BOOT
    // -------------------------------------------------------------------------
    /**
     * Lance l'initialisation des contacts de démonstration après les initialiseurs
     * relationnels principaux.
     *
     * L'ordre 5 permet de laisser le temps aux comptes utilisateurs de démonstration
     * d'être créés avant la génération des documents MongoDB.
     */
    @Bean
    @Order(5)
    public CommandLineRunner initDemoContacts(
            ContactMongoRepository contactMongoRepository,
            UserRepository userRepository
    ) {
        return args -> initializeDemoContacts(contactMongoRepository, userRepository);
    }

    // -------------------------------------------------------------------------
    // INITIALISATION PRINCIPALE
    // -------------------------------------------------------------------------
    /**
     * Initialise les messages de contact MongoDB lorsque les comptes de
     * démonstration requis existent déjà en base relationnelle.
     *
     * Si un compte requis par un scénario est absent, seul le scénario concerné
     * est ignoré afin de ne jamais bloquer le démarrage de l'application.
     */
    private void initializeDemoContacts(
            ContactMongoRepository contactMongoRepository,
            UserRepository userRepository
    ) {
        LocalDateTime initializationDate = LocalDateTime.now();
        List<DemoContactScenario> scenarios = buildDemoContactScenarios();

        int createdCount = 0;

        for (DemoContactScenario scenario : scenarios) {
            if (contactAlreadyExists(contactMongoRepository, scenario)) {
                continue;
            }

            Optional<User> senderOptional = userRepository.findByEmailUserWithRole(scenario.senderEmail());

            if (senderOptional.isEmpty()) {
                logger.warn(
                        "Contact de démonstration ignoré : utilisateur expéditeur introuvable pour l'email {}.",
                        scenario.senderEmail()
                );
                continue;
            }

            Optional<User> answeringAdminOptional = resolveAnsweringAdminUser(userRepository, scenario);

            if (!isScenarioConsistent(scenario, answeringAdminOptional)) {
                logger.warn(
                        "Contact de démonstration ignoré : scénario incohérent pour le sujet '{}'.",
                        scenario.subject()
                );
                continue;
            }

            ContactDocument document = createContactDocument(
                    scenario,
                    senderOptional.get(),
                    answeringAdminOptional.orElse(null),
                    initializationDate
            );

            contactMongoRepository.save(document);
            createdCount++;
        }

        if (createdCount == 0) {
            logger.info("Contacts de démonstration déjà présents.");
            return;
        }

        logger.info("{} contact(s) de démonstration MongoDB créé(s) avec succès.", createdCount);
    }

    // -------------------------------------------------------------------------
    // CONTRÔLE D'IDEMPOTENCE
    // -------------------------------------------------------------------------
    /**
     * Vérifie l'existence d'un scénario de démonstration à partir d'une clé
     * métier naturelle stable.
     *
     * Cette stratégie évite toute dépendance à l'ObjectId MongoDB.
     */
    private boolean contactAlreadyExists(
            ContactMongoRepository contactMongoRepository,
            DemoContactScenario scenario
    ) {
        return contactMongoRepository.existsByEmailContactAndSubjectContactAndOriginContact(
                scenario.senderEmail(),
                scenario.subject(),
                scenario.origin()
        );
    }

    // -------------------------------------------------------------------------
    // RÉSOLUTION ET VALIDATION DES RÉPONSES ADMINISTRATEUR
    // -------------------------------------------------------------------------
    /**
     * Résout dynamiquement l'administrateur ayant répondu à un message.
     *
     * Les messages NEW n'ont volontairement aucun administrateur répondant.
     */
    private Optional<User> resolveAnsweringAdminUser(
            UserRepository userRepository,
            DemoContactScenario scenario
    ) {
        if (!ContactStatus.ANSWERED.equals(scenario.status())) {
            return Optional.empty();
        }

        if (scenario.answeredByAdminEmail() == null || scenario.answeredByAdminEmail().isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByEmailUserWithRole(scenario.answeredByAdminEmail());
    }

    /**
     * Vérifie la cohérence métier du scénario avant création du document MongoDB.
     *
     * Un message répondu doit obligatoirement posséder :
     *      - une réponse ;
     *      - un administrateur répondant ;
     *      - une date relative de réponse.
     */
    private boolean isScenarioConsistent(
            DemoContactScenario scenario,
            Optional<User> answeringAdminOptional
    ) {
        if (!ContactStatus.ANSWERED.equals(scenario.status())) {
            return !scenario.responseSent()
                    && scenario.responseContent() == null
                    && scenario.answeredByAdminEmail() == null
                    && scenario.answeredDaysAgo() == null;
        }

        return scenario.responseSent()
                && scenario.responseContent() != null
                && !scenario.responseContent().isBlank()
                && scenario.answeredByAdminEmail() != null
                && !scenario.answeredByAdminEmail().isBlank()
                && scenario.answeredDaysAgo() != null
                && answeringAdminOptional.isPresent();
    }

    // -------------------------------------------------------------------------
    // FABRIQUE DE DOCUMENT MONGODB
    // -------------------------------------------------------------------------
    /**
     * Convertit un scénario de démonstration en document MongoDB persistant.
     *
     * Le document est construit directement sans passer par ContactService afin
     * d'éviter la création automatique de notifications système.
     */
    private ContactDocument createContactDocument(
            DemoContactScenario scenario,
            User sender,
            User answeringAdmin,
            LocalDateTime initializationDate
    ) {
        ContactDocument document = new ContactDocument();

        document.setIdUser(sender.getIdUser());
        document.setNameContact(scenario.senderName());
        document.setEmailContact(scenario.senderEmail());
        document.setSubjectContact(scenario.subject());
        document.setContentContact(scenario.content());
        document.setOriginContact(scenario.origin());
        document.setStatusContact(scenario.status().name());
        document.setDateContact(initializationDate.minusDays(scenario.createdDaysAgo()));
        document.setResponseSentContact(scenario.responseSent());
        document.setResponseContentContact(scenario.responseContent());

        if (ContactStatus.ANSWERED.equals(scenario.status())) {
            document.setAnsweredByUserId(answeringAdmin.getIdUser());
            document.setUpdatedAtContact(initializationDate.minusDays(scenario.answeredDaysAgo()));
        } else {
            document.setAnsweredByUserId(null);
            document.setUpdatedAtContact(null);
        }

        return document;
    }

    // -------------------------------------------------------------------------
    // DONNÉES MÉTIER DE DÉMONSTRATION
    // -------------------------------------------------------------------------
    /**
     * Définit les scénarios de démonstration affichés dans le module CONTACT.
     *
     * Répartition :
     *      - plusieurs messages nouveaux à traiter ;
     *      - plusieurs messages déjà répondus ;
     *      - deux membres de démonstration ;
     *      - des cas métier variés et crédibles.
     */
    private List<DemoContactScenario> buildDemoContactScenarios() {
        return List.of(
                new DemoContactScenario(
                        LUCAS_EMAIL,
                        "Lucas Demo",
                        "Demande d'information sur l'adhésion",
                        "Bonjour, je souhaite en savoir plus sur les modalités d'adhésion à l'association et sur l'accès au catalogue MagicLibrary.",
                        ORIGIN_WEB_FORM,
                        ContactStatus.NEW,
                        false,
                        null,
                        null,
                        2,
                        null
                ),
                new DemoContactScenario(
                        LUCAS_EMAIL,
                        "Lucas Demo",
                        "Question sur un emprunt en cours",
                        "Bonjour, je voudrais savoir s'il est possible de prolonger légèrement la durée de mon emprunt actuel.",
                        ORIGIN_WEB_FORM,
                        ContactStatus.ANSWERED,
                        true,
                        "Bonjour Lucas, votre demande a bien été prise en compte. Un administrateur vérifiera l'emprunt concerné et reviendra vers vous si une prolongation est possible.",
                        ADMIN_EMAIL,
                        7,
                        6L
                ),
                new DemoContactScenario(
                        LUCAS_EMAIL,
                        "Lucas Demo",
                        "Signalement d'une erreur dans le catalogue",
                        "Bonjour, j'ai remarqué une petite incohérence sur une fiche du catalogue. Le statut affiché ne semble pas correspondre à la disponibilité réelle.",
                        ORIGIN_WEB_FORM,
                        ContactStatus.NEW,
                        false,
                        null,
                        null,
                        4,
                        null
                ),
                new DemoContactScenario(
                        LUCAS_EMAIL,
                        "Lucas Demo",
                        "Remerciement pour la bibliothèque numérique",
                        "Bonjour, je voulais simplement remercier l'équipe pour la mise en place du catalogue en ligne. La recherche est claire et très pratique.",
                        ORIGIN_WEB_FORM,
                        ContactStatus.ANSWERED,
                        true,
                        "Bonjour Lucas, merci beaucoup pour votre retour. Nous sommes ravis que la bibliothèque numérique vous soit utile.",
                        ADMIN_EMAIL,
                        14,
                        13L
                ),
                new DemoContactScenario(
                        SARAH_EMAIL,
                        "Sarah Demo",
                        "Proposition de don d'un ouvrage",
                        "Bonjour, je possède un ouvrage sur l'histoire de la magie que je souhaiterais proposer à l'association. Pouvez-vous m'indiquer la marche à suivre ?",
                        ORIGIN_WEB_FORM,
                        ContactStatus.ANSWERED,
                        true,
                        "Bonjour Sarah, merci pour votre proposition. Vous pouvez transmettre les informations de l'ouvrage à l'équipe afin que nous évaluions son intégration au catalogue.",
                        ADMIN_EMAIL,
                        10,
                        9L
                ),
                new DemoContactScenario(
                        SARAH_EMAIL,
                        "Sarah Demo",
                        "Demande d'information sur un atelier",
                        "Bonjour, je voudrais savoir si l'association prévoit prochainement un atelier ou une animation autour des livres de magie.",
                        ORIGIN_WEB_FORM,
                        ContactStatus.ANSWERED,
                        true,
                        "Bonjour Sarah, des animations sont effectivement envisagées. Les prochaines informations seront communiquées aux membres dès que le calendrier sera confirmé.",
                        ADMIN_EMAIL,
                        18,
                        17L
                ),
                new DemoContactScenario(
                        SARAH_EMAIL,
                        "Sarah Demo",
                        "Question sur un DVD du catalogue",
                        "Bonjour, je souhaite consulter un DVD repéré dans le catalogue, mais je voudrais vérifier s'il est bien disponible avant de faire une demande.",
                        ORIGIN_WEB_FORM,
                        ContactStatus.NEW,
                        false,
                        null,
                        null,
                        1,
                        null
                ),
                new DemoContactScenario(
                        SARAH_EMAIL,
                        "Sarah Demo",
                        "Suggestion d'amélioration du catalogue",
                        "Bonjour, une recherche par type de support ou par thème serait très utile pour parcourir plus rapidement les livres et DVD disponibles.",
                        ORIGIN_WEB_FORM,
                        ContactStatus.ANSWERED,
                        true,
                        "Bonjour Sarah, merci pour cette suggestion. Elle est pertinente et pourra être étudiée dans les prochaines évolutions du catalogue.",
                        ADMIN_EMAIL,
                        22,
                        21L
                )
        );
    }
}