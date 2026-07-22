package com.magiclibrary.services.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.magiclibrary.config.DemoResetProperties;

/**
 * Surveille périodiquement l'intégrité des scénarios de démonstration et
 * déclenche leur reconstruction lorsque celle-ci devient nécessaire.
 *
 * <p>Le scheduler ne réinitialise jamais systématiquement les données à chaque
 * passage. Il applique successivement trois contrôles :</p>
 *
 * <ol>
 *     <li>la surveillance automatique doit être activée ;</li>
 *     <li>le contrôle de santé doit détecter un scénario non conforme ;</li>
 *     <li>l'application doit être inactive depuis le délai configuré.</li>
 * </ol>
 *
 * <p>Cette stratégie permet au recruteur de manipuler normalement la
 * démonstration et d'observer le résultat de ses actions avant le retour à
 * l'état canonique.</p>
 *
 * <h2>Protection de l'environnement</h2>
 *
 * <p>Le composant est chargé uniquement lorsque :</p>
 *
 * <ul>
 *     <li>le profil Spring {@code demo} est actif ;</li>
 *     <li>la propriété {@code magiclibrary.demo.reset.enabled} vaut
 *     explicitement {@code true}.</li>
 * </ul>
 *
 * <p>Une instance CLIENT utilisant le profil {@code prod} seul ne charge donc
 * jamais ce scheduler.</p>
 *
 * <h2>Concurrence</h2>
 *
 * <p>Le scheduler délègue la reconstruction à {@link DemoResetService}.
 * L'orchestrateur protège lui-même l'opération contre deux exécutions
 * simultanées. Le scheduler ne duplique donc aucune logique de verrouillage.</p>
 *
 * <h2>Gestion des erreurs</h2>
 *
 * <p>Une erreur de contrôle de santé est journalisée puis reportée au passage
 * suivant. Le scheduler ne déclenche jamais une suppression sur la seule base
 * d'un contrôle impossible à établir.</p>
 */
@Component
@Profile("demo")
@ConditionalOnProperty(
        prefix = "magiclibrary.demo.reset",
        name = "enabled",
        havingValue = "true"
)
public class DemoResetScheduler {

    private static final Logger logger =
            LoggerFactory.getLogger(DemoResetScheduler.class);

    private final DemoResetProperties resetProperties;
    private final DemoActivityTracker activityTracker;
    private final DemoScenarioHealthService healthService;
    private final DemoResetService resetService;

    /**
     * Initialise le scheduler avec la configuration et les services DEMO.
     *
     * @param resetProperties configuration typée du mécanisme de reset
     * @param activityTracker suivi de la dernière activité fonctionnelle
     * @param healthService service de contrôle des scénarios
     * @param resetService orchestrateur de reconstruction
     */
    public DemoResetScheduler(
            DemoResetProperties resetProperties,
            DemoActivityTracker activityTracker,
            DemoScenarioHealthService healthService,
            DemoResetService resetService
    ) {
        this.resetProperties = resetProperties;
        this.activityTracker = activityTracker;
        this.healthService = healthService;
        this.resetService = resetService;
    }

    /**
     * Contrôle périodiquement l'état global de la démonstration.
     *
     * <p>Les durées sont lues directement depuis les propriétés Spring :</p>
     *
     * <ul>
     *     <li>{@code magiclibrary.demo.reset.initial-delay} ;</li>
     *     <li>{@code magiclibrary.demo.reset.check-interval}.</li>
     * </ul>
     *
     * <p>Le mode {@code fixedDelay} démarre le délai suivant seulement après la
     * fin du contrôle courant. Deux passages de ce scheduler ne se chevauchent
     * donc pas sur un même thread de planification.</p>
     */
    @Scheduled(
            initialDelayString =
                    "${magiclibrary.demo.reset.initial-delay:PT30S}",
            fixedDelayString =
                    "${magiclibrary.demo.reset.check-interval:PT2M}"
    )
    public void checkAndRepairDemoScenario() {
        if (!resetProperties.isSchedulerEnabled()) {
            logger.debug(
                    "Surveillance automatique DEMO désactivée par "
                            + "configuration."
            );
            return;
        }

        DemoScenarioHealthReport healthReport;

        try {
            healthReport = healthService.checkHealth();
        } catch (RuntimeException healthException) {
            logger.error(
                    "Contrôle périodique DEMO impossible. "
                            + "Aucune reconstruction n'est déclenchée.",
                    healthException
            );
            return;
        }

        if (healthReport.healthy()) {
            logger.debug(
                    "Contrôle périodique DEMO conforme : aucune "
                            + "reconstruction nécessaire."
            );
            return;
        }

        if (!activityTracker.isInactive()) {
            logger.info(
                    "Scénario DEMO dégradé, mais reconstruction différée : "
                            + "une activité récente a été détectée. "
                            + "Anomalies : {}",
                    healthReport.anomalies()
            );
            return;
        }

        logger.warn(
                "Scénario DEMO dégradé après expiration du délai "
                        + "d'inactivité. Déclenchement de la reconstruction. "
                        + "Anomalies : {}",
                healthReport.anomalies()
        );

        DemoResetReport resetReport =
                resetService.reset(DemoResetTrigger.SCHEDULED);

        if (resetReport.successful()) {
            logger.info(
                    "Reconstruction automatique DEMO terminée avec succès "
                            + "en {} ms.",
                    resetReport.duration().toMillis()
            );
            return;
        }

        logger.error(
                "La reconstruction automatique DEMO n'a pas rétabli "
                        + "l'état canonique complet. Messages : {}",
                resetReport.messages()
        );
    }
}