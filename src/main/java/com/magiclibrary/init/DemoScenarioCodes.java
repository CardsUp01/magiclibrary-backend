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
 * Base relationnelle MariaDB / MySQL :
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
 *      - emprunts canoniques de démonstration ;
 *      - notifications canoniques liées aux emprunts ;
 *      - notifications temporaires créées pendant les tests ;
 *      - messages Contact canoniques MongoDB ;
 *      - messages Contact temporaires créés pendant les tests.
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
 * Distinction canonique / temporaire :
 *      Les données canoniques sont reconstruites dans leur état officiel lors
 *      d'une réinitialisation DEMO.
 *
 *      Les données temporaires sont créées pendant l'utilisation fonctionnelle
 *      de la démonstration. Elles sont supprimées lors de la réinitialisation,
 *      mais ne sont pas recréées.
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
 *      Une donnée temporaire ne peut être supprimée automatiquement que
 *      lorsqu'elle porte explicitement le marqueur dédié. L'absence de
 *      marqueur ne doit jamais être interprétée comme une autorisation de
 *      suppression.
 *
 * Maintenance :
 *      Toute nouvelle donnée scénarisée ou temporaire de démonstration devra
 *      réutiliser ou compléter cette classe afin d'éviter les fautes de frappe
 *      et les divergences entre les composants du mécanisme DEMO.
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
     * Scénario regroupant les notifications canoniques de démonstration liées
     * aux emprunts.
     *
     * Ces notifications sont recréées dans leur état officiel à chaque
     * reconstruction du scénario, sans impacter les notifications non-DEMO.
     */
    public static final String RECRUITER_DEMO_LOAN_NOTIFICATIONS =
            "RECRUITER_DEMO_LOAN_NOTIFICATIONS";

    /**
     * Code associé aux notifications temporaires créées pendant l'utilisation
     * fonctionnelle de la démonstration.
     *
     * Ce marqueur peut notamment identifier :
     *      - les notifications administrateur générées lors de l'envoi d'un
     *        nouveau message Contact ;
     *      - les notifications membre générées lors de la réponse à un message ;
     *      - les notifications créées manuellement pendant les tests ;
     *      - les autres notifications système produites pendant une session
     *        de démonstration.
     *
     * Ces notifications sont supprimées lors de la réinitialisation DEMO et ne
     * sont pas recréées.
     *
     * Ce marqueur ne doit jamais être attribué en dehors de l'environnement
     * DEMO explicitement activé.
     */
    public static final String RECRUITER_DEMO_CREATED_NOTIFICATIONS =
            "RECRUITER_DEMO_CREATED_NOTIFICATIONS";

    // -------------------------------------------------------------------------
    // SCÉNARIOS CONTACT MONGODB
    // -------------------------------------------------------------------------

    /**
     * Scénario regroupant les messages Contact MongoDB canoniques de
     * démonstration.
     *
     * Utilisé par le mécanisme de reconstruction CONTACT DEMO pour :
     *      - nettoyer uniquement les messages Contact canoniques précédents ;
     *      - recréer une collection MongoDB crédible ;
     *      - préserver les messages Contact non-DEMO.
     */
    public static final String RECRUITER_DEMO_CONTACT_MESSAGES =
            "RECRUITER_DEMO_CONTACT_MESSAGES";

    /**
     * Code associé aux messages Contact temporaires créés depuis le formulaire
     * pendant l'utilisation de la démonstration.
     *
     * Ces messages permettent de tester le parcours complet :
     *      - envoi d'un message par un membre ;
     *      - consultation du message côté membre ;
     *      - réception et traitement côté administration ;
     *      - réponse éventuelle de l'administrateur.
     *
     * Ils sont supprimés lors de la réinitialisation DEMO, mais ne sont pas
     * recréés avec les huit messages canoniques.
     *
     * Les messages réels ou créés hors environnement DEMO doivent conserver un
     * marqueur null et ne doivent jamais être ciblés par cette constante.
     */
    public static final String RECRUITER_DEMO_CREATED_CONTACT_MESSAGES =
            "RECRUITER_DEMO_CREATED_CONTACT_MESSAGES";
}