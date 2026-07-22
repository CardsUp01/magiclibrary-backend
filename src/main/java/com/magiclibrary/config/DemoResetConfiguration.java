package com.magiclibrary.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active l'infrastructure de planification utilisée par la réinitialisation
 * automatique des scénarios de démonstration.
 *
 * <p>Cette configuration repose sur deux protections cumulatives :</p>
 *
 * <ul>
 *     <li>le profil Spring {@code demo} doit être actif ;</li>
 *     <li>la propriété {@code magiclibrary.demo.reset.enabled} doit être
 *     explicitement définie à {@code true}.</li>
 * </ul>
 *
 * <p>Le profil {@code prod} seul ne permet donc jamais d'activer le mécanisme.
 * La future instance CLIENT, configurée uniquement avec le profil
 * {@code prod}, ne chargera pas cette configuration.</p>
 *
 * <p>Cette classe active uniquement le support Spring de {@code @Scheduled}.
 * Elle ne contient aucune logique de contrôle, de suppression ou de
 * reconstruction des données.</p>
 */
@Configuration
@Profile("demo")
@ConditionalOnProperty(
        prefix = "magiclibrary.demo.reset",
        name = "enabled",
        havingValue = "true"
)
@EnableScheduling
public class DemoResetConfiguration {
}