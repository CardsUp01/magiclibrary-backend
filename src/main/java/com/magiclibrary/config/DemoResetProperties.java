package com.magiclibrary.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Centralise les paramètres de fonctionnement du mécanisme de
 * réinitialisation automatique de l'environnement DEMO.
 *
 * <p>Les valeurs sont chargées depuis les propriétés dont le préfixe est
 * {@code magiclibrary.demo.reset}.</p>
 *
 * <p>Cette classe est créée uniquement lorsque :</p>
 *
 * <ul>
 *     <li>le profil Spring {@code demo} est actif ;</li>
 *     <li>la propriété {@code magiclibrary.demo.reset.enabled} vaut
 *     explicitement {@code true}.</li>
 * </ul>
 *
 * <p>Elle ne contient aucune logique de suppression ou de reconstruction.
 * Elle fournit uniquement une configuration typée et centralisée aux
 * composants du mécanisme de réinitialisation.</p>
 */
@Component
@Profile("demo")
@ConditionalOnProperty(
        prefix = "magiclibrary.demo.reset",
        name = "enabled",
        havingValue = "true"
)
@ConfigurationProperties(prefix = "magiclibrary.demo.reset")
public class DemoResetProperties {

    /**
     * Active la reconstruction canonique des scénarios au démarrage de
     * l'application DEMO.
     */
    private boolean startupEnabled;

    /**
     * Active le contrôle périodique de l'intégrité des scénarios DEMO.
     */
    private boolean schedulerEnabled;

    /**
     * Autorise l'action manuelle réservée à l'administrateur DEMO.
     */
    private boolean manualEnabled;

    /**
     * Délai attendu avant la première vérification automatique.
     */
    private Duration initialDelay = Duration.ofSeconds(30);

    /**
     * Intervalle entre deux contrôles automatiques successifs.
     */
    private Duration checkInterval = Duration.ofMinutes(2);

    /**
     * Durée minimale sans activité avant la reconstruction d'un scénario
     * dégradé.
     */
    private Duration inactivityDelay = Duration.ofMinutes(15);

    public boolean isStartupEnabled() {
        return startupEnabled;
    }

    public void setStartupEnabled(boolean startupEnabled) {
        this.startupEnabled = startupEnabled;
    }

    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public void setSchedulerEnabled(boolean schedulerEnabled) {
        this.schedulerEnabled = schedulerEnabled;
    }

    public boolean isManualEnabled() {
        return manualEnabled;
    }

    public void setManualEnabled(boolean manualEnabled) {
        this.manualEnabled = manualEnabled;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(Duration initialDelay) {
        this.initialDelay = requirePositiveDuration(
                initialDelay,
                "initialDelay"
        );
    }

    public Duration getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(Duration checkInterval) {
        this.checkInterval = requirePositiveDuration(
                checkInterval,
                "checkInterval"
        );
    }

    public Duration getInactivityDelay() {
        return inactivityDelay;
    }

    public void setInactivityDelay(Duration inactivityDelay) {
        this.inactivityDelay = requirePositiveDuration(
                inactivityDelay,
                "inactivityDelay"
        );
    }

    /**
     * Refuse les durées absentes, nulles ou négatives afin d'empêcher une
     * configuration incohérente du scheduler.
     *
     * @param duration durée chargée depuis la configuration
     * @param propertyName nom logique de la propriété contrôlée
     * @return durée validée
     * @throws IllegalArgumentException si la durée est absente, nulle ou
     *                                  négative
     */
    private Duration requirePositiveDuration(
            Duration duration,
            String propertyName
    ) {
        if (duration == null
                || duration.isZero()
                || duration.isNegative()) {
            throw new IllegalArgumentException(
                    "La propriété DEMO '"
                            + propertyName
                            + "' doit définir une durée strictement positive."
            );
        }

        return duration;
    }
}