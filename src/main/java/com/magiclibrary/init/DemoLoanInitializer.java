package com.magiclibrary.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import com.magiclibrary.config.DemoResetProperties;
import com.magiclibrary.services.demo.DemoResetReport;
import com.magiclibrary.services.demo.DemoResetService;
import com.magiclibrary.services.demo.DemoResetTrigger;

/**
 * Déclenche la reconstruction canonique des scénarios de démonstration au
 * démarrage de l'application.
 *
 * <p>Cette classe ne contient plus aucune logique métier de suppression ou de
 * création des prêts, lignes d'emprunt, objets et notifications. Ces
 * responsabilités sont désormais centralisées dans les services spécialisés
 * du mécanisme de réinitialisation DEMO.</p>
 *
 * <p>L'initialiseur délègue l'opération complète à {@link DemoResetService}.
 * L'orchestrateur coordonne :</p>
 *
 * <ul>
 *     <li>le contrôle initial de l'état des scénarios ;</li>
 *     <li>la reconstruction transactionnelle des données MariaDB ;</li>
 *     <li>la reconstruction des documents CONTACT MongoDB ;</li>
 *     <li>le contrôle final de conformité ;</li>
 *     <li>la production d'un rapport d'exécution.</li>
 * </ul>
 *
 * <p>Trois protections cumulatives empêchent toute activation accidentelle
 * hors de l'environnement de démonstration :</p>
 *
 * <ul>
 *     <li>le profil Spring {@code demo} doit être actif ;</li>
 *     <li>la propriété {@code magiclibrary.demo.reset.enabled} doit valoir
 *     explicitement {@code true} ;</li>
 *     <li>la propriété
 *     {@code magiclibrary.demo.reset.startup-enabled} doit autoriser le reset
 *     au démarrage.</li>
 * </ul>
 *
 * <p>Le profil {@code prod} utilisé seul, notamment pour une future instance
 * CLIENT, ne charge donc jamais cet initialiseur.</p>
 *
 * <p>L'ordre {@code 4} est conservé afin que les rôles et comptes socles soient
 * initialisés avant le lancement de la reconstruction :</p>
 *
 * <ol>
 *     <li>{@code RoleInitializer} ;</li>
 *     <li>{@code UserInitializer} ;</li>
 *     <li>{@code MemberInitializer} ;</li>
 *     <li>présent initialiseur DEMO.</li>
 * </ol>
 */
@Configuration
@Profile("demo")
@ConditionalOnProperty(
        prefix = "magiclibrary.demo.reset",
        name = "enabled",
        havingValue = "true"
)
public class DemoLoanInitializer {

    private static final Logger logger =
            LoggerFactory.getLogger(DemoLoanInitializer.class);

    /**
     * Crée le déclencheur de reconstruction exécuté après les initialiseurs des
     * données socles.
     *
     * <p>Lorsque le reset au démarrage est désactivé, le runner termine sans
     * modifier les données. Lorsqu'il est activé, l'intégralité de l'opération
     * est déléguée à l'orchestrateur avec le déclencheur
     * {@link DemoResetTrigger#STARTUP}.</p>
     *
     * <p>Le rapport retourné est journalisé sans exposer de secret ni de donnée
     * personnelle. Un échec fonctionnel est signalé explicitement, mais
     * l'initialiseur ne duplique aucune gestion technique des exceptions déjà
     * assurée par l'orchestrateur.</p>
     *
     * @param resetService orchestrateur principal du mécanisme DEMO
     * @param resetProperties configuration typée du reset
     * @return runner Spring Boot chargé du déclenchement au démarrage
     */
    @Bean
    @Order(4)
    public CommandLineRunner initDemoLoans(
            DemoResetService resetService,
            DemoResetProperties resetProperties
    ) {
        return args -> {
            if (!resetProperties.isStartupEnabled()) {
                logger.info(
                        "Réinitialisation DEMO au démarrage désactivée par "
                                + "configuration."
                );
                return;
            }

            logger.info(
                    "Déclenchement de la reconstruction canonique des "
                            + "scénarios DEMO au démarrage."
            );

            DemoResetReport report =
                    resetService.reset(DemoResetTrigger.STARTUP);

            if (report.successful()) {
                logger.info(
                        "Reconstruction DEMO au démarrage terminée avec "
                                + "succès en {} ms.",
                        report.duration().toMillis()
                );
                return;
            }

            logger.error(
                    "La reconstruction DEMO au démarrage n'a pas rétabli "
                            + "l'état canonique complet. Messages : {}",
                    report.messages()
            );
        };
    }
}