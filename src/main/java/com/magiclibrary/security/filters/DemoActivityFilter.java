package com.magiclibrary.security.filters;

import java.io.IOException;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.magiclibrary.services.demo.DemoActivityTracker;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Enregistre l'activité HTTP susceptible de modifier l'état fonctionnel de la
 * démonstration.
 *
 * <p>Le filtre permet au scheduler de différer une reconstruction automatique
 * lorsqu'un recruteur vient d'effectuer une opération significative, par
 * exemple :</p>
 *
 * <ul>
 *     <li>créer ou restituer un emprunt ;</li>
 *     <li>modifier une donnée depuis l'administration ;</li>
 *     <li>envoyer ou traiter un message de contact ;</li>
 *     <li>effectuer toute autre requête HTTP modifiant l'état applicatif.</li>
 * </ul>
 *
 * <h2>Requêtes suivies</h2>
 *
 * <p>Seules les méthodes HTTP destinées à modifier un état sont considérées :</p>
 *
 * <ul>
 *     <li>{@code POST} ;</li>
 *     <li>{@code PUT} ;</li>
 *     <li>{@code PATCH} ;</li>
 *     <li>{@code DELETE}.</li>
 * </ul>
 *
 * <p>Les consultations {@code GET}, les requêtes {@code HEAD}, les
 * pré-vérifications CORS {@code OPTIONS} et les requêtes {@code TRACE} sont
 * ignorées. Une visite, un robot d'indexation ou un contrôle de disponibilité
 * ne peut donc pas repousser indéfiniment le reset automatique.</p>
 *
 * <h2>Validation de l'activité</h2>
 *
 * <p>L'activité est enregistrée uniquement après l'exécution complète de la
 * requête et lorsque la réponse HTTP n'est pas une erreur client ou serveur.
 * Une tentative refusée par Spring Security, une erreur de validation ou une
 * exception applicative ne prolonge donc pas le délai d'inactivité.</p>
 *
 * <h2>Isolation de l'environnement</h2>
 *
 * <p>Le filtre est créé uniquement lorsque :</p>
 *
 * <ul>
 *     <li>le profil Spring {@code demo} est actif ;</li>
 *     <li>la propriété {@code magiclibrary.demo.reset.enabled} vaut
 *     explicitement {@code true}.</li>
 * </ul>
 *
 * <p>Il n'est jamais chargé sur une instance CLIENT utilisant le profil
 * {@code prod} seul.</p>
 *
 * <h2>Périmètre technique</h2>
 *
 * <p>Le filtre ne consulte aucune base de données et ne modifie aucune donnée
 * métier. Il met uniquement à jour l'instant conservé en mémoire par
 * {@link DemoActivityTracker}.</p>
 */
@Component
@Profile("demo")
@ConditionalOnProperty(
        prefix = "magiclibrary.demo.reset",
        name = "enabled",
        havingValue = "true"
)
public class DemoActivityFilter extends OncePerRequestFilter {

    private static final Set<String> ACTIVITY_HTTP_METHODS = Set.of(
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name()
    );

    private final DemoActivityTracker activityTracker;

    /**
     * Initialise le filtre avec le tracker partagé par le scheduler.
     *
     * @param activityTracker suivi de la dernière activité DEMO
     */
    public DemoActivityFilter(DemoActivityTracker activityTracker) {
        this.activityTracker = activityTracker;
    }

    /**
     * Exécute la chaîne applicative, puis enregistre l'activité uniquement
     * lorsque la requête modifiante s'est terminée sans erreur HTTP.
     *
     * @param request requête HTTP courante
     * @param response réponse HTTP courante
     * @param filterChain chaîne de filtres
     * @throws ServletException en cas d'erreur de traitement Servlet
     * @throws IOException en cas d'erreur d'entrée-sortie
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        if (response.getStatus() < HttpServletResponse.SC_BAD_REQUEST) {
            activityTracker.markActivity();
        }
    }

    /**
     * Ignore toutes les méthodes HTTP qui ne modifient normalement pas l'état
     * de l'application.
     *
     * @param request requête HTTP courante
     * @return {@code true} lorsque la requête ne doit pas être suivie
     */
    @Override
    protected boolean shouldNotFilter(
            @NonNull HttpServletRequest request
    ) {
        return !ACTIVITY_HTTP_METHODS.contains(request.getMethod());
    }

    /**
     * Évite d'enregistrer une seconde activité lors d'un dispatch d'erreur.
     *
     * @return toujours {@code true}
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    /**
     * Évite d'enregistrer plusieurs fois une même activité lors d'un traitement
     * asynchrone.
     *
     * @return toujours {@code true}
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }
}