package com.magiclibrary.services.demo;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Représente le résultat immuable d'une tentative de réinitialisation des
 * scénarios de démonstration.
 *
 * <p>Ce rapport décrit l'exécution globale du mécanisme sans exposer de
 * détails techniques sensibles. Il peut être utilisé par :</p>
 *
 * <ul>
 *     <li>le déclencheur automatique planifié ;</li>
 *     <li>la reconstruction exécutée au démarrage ;</li>
 *     <li>l'action manuelle réservée à l'administrateur DEMO ;</li>
 *     <li>les journaux applicatifs ;</li>
 *     <li>les tests automatisés.</li>
 * </ul>
 *
 * <p>Une réinitialisation réussie implique que la reconstruction a été
 * exécutée sans erreur et que le contrôle final confirme l'état canonique de
 * l'ensemble des scénarios.</p>
 *
 * <p>Les rapports d'échec peuvent conserver un état initial ou final
 * {@code null} lorsqu'un diagnostic n'a pas pu être exécuté. Cette absence est
 * volontaire et permet de distinguer un état non conforme d'un état impossible
 * à établir.</p>
 *
 * @param successful indique si la réinitialisation complète a réussi
 * @param trigger origine fonctionnelle du déclenchement
 * @param startedAt instant de début de l'opération
 * @param completedAt instant de fin de l'opération
 * @param duration durée totale calculée de l'opération
 * @param beforeReset état des scénarios avant la reconstruction, éventuellement
 *                    {@code null}
 * @param afterReset état des scénarios après la reconstruction, éventuellement
 *                   {@code null}
 * @param messages informations fonctionnelles ou anomalies d'exécution
 */
public record DemoResetReport(
        boolean successful,
        DemoResetTrigger trigger,
        Instant startedAt,
        Instant completedAt,
        Duration duration,
        DemoScenarioHealthReport beforeReset,
        DemoScenarioHealthReport afterReset,
        List<String> messages
) {

    /**
     * Constructeur compact garantissant la cohérence temporelle et
     * l'immutabilité du rapport.
     *
     * <p>Le déclencheur est obligatoire. Les instants absents sont remplacés
     * de manière défensive. La durée est toujours recalculée à partir des
     * instants afin d'éviter toute divergence entre les données temporelles du
     * rapport.</p>
     *
     * <p>La collection de messages est remplacée par une copie immuable. Une
     * valeur {@code null} devient une liste vide.</p>
     *
     * @throws IllegalArgumentException si le déclencheur est absent ou si la
     *                                  date de fin précède la date de début
     */
    public DemoResetReport {
        if (trigger == null) {
            throw new IllegalArgumentException(
                    "Le déclencheur du rapport de reset DEMO ne peut pas être nul."
            );
        }

        startedAt = startedAt == null
                ? Instant.now()
                : startedAt;

        completedAt = completedAt == null
                ? startedAt
                : completedAt;

        if (completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "La date de fin du reset DEMO ne peut pas précéder "
                            + "sa date de début."
            );
        }

        duration = Duration.between(
                startedAt,
                completedAt
        );

        messages = messages == null
                ? List.of()
                : List.copyOf(messages);
    }
}