package com.magiclibrary.services.demo;

/**
 * Définit le contrat de contrôle de l'état canonique des scénarios DEMO.
 *
 * <p>Le contrôle porte sur l'ensemble des données nécessaires à une
 * démonstration recruteur complète :</p>
 *
 * <ul>
 *     <li>les comptes socles marqués DEMO ;</li>
 *     <li>les prêts et leurs lignes ;</li>
 *     <li>la disponibilité des objets associés ;</li>
 *     <li>les notifications relationnelles ;</li>
 *     <li>les messages du module CONTACT stockés dans MongoDB.</li>
 * </ul>
 *
 * <p>Cette interface ne réalise aucune suppression et aucune reconstruction.
 * Elle produit uniquement un diagnostic exploitable par le scheduler,
 * l'orchestrateur de reset et les tests automatisés.</p>
 */
public interface DemoScenarioHealthService {

    /**
     * Contrôle l'intégrité complète des scénarios DEMO et retourne un rapport
     * détaillé.
     *
     * <p>Une anomalie détectée dans un seul périmètre rend le rapport global
     * non conforme, sans provoquer automatiquement de modification des
     * données.</p>
     *
     * @return rapport non nul décrivant l'état courant des scénarios DEMO
     */
    DemoScenarioHealthReport checkHealth();
}