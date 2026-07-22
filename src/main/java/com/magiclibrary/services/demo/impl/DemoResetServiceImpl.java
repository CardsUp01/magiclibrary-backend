package com.magiclibrary.services.demo.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.magiclibrary.services.demo.DemoMongoScenarioService;
import com.magiclibrary.services.demo.DemoRelationalScenarioService;
import com.magiclibrary.services.demo.DemoResetReport;
import com.magiclibrary.services.demo.DemoResetService;
import com.magiclibrary.services.demo.DemoResetTrigger;
import com.magiclibrary.services.demo.DemoScenarioHealthReport;
import com.magiclibrary.services.demo.DemoScenarioHealthService;

/**
 * Orchestre la réinitialisation complète et contrôlée des scénarios DEMO.
 *
 * <p>Cette classe ne contient directement aucune requête de suppression ni
 * aucune logique de reconstruction métier. Elle coordonne les services
 * spécialisés responsables respectivement :</p>
 *
 * <ul>
 *     <li>du diagnostic de l'état courant des scénarios ;</li>
 *     <li>de la reconstruction transactionnelle des données MariaDB ;</li>
 *     <li>de la reconstruction des documents CONTACT MongoDB ;</li>
 *     <li>du contrôle final de conformité.</li>
 * </ul>
 *
 * <p>Une réinitialisation est considérée comme réussie uniquement lorsque les
 * deux reconstructions se terminent sans exception et que le contrôle final
 * confirme l'état canonique complet des scénarios.</p>
 *
 * <p>Un verrou local empêche deux reconstructions concurrentes dans une même
 * instance applicative. Cette protection correspond au déploiement Railway
 * DEMO actuel composé d'une seule instance. Un verrou distribué devra être
 * ajouté avant tout déploiement simultané de plusieurs instances.</p>
 *
 * <p>Le composant n'est créé que lorsque le profil {@code demo} est actif et
 * que {@code magiclibrary.demo.reset.enabled=true}. Le profil {@code prod}
 * utilisé seul ne peut donc jamais activer ce mécanisme.</p>
 */
@Service
@Profile("demo")
@ConditionalOnProperty(
        prefix = "magiclibrary.demo.reset",
        name = "enabled",
        havingValue = "true"
)
public class DemoResetServiceImpl implements DemoResetService {

    private static final Logger logger =
            LoggerFactory.getLogger(DemoResetServiceImpl.class);

    private final DemoScenarioHealthService healthService;
    private final DemoRelationalScenarioService relationalScenarioService;
    private final DemoMongoScenarioService mongoScenarioService;
    private final ReentrantLock resetLock = new ReentrantLock();

    /**
     * Initialise l'orchestrateur avec les trois services spécialisés requis.
     *
     * @param healthService service de diagnostic global
     * @param relationalScenarioService service de reconstruction MariaDB
     * @param mongoScenarioService service de reconstruction MongoDB
     */
    public DemoResetServiceImpl(
            DemoScenarioHealthService healthService,
            DemoRelationalScenarioService relationalScenarioService,
            DemoMongoScenarioService mongoScenarioService
    ) {
        this.healthService = healthService;
        this.relationalScenarioService = relationalScenarioService;
        this.mongoScenarioService = mongoScenarioService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DemoResetReport reset(DemoResetTrigger trigger) {
        if (trigger == null) {
            throw new IllegalArgumentException(
                    "Le déclencheur du reset DEMO ne peut pas être nul."
            );
        }

        Instant startedAt = Instant.now();
        List<String> messages = new ArrayList<>();

        if (!resetLock.tryLock()) {
            Instant completedAt = Instant.now();

            messages.add(
                    "Réinitialisation ignorée : une autre opération DEMO "
                            + "est déjà en cours."
            );

            logger.warn(
                    "Reset DEMO ignoré pour le déclencheur {} : "
                            + "une reconstruction est déjà en cours.",
                    trigger
            );

            return new DemoResetReport(
                    false,
                    trigger,
                    startedAt,
                    completedAt,
                    null,
                    null,
                    null,
                    messages
            );
        }

        DemoScenarioHealthReport beforeReset = null;
        DemoScenarioHealthReport afterReset;
        boolean successful = false;

        try {
            logger.info(
                    "Début de la réinitialisation DEMO. Déclencheur : {}.",
                    trigger
            );

            beforeReset = healthService.checkHealth();

            if (beforeReset.healthy()) {
                messages.add(
                        "Les scénarios étaient conformes avant reconstruction."
                );
            } else {
                messages.add(
                        "Des anomalies ont été détectées avant reconstruction."
                );
                messages.addAll(beforeReset.anomalies());
            }

            relationalScenarioService.rebuild();

            messages.add(
                    "Reconstruction relationnelle MariaDB terminée."
            );

            mongoScenarioService.rebuild();

            messages.add(
                    "Reconstruction des documents CONTACT MongoDB terminée."
            );

            afterReset = healthService.checkHealth();
            successful = afterReset.healthy();

            if (successful) {
                messages.add(
                        "Le contrôle final confirme l'état canonique complet "
                                + "des scénarios DEMO."
                );

                logger.info(
                        "Réinitialisation DEMO terminée avec succès. "
                                + "Déclencheur : {}.",
                        trigger
                );
            } else {
                messages.add(
                        "La reconstruction s'est terminée, mais le contrôle "
                                + "final détecte encore des anomalies."
                );

                messages.addAll(afterReset.anomalies());

                logger.error(
                        "Réinitialisation DEMO incomplète après contrôle final. "
                                + "Déclencheur : {}. Anomalies : {}",
                        trigger,
                        afterReset.anomalies()
                );
            }
        } catch (RuntimeException exception) {
            messages.add(
                    "Échec de la réinitialisation DEMO : "
                            + safeExceptionMessage(exception)
            );

            logger.error(
                    "Échec de la réinitialisation DEMO. Déclencheur : {}.",
                    trigger,
                    exception
            );

            afterReset = performSafeFinalHealthCheck(messages);
        } finally {
            resetLock.unlock();
        }

        Instant completedAt = Instant.now();

        return new DemoResetReport(
                successful,
                trigger,
                startedAt,
                completedAt,
                null,
                beforeReset,
                afterReset,
                messages
        );
    }

    /**
     * Tente d'établir un état final même lorsqu'une reconstruction a levé une
     * exception.
     *
     * <p>Une seconde exception de diagnostic ne masque jamais l'exception
     * initiale. Elle est uniquement consignée dans le rapport et les
     * journaux.</p>
     *
     * @param messages messages du rapport d'exécution
     * @return rapport de santé final, ou {@code null} si le diagnostic échoue
     */
    private DemoScenarioHealthReport performSafeFinalHealthCheck(
            List<String> messages
    ) {
        try {
            DemoScenarioHealthReport finalHealth =
                    healthService.checkHealth();

            if (!finalHealth.healthy()) {
                messages.add(
                        "Le contrôle de secours confirme que les scénarios "
                                + "restent non conformes."
                );

                messages.addAll(finalHealth.anomalies());
            }

            return finalHealth;
        } catch (RuntimeException healthException) {
            messages.add(
                    "Le contrôle final de secours a également échoué : "
                            + safeExceptionMessage(healthException)
            );

            logger.error(
                    "Impossible d'établir l'état final des scénarios DEMO "
                            + "après l'échec du reset.",
                    healthException
            );

            return null;
        }
    }

    /**
     * Produit un message d'erreur exploitable sans retourner une valeur nulle
     * ni exposer directement une trace technique dans le rapport fonctionnel.
     *
     * @param exception exception interceptée
     * @return message non vide adapté au rapport
     */
    private String safeExceptionMessage(RuntimeException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message;
    }
}