package com.magiclibrary.init;

/**
 * =============================================================================
 * CONSTANTES OFFICIELLES - SCÉNARIOS DE DÉMONSTRATION MAGICLIBRARY
 * =============================================================================
 *
 * Objectif :
 *      Centraliser les codes fonctionnels utilisés pour identifier les données
 *      de démonstration dans MagicLibrary.
 *
 * Rôle architectural :
 *      Cette classe évite de disperser des chaînes de caractères dans plusieurs
 *      initialiseurs, repositories ou services techniques.
 *
 *      Elle garantit que les données de démonstration sont identifiées par un
 *      marqueur stable, explicite et indépendant :
 *          - des identifiants techniques ;
 *          - des emails ;
 *          - des titres ;
 *          - des contenus textuels ;
 *          - des notes ;
 *          - des statuts métier.
 *
 * Base relationnelle MariaDB :
 *      Les entités concernées utilisent la colonne :
 *
 *          demo_scenario_code
 *
 * Base MongoDB :
 *      Les documents concernés utilisent le champ :
 *
 *          demoScenarioCode
 *
 * Données concernées :
 *      - comptes de démonstration ;
 *      - emprunts de démonstration ;
 *      - notifications de démonstration ;
 *      - messages Contact de démonstration.
 *
 * Données non concernées :
 *      - ROLE ;
 *      - ITEM ;
 *      - LOAN_LINE directement.
 *
 * Important :
 *      LOAN_LINE ne possède pas son propre marqueur.
 *      Une ligne d'emprunt est considérée comme donnée de démonstration par
 *      rattachement à son emprunt parent :
 *
 *          LoanLine -> Loan -> demoScenarioCode
 *
 * Sécurité :
 *      Ces constantes ne contiennent aucun secret.
 *      Elles peuvent être versionnées dans GitHub sans risque.
 *
 * Maintenance :
 *      Toute nouvelle donnée scénarisée de démonstration devra réutiliser ou
 *      compléter cette classe afin d'éviter les fautes de frappe et les
 *      divergences entre initializers.
 *
 * =============================================================================
 */
public final class DemoScenarioCodes {

    // -------------------------------------------------------------------------
    // CONSTRUCTEUR PRIVÉ
    // -------------------------------------------------------------------------
    /**
     * Constructeur privé volontaire.
     *
     * Cette classe est une classe utilitaire contenant uniquement des constantes.
     * Elle ne doit jamais être instanciée.
     */
    private DemoScenarioCodes() {
        throw new UnsupportedOperationException("Classe utilitaire non instanciable.");
    }

    // -------------------------------------------------------------------------
    // SCÉNARIO GLOBAL RECRUTEUR
    // -------------------------------------------------------------------------
    /**
     * Code générique du scénario de démonstration destiné aux recruteurs.
     *
     * Peut être utilisé pour identifier les comptes socles de démonstration :
     *      - administrateur ;
     *      - Lucas ;
     *      - Sarah.
     *
     * Ces comptes sont des données permanentes de démonstration.
     * Ils ne doivent pas être supprimés automatiquement lors d'une reconstruction
     * des scénarios.
     */
    public static final String RECRUITER_DEMO_USERS = "RECRUITER_DEMO_USERS";

    // -------------------------------------------------------------------------
    // SCÉNARIOS D'EMPRUNTS
    // -------------------------------------------------------------------------
    /**
     * Scénario d'emprunt actif associé au membre Lucas.
     *
     * Utilisé pour démontrer :
     *      - la consultation des emprunts côté membre ;
     *      - l'affichage d'un emprunt en cours ;
     *      - la cohérence entre LOAN, LOAN_LINE et ITEM ;
     *      - la disponibilité automatiquement ajustée des objets empruntés.
     */
    public static final String RECRUITER_DEMO_LUCAS_ACTIVE_LOAN =
            "RECRUITER_DEMO_LUCAS_ACTIVE_LOAN";

    /**
     * Scénario d'emprunt en retard associé au membre Sarah.
     *
     * Utilisé pour démontrer :
     *      - la gestion d'un emprunt en retard ;
     *      - l'affichage d'un statut LATE ;
     *      - la génération d'une notification de rappel prioritaire ;
     *      - la crédibilité du parcours recruteur côté administration.
     */
    public static final String RECRUITER_DEMO_SARAH_OVERDUE_LOAN =
            "RECRUITER_DEMO_SARAH_OVERDUE_LOAN";

    // -------------------------------------------------------------------------
    // SCÉNARIOS DE NOTIFICATIONS
    // -------------------------------------------------------------------------
    /**
     * Scénario regroupant les notifications de démonstration liées aux emprunts.
     *
     * Les notifications sont recréables et peuvent être supprimées proprement
     * avant reconstruction, sans impacter les notifications réelles.
     */
    public static final String RECRUITER_DEMO_LOAN_NOTIFICATIONS =
            "RECRUITER_DEMO_LOAN_NOTIFICATIONS";

    // -------------------------------------------------------------------------
    // SCÉNARIOS CONTACT MONGODB
    // -------------------------------------------------------------------------
    /**
     * Scénario regroupant les messages Contact MongoDB de démonstration.
     *
     * Utilisé par DemoContactInitializer pour :
     *      - nettoyer uniquement les messages Contact de démonstration ;
     *      - recréer une collection MongoDB crédible ;
     *      - préserver les vrais messages Contact éventuels.
     */
    public static final String RECRUITER_DEMO_CONTACT_MESSAGES =
            "RECRUITER_DEMO_CONTACT_MESSAGES";
}