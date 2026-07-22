package com.magiclibrary.services.demo;

/**
 * Définit le contrat de reconstruction des documents MongoDB appartenant aux
 * scénarios de démonstration du module CONTACT.
 *
 * <p>Ce service couvre exclusivement les documents stockés dans la collection
 * MongoDB {@code contact} et portant le marqueur officiel du scénario CONTACT
 * de démonstration.</p>
 *
 * <p>La reconstruction doit respecter l'ordre sécurisé suivant :</p>
 *
 * <ul>
 *     <li>construire la définition complète des scénarios canoniques ;</li>
 *     <li>valider tous les expéditeurs et administrateurs requis ;</li>
 *     <li>valider la cohérence métier de chaque scénario ;</li>
 *     <li>préparer tous les documents en mémoire ;</li>
 *     <li>supprimer uniquement les documents marqués DEMO ;</li>
 *     <li>enregistrer l'ensemble des documents préparés ;</li>
 *     <li>vérifier que le nombre attendu de documents a été persisté.</li>
 * </ul>
 *
 * <p>Aucune suppression ne doit être déclenchée tant que tous les prérequis
 * relationnels et toutes les définitions de scénarios n'ont pas été validés.</p>
 *
 * <p>Le caractère DEMO d'un document ne doit jamais être déduit de l'email,
 * du sujet, de l'origine, du statut ou du contenu du message. Toute suppression
 * doit utiliser exclusivement le champ {@code demoScenarioCode} et les
 * constantes officielles définies dans {@code DemoScenarioCodes}.</p>
 *
 * <p>Le service construit directement les {@code ContactDocument} sans passer
 * par {@code ContactService}, afin d'éviter la génération automatique de
 * notifications système étrangères au scénario canonique.</p>
 */
public interface DemoMongoScenarioService {

    /**
     * Reconstruit l'état canonique complet des documents CONTACT DEMO.
     *
     * <p>Une donnée relationnelle requise absente, un scénario incohérent ou un
     * nombre de documents persistés incorrect doit provoquer un échec explicite
     * de l'opération.</p>
     *
     * @throws IllegalStateException si les utilisateurs requis sont absents,
     *                               si une définition est incohérente ou si la
     *                               reconstruction finale est incomplète
     */
    void rebuild();
}