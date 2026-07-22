package com.magiclibrary.services.demo;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.magiclibrary.config.DemoResetProperties;

/**
 * Suit la dernière activité connue affectant les scénarios de démonstration.
 *
 * <p>Le contrôle périodique utilise cette information pour éviter de
 * reconstruire les données pendant qu'un recruteur interagit encore avec
 * l'application.</p>
 *
 * <p>Le suivi est conservé en mémoire, ce qui est adapté au déploiement
 * Railway DEMO actuel composé d'une seule instance applicative. Après un
 * redémarrage, la date est réinitialisée et la reconstruction de démarrage
 * garantit le retour à l'état canonique.</p>
 *
 * <p>Cette implémentation devra être remplacée par un stockage ou un verrou
 * partagé uniquement si plusieurs instances applicatives sont déployées
 * simultanément.</p>
 */
@Service
@Profile("demo")
@ConditionalOnProperty(
        prefix = "magiclibrary.demo.reset",
        name = "enabled",
        havingValue = "true"
)
public class DemoActivityTracker {

    private final DemoResetProperties resetProperties;
    private final AtomicReference<Instant> lastActivityAt;

    /**
     * Initialise le suivi avec l'instant de création du composant.
     *
     * <p>Cette valeur empêche le scheduler de considérer immédiatement
     * l'application comme inactive juste après son démarrage.</p>
     *
     * @param resetProperties configuration typée du mécanisme DEMO
     */
    public DemoActivityTracker(DemoResetProperties resetProperties) {
        this.resetProperties = resetProperties;
        this.lastActivityAt = new AtomicReference<>(Instant.now());
    }

    /**
     * Enregistre une activité fonctionnelle significative sur l'instance DEMO.
     *
     * <p>Cette méthode est notamment appelée après une requête ayant modifié
     * avec succès l'état de la démonstration. Elle reporte ainsi le prochain
     * reset automatique afin de laisser au recruteur le temps d'observer le
     * résultat de ses actions.</p>
     */
    public void markActivity() {
        lastActivityAt.set(Instant.now());
    }

    /**
     * Retourne la date de la dernière activité DEMO enregistrée.
     *
     * @return instant de la dernière activité connue
     */
    public Instant getLastActivityAt() {
        return lastActivityAt.get();
    }

    /**
     * Détermine si le délai minimal d'inactivité configuré est atteint.
     *
     * @return {@code true} lorsque la DEMO est inactive depuis une durée au
     *         moins égale à {@code inactivity-delay}
     */
    public boolean isInactive() {
        Instant lastActivity = lastActivityAt.get();
        Duration inactivity = Duration.between(
                lastActivity,
                Instant.now()
        );

        return inactivity.compareTo(
                resetProperties.getInactivityDelay()
        ) >= 0;
    }
}