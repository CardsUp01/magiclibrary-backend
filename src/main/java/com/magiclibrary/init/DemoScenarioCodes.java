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
 *      - comptes socles permanents de démonstration ;
 *      - comptes temporaires créés pendant l'utilisation de la démo ;
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
 *      Les marqueurs ne doivent jamais remplacer les protections d'activation
 *      du mécanisme de réinitialisation :
 *          - profil Spring demo actif ;
 *          - propriété magiclibrary.demo.reset.enabled=true.
 *
 *      Les comptes temporaires ne peuvent être supprimés automatiquement que
 *      lorsqu'ils portent explicitement le marqueur dédié. L'absence de
 *      marqueur ne doit jamais être interprétée comme une autorisation de
 *      suppression.
 *
 * Maintenance :
 *      Toute nouvelle donnée scénarisée de démonstration devra réutiliser ou
 *      compléter cette classe afin d'éviter les fautes de frappe et les
 *      divergences entre les composants du mécanisme DEMO.
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
     * Cette classe est une classe utilitaire contenant uniquement des
     * constantes. Elle ne doit jamais être instanciée.
     */
    private DemoScenarioCodes() {
        throw new UnsupportedOperationException(
                "Classe utilitaire non instanciable."
        );
    }

    // -------------------------------------------------------------------------
    // COMPTES DU SCÉNARIO RECRUTEUR
    // -------------------------------------------------------------------------

    /**
     * Code associé aux comptes socles permanents de la démonstration destinée
     * aux recruteurs.
     *
     * Ce marqueur identifie les comptes canoniques nécessaires au scénario :
     *      - l'administrateur de démonstration ;
     *      - Lucas ;
     *      - Sarah.
     *
     * Ces comptes doivent être conservés lors d'une reconstruction. Leur état
     * fonctionnel et leur identité canonique peuvent être restaurés, mais ils
     * ne doivent pas être supprimés automatiquement.
     */
    public static final String RECRUITER_DEMO_USERS =
            "RECRUITER_DEMO_USERS";

    /**
     * Code associé aux comptes temporaires créés depuis l'interface lorsque
     * l'application fonctionne dans l'environnement de démonstration
     * recruteur.
     *
     * Ces comptes permettent de tester les parcours d'administration des
     * membres sans modifier durablement l'état canonique de la démonstration.
     *
     * Contrairement aux comptes socles portant
     * {@link #RECRUITER_DEMO_USERS}, ils sont recréables et peuvent être
     * supprimés automatiquement lors d'une réinitialisation DEMO.
     *
     * Ce marqueur ne doit jamais être attribué lorsque le profil
     * {@code demo} n'est pas actif.
     *
     * L'absence de marqueur ne doit jamais être utilisée pour identifier un
     * compte temporaire ou autoriser sa suppression.
     */
    public static final String RECRUITER_DEMO_CREATED_USERS =
            "RECRUITER_DEMO_CREATED_USERS";

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
     * Scénario regroupant les notifications de démonstration liées aux
     * emprunts.
     *
     * Les notifications sont recréables et peuvent être supprimées proprement
     * avant reconstruction, sans impacter les notifications non-DEMO.
     */
    public static final String RECRUITER_DEMO_LOAN_NOTIFICATIONS =
            "RECRUITER_DEMO_LOAN_NOTIFICATIONS";

    // -------------------------------------------------------------------------
    // SCÉNARIOS CONTACT MONGODB
    // -------------------------------------------------------------------------

    /**
     * Scénario regroupant les messages Contact MongoDB de démonstration.
     *
     * Utilisé par le mécanisme de reconstruction CONTACT DEMO pour :
     *      - nettoyer uniquement les messages Contact de démonstration ;
     *      - recréer une collection MongoDB crédible ;
     *      - préserver les messages Contact non-DEMO.
     */
    public static final String RECRUITER_DEMO_CONTACT_MESSAGES =
            "RECRUITER_DEMO_CONTACT_MESSAGES";
}