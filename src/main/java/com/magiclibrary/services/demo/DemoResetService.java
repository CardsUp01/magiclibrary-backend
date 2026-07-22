package com.magiclibrary.services.demo;

/**
 * Définit le contrat d'orchestration de la réinitialisation complète des
 * scénarios de démonstration.
 *
 * <p>Le service d'orchestration coordonne les opérations suivantes :</p>
 *
 * <ul>
 *     <li>contrôler l'état initial des scénarios DEMO ;</li>
 *     <li>reconstruire les données relationnelles MariaDB ;</li>
 *     <li>reconstruire les documents CONTACT MongoDB ;</li>
 *     <li>effectuer un nouveau contrôle de santé après reconstruction ;</li>
 *     <li>produire un rapport complet et immuable.</li>
 * </ul>
 *
 * <p>Cette interface ne définit aucune règle de planification. Le déclenchement
 * au démarrage, le scheduler et l'action manuelle utilisent ce même contrat
 * afin d'éviter toute duplication de logique.</p>
 *
 * <p>Le caractère DEMO des données ne doit jamais être déduit d'un email,
 * d'un titre, d'un contenu textuel ou d'un identifiant technique. Les
 * opérations destructives restent exclusivement fondées sur les marqueurs
 * officiels centralisés dans {@code DemoScenarioCodes}.</p>
 */
public interface DemoResetService {

    /**
     * Exécute une tentative complète de réinitialisation des scénarios DEMO.
     *
     * <p>L'origine du déclenchement est obligatoire afin de rendre l'opération
     * traçable dans le rapport et dans les journaux applicatifs.</p>
     *
     * <p>L'implémentation empêche deux réinitialisations concurrentes au sein
     * d'une même instance applicative.</p>
     *
     * @param trigger origine fonctionnelle non nulle du déclenchement
     * @return rapport non nul décrivant le résultat complet de l'opération
     * @throws IllegalArgumentException si le déclencheur est nul
     */
    DemoResetReport reset(DemoResetTrigger trigger);
}