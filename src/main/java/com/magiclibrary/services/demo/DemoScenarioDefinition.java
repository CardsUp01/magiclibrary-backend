package com.magiclibrary.services.demo;

import java.util.Set;

/**
 * Centralise les valeurs métier stables utilisées par les scénarios de
 * démonstration de MagicLibrary.
 *
 * <p>Cette classe constitue la source unique pour les informations partagées
 * entre :</p>
 *
 * <ul>
 *     <li>le contrôle de santé des scénarios ;</li>
 *     <li>la reconstruction des données MariaDB ;</li>
 *     <li>la reconstruction des documents MongoDB ;</li>
 *     <li>les tests automatisés du mécanisme DEMO.</li>
 * </ul>
 *
 * <p>Les codes techniques permettant d'identifier les données persistées
 * restent centralisés dans {@code DemoScenarioCodes}. Cette classe ne les
 * duplique donc pas.</p>
 *
 * <p>L'adresse de l'administrateur n'est volontairement pas définie ici :
 * elle provient de la propriété configurable {@code ADMIN_EMAIL} et ne doit
 * jamais être codée en dur dans la logique DEMO.</p>
 *
 * <p>Les mots de passe ne sont également jamais définis dans cette classe.
 * Ils restent fournis par les propriétés d'environnement prévues pour chaque
 * compte et ne doivent pas être modifiés pendant une réinitialisation.</p>
 */
public final class DemoScenarioDefinition {

    // =========================================================================
    // RÔLES CANONIQUES
    // =========================================================================

    /**
     * Libellé du rôle attendu pour le compte administrateur de démonstration.
     */
    public static final String ADMIN_ROLE_LABEL = "ADMIN";

    /**
     * Libellé du rôle attendu pour les comptes membres Lucas et Sarah.
     */
    public static final String MEMBER_ROLE_LABEL = "MEMBRE";


    // =========================================================================
    // COMPTE ADMINISTRATEUR DE DÉMONSTRATION
    // =========================================================================

    /**
     * Civilité canonique du compte administrateur.
     */
    public static final String ADMIN_CIVILITY = "M";

    /**
     * Prénom canonique du compte administrateur.
     */
    public static final String ADMIN_FIRST_NAME = "Admin";

    /**
     * Nom canonique du compte administrateur.
     */
    public static final String ADMIN_LAST_NAME = "System";

    /**
     * Nom d'affichage canonique du compte administrateur.
     */
    public static final String ADMIN_DISPLAY_NAME = "Admin System";


    // =========================================================================
    // COMPTES MEMBRES DE DÉMONSTRATION
    // =========================================================================

    /**
     * Civilité canonique du compte Lucas.
     */
    public static final String LUCAS_CIVILITY = "M";

    /**
     * Prénom canonique du compte Lucas.
     */
    public static final String LUCAS_FIRST_NAME = "Lucas";

    /**
     * Nom canonique du compte Lucas.
     */
    public static final String LUCAS_LAST_NAME = "Demo";

    /**
     * Adresse stable du compte membre utilisé pour le scénario d'emprunt actif.
     */
    public static final String LUCAS_EMAIL = "lucas.demo@magiclibrary.fr";

    /**
     * Nom d'affichage attendu pour le compte de démonstration Lucas.
     */
    public static final String LUCAS_DISPLAY_NAME = "Lucas Demo";

    /**
     * Civilité canonique du compte Sarah.
     */
    public static final String SARAH_CIVILITY = "Mme";

    /**
     * Prénom canonique du compte Sarah.
     */
    public static final String SARAH_FIRST_NAME = "Sarah";

    /**
     * Nom canonique du compte Sarah.
     */
    public static final String SARAH_LAST_NAME = "Demo";

    /**
     * Adresse stable du compte membre utilisé pour le scénario d'emprunt en
     * retard.
     */
    public static final String SARAH_EMAIL = "sarah.demo@magiclibrary.fr";

    /**
     * Nom d'affichage attendu pour le compte de démonstration Sarah.
     */
    public static final String SARAH_DISPLAY_NAME = "Sarah Demo";

    /**
     * Nombre exact de comptes socles permanents attendu dans le scénario
     * recruteur : l'administrateur, Lucas et Sarah.
     *
     * <p>Cette valeur ne représente pas nécessairement le nombre total
     * d'utilisateurs présent pendant une session. Des comptes temporaires
     * peuvent être créés pour tester l'administration, mais ils doivent être
     * supprimés lors de la réinitialisation suivante.</p>
     */
    public static final int EXPECTED_CANONICAL_USER_COUNT = 3;


    // =========================================================================
    // OBJETS DU CATALOGUE UTILISÉS PAR LES EMPRUNTS DEMO
    // =========================================================================

    /**
     * Référence source du premier livre emprunté par Lucas.
     */
    public static final String LUCAS_BOOK_ONE_SOURCE_REF = "source_ref:L00001";

    /**
     * Référence source du second livre emprunté par Lucas.
     */
    public static final String LUCAS_BOOK_TWO_SOURCE_REF = "source_ref:L00002";

    /**
     * Référence source du DVD emprunté par Sarah.
     */
    public static final String SARAH_DVD_SOURCE_REF = "source_ref:D00001";

    /**
     * Ensemble immuable des références catalogue indispensables aux scénarios
     * d'emprunt DEMO.
     */
    public static final Set<String> REQUIRED_ITEM_SOURCE_REFS = Set.of(
            LUCAS_BOOK_ONE_SOURCE_REF,
            LUCAS_BOOK_TWO_SOURCE_REF,
            SARAH_DVD_SOURCE_REF
    );


    // =========================================================================
    // CARACTÉRISTIQUES CANONIQUES DES EMPRUNTS
    // =========================================================================

    /**
     * Origine technique affectée aux prêts reconstruits automatiquement.
     */
    public static final String LOAN_ORIGIN_SYSTEM = "SYSTEM";

    /**
     * Nombre exact de lignes attendu pour le prêt actif de Lucas.
     */
    public static final int LUCAS_EXPECTED_LOAN_LINE_COUNT = 2;

    /**
     * Nombre exact de lignes attendu pour le prêt en retard de Sarah.
     */
    public static final int SARAH_EXPECTED_LOAN_LINE_COUNT = 1;

    /**
     * Nombre total d'objets indisponibles attendu pour les deux scénarios.
     */
    public static final int EXPECTED_BORROWED_ITEM_COUNT = 3;

    /**
     * Destination interne des notifications liées aux emprunts DEMO.
     */
    public static final String LOAN_NOTIFICATION_ACTION_URL = "/mes-emprunts";

    /**
     * Priorité attendue pour la notification de rappel de Lucas.
     */
    public static final String LUCAS_NOTIFICATION_PRIORITY = "MEDIUM";

    /**
     * Priorité attendue pour la notification de retard de Sarah.
     */
    public static final String SARAH_NOTIFICATION_PRIORITY = "HIGH";

    /**
     * Nombre total de notifications d'emprunt attendu.
     */
    public static final int EXPECTED_LOAN_NOTIFICATION_COUNT = 2;


    // =========================================================================
    // CARACTÉRISTIQUES CANONIQUES DES CONTACTS MONGODB
    // =========================================================================

    /**
     * Origine commune des messages créés depuis le formulaire public.
     */
    public static final String CONTACT_ORIGIN_WEB_FORM = "formulaire-web";

    /**
     * Nombre total de documents Contact attendu dans le scénario DEMO.
     */
    public static final int EXPECTED_CONTACT_COUNT = 8;

    /**
     * Nombre de messages Contact attendus avec le statut NEW.
     */
    public static final int EXPECTED_NEW_CONTACT_COUNT = 3;

    /**
     * Nombre de messages Contact attendus avec le statut ANSWERED.
     */
    public static final int EXPECTED_ANSWERED_CONTACT_COUNT = 5;

    /**
     * Sujets canoniques permettant de vérifier que les huit messages attendus
     * sont présents sans dépendre des identifiants techniques MongoDB.
     */
    public static final Set<String> EXPECTED_CONTACT_SUBJECTS = Set.of(
            "Demande d'information sur l'adhésion",
            "Question sur un emprunt en cours",
            "Signalement d'une erreur dans le catalogue",
            "Remerciement pour la bibliothèque numérique",
            "Proposition de don d'un ouvrage",
            "Demande d'information sur un atelier",
            "Question sur un DVD du catalogue",
            "Suggestion d'amélioration du catalogue"
    );


    // =========================================================================
    // CONSTRUCTION INTERDITE
    // =========================================================================

    /**
     * Empêche l'instanciation d'une classe exclusivement composée de
     * constantes.
     */
    private DemoScenarioDefinition() {
        throw new IllegalStateException(
                "DemoScenarioDefinition est une classe utilitaire non instanciable."
        );
    }
}