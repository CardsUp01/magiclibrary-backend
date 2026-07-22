package com.magiclibrary.services.demo.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.magiclibrary.entities.User;
import com.magiclibrary.enums.ContactStatus;
import com.magiclibrary.init.DemoScenarioCodes;
import com.magiclibrary.mongo.demo.DemoContactScenario;
import com.magiclibrary.mongo.documents.ContactDocument;
import com.magiclibrary.mongo.repositories.ContactMongoRepository;
import com.magiclibrary.repositories.interfaces.UserRepository;
import com.magiclibrary.services.demo.DemoMongoScenarioService;
import com.magiclibrary.services.demo.DemoScenarioDefinition;

/**
 * Reconstruit l'état canonique des documents MongoDB du module CONTACT.
 *
 * <p>Cette implémentation remplace la logique de reconstruction historiquement
 * portée par {@code DemoContactInitializer}. Elle est réutilisable par
 * l'orchestrateur de réinitialisation, le démarrage automatique et le
 * scheduler.</p>
 *
 * <p>La reconstruction suit un ordre strictement sécurisé :</p>
 *
 * <ul>
 *     <li>construction des huit définitions canoniques ;</li>
 *     <li>validation complète de chaque définition ;</li>
 *     <li>résolution de tous les utilisateurs SQL requis ;</li>
 *     <li>préparation de tous les documents MongoDB en mémoire ;</li>
 *     <li>suppression exclusive des documents canoniques marqués DEMO ;</li>
 *     <li>suppression exclusive des documents temporaires créés en DEMO ;</li>
 *     <li>enregistrement groupé des huit documents préparés ;</li>
 *     <li>contrôle final du nombre de documents persistés ;</li>
 *     <li>contrôle de l'absence de document temporaire résiduel.</li>
 * </ul>
 *
 * <p>Aucune suppression n'est exécutée avant la validation complète des
 * prérequis. Un utilisateur absent ou une définition incohérente provoque un
 * échec explicite sans modification préalable de la collection.</p>
 *
 * <p>Le caractère DEMO des documents supprimables repose exclusivement sur
 * {@code demoScenarioCode}. Les emails, sujets, statuts, origines et contenus
 * servent uniquement à reconstruire le scénario canonique.</p>
 *
 * <p>Deux catégories de documents sont distinguées :</p>
 *
 * <ul>
 *     <li>les huit documents canoniques, recréés à chaque reset ;</li>
 *     <li>les documents temporaires créés pendant les tests fonctionnels,
 *     supprimés mais jamais recréés.</li>
 * </ul>
 *
 * <p>Les documents sans marqueur DEMO sont systématiquement préservés.</p>
 *
 * <p>Les documents sont construits directement sans passer par
 * {@code ContactService}. Cette séparation empêche la génération de
 * notifications système étrangères au scénario de démonstration.</p>
 */
@Service
@Profile("demo")
@ConditionalOnProperty(
        prefix = "magiclibrary.demo.reset",
        name = "enabled",
        havingValue = "true"
)
public class DemoMongoScenarioServiceImpl
        implements DemoMongoScenarioService {

    private static final Logger logger =
            LoggerFactory.getLogger(DemoMongoScenarioServiceImpl.class);

    private final ContactMongoRepository contactMongoRepository;
    private final UserRepository userRepository;
    private final String adminEmail;

    /**
     * Initialise le service avec les repositories nécessaires et l'adresse
     * configurable du compte administrateur.
     *
     * <p>L'adresse administrateur n'est jamais codée en dur dans la logique de
     * reconstruction. Elle provient de la propriété {@code ADMIN_EMAIL},
     * définie séparément pour chaque environnement.</p>
     *
     * @param contactMongoRepository repository MongoDB des contacts
     * @param userRepository repository relationnel des utilisateurs
     * @param adminEmail adresse du compte administrateur configuré
     */
    public DemoMongoScenarioServiceImpl(
            ContactMongoRepository contactMongoRepository,
            UserRepository userRepository,
            @Value("${ADMIN_EMAIL}") String adminEmail
    ) {
        this.contactMongoRepository = contactMongoRepository;
        this.userRepository = userRepository;
        this.adminEmail = requireNonBlank(
                adminEmail,
                "La propriété ADMIN_EMAIL est absente ou vide."
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rebuild() {
        LocalDateTime initializationDate = LocalDateTime.now();
        List<DemoContactScenario> scenarios = buildDemoContactScenarios();

        validateScenarioDefinitions(scenarios);

        Map<String, User> resolvedUsers = resolveRequiredUsers(scenarios);

        List<ContactDocument> preparedDocuments = prepareDocuments(
                scenarios,
                resolvedUsers,
                initializationDate
        );

        validatePreparedDocuments(preparedDocuments);

        long temporaryDocumentCountBeforeReset =
                contactMongoRepository.countByDemoScenarioCode(
                        DemoScenarioCodes
                                .RECRUITER_DEMO_CREATED_CONTACT_MESSAGES
                );

        deleteDemoContactDocuments();

        contactMongoRepository.saveAll(preparedDocuments);

        verifyPersistedScenario();

        logger.info(
                "{} document(s) CONTACT de démonstration MongoDB "
                        + "reconstruit(s) avec succès ; "
                        + "{} message(s) CONTACT temporaire(s) supprimé(s).",
                preparedDocuments.size(),
                temporaryDocumentCountBeforeReset
        );
    }

    /**
     * Supprime exclusivement les documents Contact explicitement marqués
     * comme données de démonstration.
     *
     * <p>Deux marqueurs sont ciblés :</p>
     *
     * <ul>
     *     <li>le marqueur des huit documents canoniques précédents ;</li>
     *     <li>le marqueur des messages temporaires créés pendant les tests.</li>
     * </ul>
     *
     * <p>Les documents sans marqueur ou portant un autre marqueur ne sont
     * jamais supprimés.</p>
     */
    private void deleteDemoContactDocuments() {
        contactMongoRepository.deleteByDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_CONTACT_MESSAGES
        );

        contactMongoRepository.deleteByDemoScenarioCode(
                DemoScenarioCodes
                        .RECRUITER_DEMO_CREATED_CONTACT_MESSAGES
        );
    }

    // =========================================================================
    // VALIDATION DES DÉFINITIONS
    // =========================================================================

    /**
     * Vérifie la présence, l'unicité et la cohérence métier des huit
     * définitions avant toute opération destructive.
     *
     * @param scenarios définitions canoniques à valider
     * @throws IllegalStateException si le scénario global est incomplet ou
     *                               incohérent
     */
    private void validateScenarioDefinitions(
            List<DemoContactScenario> scenarios
    ) {
        if (scenarios == null
                || scenarios.size()
                != DemoScenarioDefinition.EXPECTED_CONTACT_COUNT) {
            throw new IllegalStateException(
                    "Le scénario CONTACT DEMO doit définir exactement "
                            + DemoScenarioDefinition.EXPECTED_CONTACT_COUNT
                            + " messages."
            );
        }

        long newCount = scenarios.stream()
                .filter(scenario ->
                        ContactStatus.NEW.equals(scenario.status())
                )
                .count();

        long answeredCount = scenarios.stream()
                .filter(scenario ->
                        ContactStatus.ANSWERED.equals(scenario.status())
                )
                .count();

        if (newCount
                != DemoScenarioDefinition.EXPECTED_NEW_CONTACT_COUNT) {
            throw new IllegalStateException(
                    "Le scénario CONTACT DEMO doit définir exactement "
                            + DemoScenarioDefinition.EXPECTED_NEW_CONTACT_COUNT
                            + " messages NEW."
            );
        }

        if (answeredCount
                != DemoScenarioDefinition.EXPECTED_ANSWERED_CONTACT_COUNT) {
            throw new IllegalStateException(
                    "Le scénario CONTACT DEMO doit définir exactement "
                            + DemoScenarioDefinition
                            .EXPECTED_ANSWERED_CONTACT_COUNT
                            + " messages ANSWERED."
            );
        }

        List<String> subjects = scenarios.stream()
                .map(DemoContactScenario::subject)
                .toList();

        if (subjects.stream().anyMatch(Objects::isNull)) {
            throw new IllegalStateException(
                    "Un sujet CONTACT DEMO est nul."
            );
        }

        if (subjects.stream().distinct().count() != scenarios.size()) {
            throw new IllegalStateException(
                    "Les sujets CONTACT DEMO doivent être uniques."
            );
        }

        if (!DemoScenarioDefinition.EXPECTED_CONTACT_SUBJECTS.equals(
                subjects.stream().collect(Collectors.toUnmodifiableSet())
        )) {
            throw new IllegalStateException(
                    "Les sujets CONTACT DEMO ne correspondent pas "
                            + "aux huit définitions canoniques."
            );
        }

        for (DemoContactScenario scenario : scenarios) {
            validateScenario(scenario);
        }
    }

    /**
     * Vérifie la cohérence complète d'une définition individuelle.
     *
     * @param scenario définition à contrôler
     * @throws IllegalStateException si la définition est invalide
     */
    private void validateScenario(DemoContactScenario scenario) {
        if (scenario == null) {
            throw new IllegalStateException(
                    "Une définition CONTACT DEMO est nulle."
            );
        }

        requireScenarioValue(
                scenario.senderEmail(),
                "email expéditeur",
                scenario
        );

        requireScenarioValue(
                scenario.senderName(),
                "nom expéditeur",
                scenario
        );

        requireScenarioValue(
                scenario.subject(),
                "sujet",
                scenario
        );

        requireScenarioValue(
                scenario.content(),
                "contenu",
                scenario
        );

        requireScenarioValue(
                scenario.origin(),
                "origine",
                scenario
        );

        if (!DemoScenarioDefinition.CONTACT_ORIGIN_WEB_FORM.equals(
                scenario.origin()
        )) {
            throw inconsistentScenario(
                    scenario,
                    "origine différente de "
                            + DemoScenarioDefinition.CONTACT_ORIGIN_WEB_FORM
            );
        }

        if (scenario.status() == null) {
            throw inconsistentScenario(
                    scenario,
                    "statut absent"
            );
        }

        if (scenario.createdDaysAgo() < 0) {
            throw inconsistentScenario(
                    scenario,
                    "date relative de création invalide"
            );
        }

        if (ContactStatus.NEW.equals(scenario.status())) {
            validateNewScenario(scenario);
            return;
        }

        if (ContactStatus.ANSWERED.equals(scenario.status())) {
            validateAnsweredScenario(scenario);
            return;
        }

        throw inconsistentScenario(
                scenario,
                "statut non pris en charge : " + scenario.status()
        );
    }

    /**
     * Vérifie qu'un message NEW ne contient aucune donnée de réponse.
     *
     * @param scenario définition NEW à contrôler
     */
    private void validateNewScenario(DemoContactScenario scenario) {
        if (scenario.responseSent()
                || scenario.responseContent() != null
                || scenario.answeredByAdminEmail() != null
                || scenario.answeredDaysAgo() != null) {
            throw inconsistentScenario(
                    scenario,
                    "un message NEW contient des données de réponse"
            );
        }
    }

    /**
     * Vérifie qu'un message ANSWERED contient toutes les informations requises.
     *
     * @param scenario définition ANSWERED à contrôler
     */
    private void validateAnsweredScenario(
            DemoContactScenario scenario
    ) {
        if (!scenario.responseSent()) {
            throw inconsistentScenario(
                    scenario,
                    "la réponse n'est pas marquée comme envoyée"
            );
        }

        requireScenarioValue(
                scenario.responseContent(),
                "contenu de réponse",
                scenario
        );

        requireScenarioValue(
                scenario.answeredByAdminEmail(),
                "email administrateur répondant",
                scenario
        );

        if (!adminEmail.equalsIgnoreCase(
                scenario.answeredByAdminEmail()
        )) {
            throw inconsistentScenario(
                    scenario,
                    "l'administrateur répondant ne correspond pas "
                            + "à la propriété ADMIN_EMAIL"
            );
        }

        if (scenario.answeredDaysAgo() == null
                || scenario.answeredDaysAgo() < 0) {
            throw inconsistentScenario(
                    scenario,
                    "date relative de réponse invalide"
            );
        }

        if (scenario.answeredDaysAgo()
                > scenario.createdDaysAgo()) {
            throw inconsistentScenario(
                    scenario,
                    "la réponse précède chronologiquement "
                            + "la création du message"
            );
        }
    }

    // =========================================================================
    // RÉSOLUTION DES UTILISATEURS
    // =========================================================================

    /**
     * Résout tous les comptes SQL nécessaires avant toute suppression MongoDB.
     *
     * <p>La résolution par email permet de retrouver les données socles
     * attendues. Elle ne sert jamais à identifier les documents supprimables
     * comme données DEMO.</p>
     *
     * @param scenarios définitions validées
     * @return table immuable des utilisateurs résolus par email
     * @throws IllegalStateException si un utilisateur requis est absent
     */
    private Map<String, User> resolveRequiredUsers(
            List<DemoContactScenario> scenarios
    ) {
        Map<String, User> resolvedUsers = new HashMap<>();

        for (DemoContactScenario scenario : scenarios) {
            resolveAndStoreUser(
                    scenario.senderEmail(),
                    resolvedUsers
            );

            if (ContactStatus.ANSWERED.equals(
                    scenario.status()
            )) {
                resolveAndStoreUser(
                        scenario.answeredByAdminEmail(),
                        resolvedUsers
                );
            }
        }

        return Map.copyOf(resolvedUsers);
    }

    /**
     * Résout un compte par email et l'ajoute à la table de travail.
     *
     * @param email email du compte requis
     * @param resolvedUsers table mutable en cours de construction
     */
    private void resolveAndStoreUser(
            String email,
            Map<String, User> resolvedUsers
    ) {
        if (resolvedUsers.containsKey(email)) {
            return;
        }

        User user = userRepository.findByEmailUserWithRole(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Utilisateur requis introuvable pour la "
                                + "reconstruction CONTACT DEMO : "
                                + email + "."
                ));

        resolvedUsers.put(email, user);
    }

    // =========================================================================
    // PRÉPARATION DES DOCUMENTS
    // =========================================================================

    /**
     * Construit tous les documents en mémoire après validation complète des
     * scénarios et résolution des utilisateurs.
     *
     * @param scenarios définitions canoniques validées
     * @param resolvedUsers utilisateurs SQL résolus
     * @param initializationDate instant commun de reconstruction
     * @return liste complète des documents préparés
     */
    private List<ContactDocument> prepareDocuments(
            List<DemoContactScenario> scenarios,
            Map<String, User> resolvedUsers,
            LocalDateTime initializationDate
    ) {
        List<ContactDocument> documents = new ArrayList<>(
                scenarios.size()
        );

        for (DemoContactScenario scenario : scenarios) {
            User sender = resolvedUsers.get(
                    scenario.senderEmail()
            );

            User answeringAdmin = null;

            if (ContactStatus.ANSWERED.equals(
                    scenario.status()
            )) {
                answeringAdmin = resolvedUsers.get(
                        scenario.answeredByAdminEmail()
                );
            }

            documents.add(
                    createContactDocument(
                            scenario,
                            sender,
                            answeringAdmin,
                            initializationDate
                    )
            );
        }

        return List.copyOf(documents);
    }

    /**
     * Convertit une définition canonique en document MongoDB.
     *
     * @param scenario définition validée
     * @param sender utilisateur expéditeur résolu
     * @param answeringAdmin administrateur répondant, ou null pour NEW
     * @param initializationDate instant commun de reconstruction
     * @return document complet prêt à être persisté
     */
    private ContactDocument createContactDocument(
            DemoContactScenario scenario,
            User sender,
            User answeringAdmin,
            LocalDateTime initializationDate
    ) {
        if (sender == null) {
            throw inconsistentScenario(
                    scenario,
                    "utilisateur expéditeur non résolu"
            );
        }

        ContactDocument document = new ContactDocument();

        document.setIdUser(sender.getIdUser());
        document.setNameContact(scenario.senderName());
        document.setEmailContact(scenario.senderEmail());
        document.setSubjectContact(scenario.subject());
        document.setContentContact(scenario.content());
        document.setOriginContact(scenario.origin());
        document.setStatusContact(scenario.status().name());

        document.setDateContact(
                initializationDate.minusDays(
                        scenario.createdDaysAgo()
                )
        );

        document.setResponseSentContact(
                scenario.responseSent()
        );

        document.setResponseContentContact(
                scenario.responseContent()
        );

        document.setDemoScenarioCode(
                DemoScenarioCodes
                        .RECRUITER_DEMO_CONTACT_MESSAGES
        );

        if (ContactStatus.ANSWERED.equals(
                scenario.status()
        )) {
            if (answeringAdmin == null) {
                throw inconsistentScenario(
                        scenario,
                        "administrateur répondant non résolu"
                );
            }

            document.setAnsweredByUserId(
                    answeringAdmin.getIdUser()
            );

            document.setUpdatedAtContact(
                    initializationDate.minusDays(
                            scenario.answeredDaysAgo()
                    )
            );
        } else {
            document.setAnsweredByUserId(null);
            document.setUpdatedAtContact(null);
        }

        return document;
    }

    /**
     * Vérifie les documents préparés avant la suppression de l'ancien scénario.
     *
     * @param documents documents construits en mémoire
     */
    private void validatePreparedDocuments(
            List<ContactDocument> documents
    ) {
        if (documents.size()
                != DemoScenarioDefinition.EXPECTED_CONTACT_COUNT) {
            throw new IllegalStateException(
                    "La préparation CONTACT DEMO a produit "
                            + documents.size()
                            + " document(s) au lieu de "
                            + DemoScenarioDefinition.EXPECTED_CONTACT_COUNT
                            + "."
            );
        }

        boolean invalidMarkerExists = documents.stream()
                .anyMatch(document ->
                        !DemoScenarioCodes
                                .RECRUITER_DEMO_CONTACT_MESSAGES
                                .equals(
                                        document.getDemoScenarioCode()
                                )
                );

        if (invalidMarkerExists) {
            throw new IllegalStateException(
                    "Au moins un document CONTACT préparé ne porte pas "
                            + "le marqueur DEMO officiel."
            );
        }
    }

    // =========================================================================
    // VÉRIFICATION APRÈS PERSISTANCE
    // =========================================================================

    /**
     * Vérifie que MongoDB contient exactement les huit documents canoniques et
     * qu'aucun document temporaire marqué ne subsiste après reconstruction.
     */
    private void verifyPersistedScenario() {
        long persistedCanonicalCount =
                contactMongoRepository.countByDemoScenarioCode(
                        DemoScenarioCodes
                                .RECRUITER_DEMO_CONTACT_MESSAGES
                );

        if (persistedCanonicalCount
                != DemoScenarioDefinition.EXPECTED_CONTACT_COUNT) {
            throw new IllegalStateException(
                    "Reconstruction CONTACT DEMO incomplète : "
                            + persistedCanonicalCount
                            + " document(s) canonique(s) persisté(s) au lieu de "
                            + DemoScenarioDefinition.EXPECTED_CONTACT_COUNT
                            + "."
            );
        }

        long persistedTemporaryCount =
                contactMongoRepository.countByDemoScenarioCode(
                        DemoScenarioCodes
                                .RECRUITER_DEMO_CREATED_CONTACT_MESSAGES
                );

        if (persistedTemporaryCount != 0L) {
            throw new IllegalStateException(
                    "Reconstruction CONTACT DEMO incomplète : "
                            + persistedTemporaryCount
                            + " document(s) CONTACT temporaire(s) "
                            + "subsiste(nt) après le reset."
            );
        }
    }

    // =========================================================================
    // OUTILS DE VALIDATION
    // =========================================================================

    /**
     * Vérifie qu'une propriété de configuration obligatoire n'est pas vide.
     *
     * @param value valeur à contrôler
     * @param errorMessage message utilisé en cas d'échec
     * @return valeur normalisée
     */
    private String requireNonBlank(
            String value,
            String errorMessage
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(errorMessage);
        }

        return value.trim();
    }

    /**
     * Vérifie qu'une valeur textuelle obligatoire d'un scénario est présente.
     *
     * @param value valeur à contrôler
     * @param fieldName nom fonctionnel du champ
     * @param scenario scénario concerné
     */
    private void requireScenarioValue(
            String value,
            String fieldName,
            DemoContactScenario scenario
    ) {
        if (value == null || value.isBlank()) {
            throw inconsistentScenario(
                    scenario,
                    fieldName + " absent ou vide"
            );
        }
    }

    /**
     * Construit une exception cohérente décrivant une définition incorrecte.
     *
     * @param scenario scénario concerné
     * @param reason motif fonctionnel
     * @return exception prête à être levée
     */
    private IllegalStateException inconsistentScenario(
            DemoContactScenario scenario,
            String reason
    ) {
        String subject = scenario == null
                ? "<scénario nul>"
                : String.valueOf(scenario.subject());

        return new IllegalStateException(
                "Scénario CONTACT DEMO incohérent pour le sujet '"
                        + subject + "' : " + reason + "."
        );
    }

    // =========================================================================
    // DÉFINITIONS CANONIQUES
    // =========================================================================

    /**
     * Construit les huit scénarios CONTACT officiels.
     *
     * <p>Les dates sont relatives à l'instant de reconstruction afin de
     * conserver une démonstration crédible après chaque reset.</p>
     *
     * @return liste immuable des huit définitions canoniques
     */
    private List<DemoContactScenario> buildDemoContactScenarios() {
        return List.of(
                new DemoContactScenario(
                        DemoScenarioDefinition.LUCAS_EMAIL,
                        DemoScenarioDefinition.LUCAS_DISPLAY_NAME,
                        "Demande d'information sur l'adhésion",
                        "Bonjour, je souhaite en savoir plus sur les modalités "
                                + "d'adhésion à l'association et sur l'accès "
                                + "au catalogue MagicLibrary.",
                        DemoScenarioDefinition.CONTACT_ORIGIN_WEB_FORM,
                        ContactStatus.NEW,
                        false,
                        null,
                        null,
                        2,
                        null
                ),
                new DemoContactScenario(
                        DemoScenarioDefinition.LUCAS_EMAIL,
                        DemoScenarioDefinition.LUCAS_DISPLAY_NAME,
                        "Question sur un emprunt en cours",
                        "Bonjour, je voudrais savoir s'il est possible de "
                                + "prolonger légèrement la durée de mon "
                                + "emprunt actuel.",
                        DemoScenarioDefinition.CONTACT_ORIGIN_WEB_FORM,
                        ContactStatus.ANSWERED,
                        true,
                        "Bonjour Lucas, votre demande a bien été prise en "
                                + "compte. Un administrateur vérifiera "
                                + "l'emprunt concerné et reviendra vers vous "
                                + "si une prolongation est possible.",
                        adminEmail,
                        7,
                        6L
                ),
                new DemoContactScenario(
                        DemoScenarioDefinition.LUCAS_EMAIL,
                        DemoScenarioDefinition.LUCAS_DISPLAY_NAME,
                        "Signalement d'une erreur dans le catalogue",
                        "Bonjour, j'ai remarqué une petite incohérence sur une "
                                + "fiche du catalogue. Le statut affiché ne "
                                + "semble pas correspondre à la disponibilité "
                                + "réelle.",
                        DemoScenarioDefinition.CONTACT_ORIGIN_WEB_FORM,
                        ContactStatus.NEW,
                        false,
                        null,
                        null,
                        4,
                        null
                ),
                new DemoContactScenario(
                        DemoScenarioDefinition.LUCAS_EMAIL,
                        DemoScenarioDefinition.LUCAS_DISPLAY_NAME,
                        "Remerciement pour la bibliothèque numérique",
                        "Bonjour, je voulais simplement remercier l'équipe "
                                + "pour la mise en place du catalogue en ligne. "
                                + "La recherche est claire et très pratique.",
                        DemoScenarioDefinition.CONTACT_ORIGIN_WEB_FORM,
                        ContactStatus.ANSWERED,
                        true,
                        "Bonjour Lucas, merci beaucoup pour votre retour. "
                                + "Nous sommes ravis que la bibliothèque "
                                + "numérique vous soit utile.",
                        adminEmail,
                        14,
                        13L
                ),
                new DemoContactScenario(
                        DemoScenarioDefinition.SARAH_EMAIL,
                        DemoScenarioDefinition.SARAH_DISPLAY_NAME,
                        "Proposition de don d'un ouvrage",
                        "Bonjour, je possède un ouvrage sur l'histoire de la "
                                + "magie que je souhaiterais proposer à "
                                + "l'association. Pouvez-vous m'indiquer la "
                                + "marche à suivre ?",
                        DemoScenarioDefinition.CONTACT_ORIGIN_WEB_FORM,
                        ContactStatus.ANSWERED,
                        true,
                        "Bonjour Sarah, merci pour votre proposition. Vous "
                                + "pouvez transmettre les informations de "
                                + "l'ouvrage à l'équipe afin que nous évaluions "
                                + "son intégration au catalogue.",
                        adminEmail,
                        10,
                        9L
                ),
                new DemoContactScenario(
                        DemoScenarioDefinition.SARAH_EMAIL,
                        DemoScenarioDefinition.SARAH_DISPLAY_NAME,
                        "Demande d'information sur un atelier",
                        "Bonjour, je voudrais savoir si l'association prévoit "
                                + "prochainement un atelier ou une animation "
                                + "autour des livres de magie.",
                        DemoScenarioDefinition.CONTACT_ORIGIN_WEB_FORM,
                        ContactStatus.ANSWERED,
                        true,
                        "Bonjour Sarah, des animations sont effectivement "
                                + "envisagées. Les prochaines informations "
                                + "seront communiquées aux membres dès que le "
                                + "calendrier sera confirmé.",
                        adminEmail,
                        18,
                        17L
                ),
                new DemoContactScenario(
                        DemoScenarioDefinition.SARAH_EMAIL,
                        DemoScenarioDefinition.SARAH_DISPLAY_NAME,
                        "Question sur un DVD du catalogue",
                        "Bonjour, je souhaite consulter un DVD repéré dans le "
                                + "catalogue, mais je voudrais vérifier s'il "
                                + "est bien disponible avant de faire une "
                                + "demande.",
                        DemoScenarioDefinition.CONTACT_ORIGIN_WEB_FORM,
                        ContactStatus.NEW,
                        false,
                        null,
                        null,
                        1,
                        null
                ),
                new DemoContactScenario(
                        DemoScenarioDefinition.SARAH_EMAIL,
                        DemoScenarioDefinition.SARAH_DISPLAY_NAME,
                        "Suggestion d'amélioration du catalogue",
                        "Bonjour, une recherche par type de support ou par "
                                + "thème serait très utile pour parcourir plus "
                                + "rapidement les livres et DVD disponibles.",
                        DemoScenarioDefinition.CONTACT_ORIGIN_WEB_FORM,
                        ContactStatus.ANSWERED,
                        true,
                        "Bonjour Sarah, merci pour cette suggestion. Elle est "
                                + "pertinente et pourra être étudiée dans les "
                                + "prochaines évolutions du catalogue.",
                        adminEmail,
                        22,
                        21L
                )
        );
    }
}