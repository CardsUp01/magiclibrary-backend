package com.magiclibrary.services.demo;

/**
 * Définit le contrat de reconstruction des données relationnelles appartenant
 * aux scénarios de démonstration.
 *
 * <p>Ce service couvre exclusivement les données stockées dans MariaDB :</p>
 *
 * <ul>
 *     <li>le rétablissement contrôlé des comptes socles DEMO ;</li>
 *     <li>la suppression ciblée des notifications DEMO liées aux emprunts ;</li>
 *     <li>la restauration sécurisée de la disponibilité des objets concernés ;</li>
 *     <li>la suppression des lignes rattachées aux prêts DEMO ;</li>
 *     <li>la suppression des prêts identifiés par leurs marqueurs officiels ;</li>
 *     <li>la reconstruction des prêts, lignes et notifications canoniques.</li>
 * </ul>
 *
 * <p>Les comptes utilisateurs et les objets du catalogue constituent des
 * données socles permanentes. Ils ne doivent jamais être supprimés par cette
 * reconstruction.</p>
 *
 * <p>Les opérations destructives doivent cibler exclusivement les marqueurs
 * {@code demoScenarioCode} définis dans {@code DemoScenarioCodes}. Les emails
 * et références catalogue servent uniquement à retrouver les données socles
 * attendues, jamais à identifier une donnée comme supprimable.</p>
 *
 * <p>La transaction MariaDB est portée par l'implémentation afin que
 * l'ensemble de la reconstruction relationnelle réussisse ou soit annulé
 * comme une seule unité cohérente.</p>
 */
public interface DemoRelationalScenarioService {

    /**
     * Reconstruit l'état canonique complet des scénarios relationnels DEMO.
     *
     * <p>La méthode valide d'abord la présence et la cohérence de toutes les
     * données socles requises avant d'effectuer la moindre suppression.
     * Une donnée indispensable absente ou ambiguë interrompt l'opération et
     * provoque l'annulation de la transaction.</p>
     *
     * @throws IllegalStateException si les comptes, rôles ou objets requis
     *                               sont absents, ambigus ou incohérents
     */
    void rebuild();
}