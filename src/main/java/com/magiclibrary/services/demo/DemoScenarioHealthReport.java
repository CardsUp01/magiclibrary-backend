package com.magiclibrary.services.demo;

import java.time.Instant;
import java.util.List;

/**
 * Représente le résultat immuable d'un contrôle de santé des scénarios DEMO.
 *
 * <p>Ce rapport distingue les différents périmètres contrôlés afin de rendre
 * les diagnostics exploitables dans les journaux, les tests et le mécanisme
 * de réinitialisation automatique.</p>
 *
 * @param healthy indique si l'ensemble des scénarios est conforme
 * @param usersHealthy indique si les comptes socles DEMO sont conformes
 * @param loansHealthy indique si les prêts, lignes et objets sont conformes
 * @param notificationsHealthy indique si les notifications DEMO sont conformes
 * @param contactsHealthy indique si les documents MongoDB DEMO sont conformes
 * @param anomalies liste descriptive et non nulle des anomalies détectées
 * @param checkedAt instant auquel le contrôle s'est terminé
 */
public record DemoScenarioHealthReport(
        boolean healthy,
        boolean usersHealthy,
        boolean loansHealthy,
        boolean notificationsHealthy,
        boolean contactsHealthy,
        List<String> anomalies,
        Instant checkedAt
) {

    /**
     * Constructeur compact assurant l'immutabilité réelle de la liste
     * d'anomalies et la validité minimale du rapport.
     */
    public DemoScenarioHealthReport {
        anomalies = anomalies == null
                ? List.of()
                : List.copyOf(anomalies);

        checkedAt = checkedAt == null
                ? Instant.now()
                : checkedAt;
    }
}