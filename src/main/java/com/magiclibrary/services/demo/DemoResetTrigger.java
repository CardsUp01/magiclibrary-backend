package com.magiclibrary.services.demo;

/**
 * Identifie l'origine fonctionnelle d'une tentative de réinitialisation des
 * scénarios de démonstration.
 *
 * <p>Cette information permet de distinguer clairement les exécutions :</p>
 *
 * <ul>
 *     <li>lancées automatiquement au démarrage ;</li>
 *     <li>déclenchées par le scheduler après une période d'inactivité ;</li>
 *     <li>demandées manuellement par un administrateur DEMO.</li>
 * </ul>
 */
public enum DemoResetTrigger {

    /**
     * Reconstruction demandée pendant le démarrage de l'application DEMO.
     */
    STARTUP,

    /**
     * Reconstruction déclenchée par le contrôle automatique planifié.
     */
    SCHEDULED,

    /**
     * Reconstruction déclenchée par une action manuelle autorisée.
     */
    MANUAL
}