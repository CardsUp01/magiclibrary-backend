package com.magiclibrary.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.magiclibrary.services.demo.DemoResetReport;
import com.magiclibrary.services.demo.DemoResetService;
import com.magiclibrary.services.demo.DemoResetTrigger;

/**
 * Contrôleur SSR réservé à la réinitialisation manuelle de la démonstration.
 *
 * <p>Ce contrôleur ne contient aucune logique de suppression ou de
 * reconstruction. Il délègue exclusivement l'opération à
 * {@link DemoResetService}, qui coordonne :</p>
 *
 * <ul>
 *     <li>le contrôle initial de l'état des scénarios ;</li>
 *     <li>la reconstruction transactionnelle MariaDB ;</li>
 *     <li>la reconstruction des contacts MongoDB ;</li>
 *     <li>le contrôle final de conformité ;</li>
 *     <li>la production du rapport d'exécution.</li>
 * </ul>
 *
 * <h2>Protections cumulatives</h2>
 *
 * <p>Le contrôleur est chargé uniquement lorsque :</p>
 *
 * <ul>
 *     <li>le profil Spring {@code demo} est actif ;</li>
 *     <li>{@code magiclibrary.demo.reset.enabled=true} ;</li>
 *     <li>{@code magiclibrary.demo.reset.manual-enabled=true}.</li>
 * </ul>
 *
 * <p>L'action HTTP exige également le rôle {@code ADMIN}. Une instance CLIENT
 * utilisant le profil {@code prod} seul ne charge donc jamais ce contrôleur.</p>
 *
 * <h2>Réponse SSR</h2>
 *
 * <p>Après l'opération, le contrôleur redirige vers la page d'accueil, où se
 * trouve désormais l'action globale de réinitialisation, et transmet un message
 * temporaire décrivant le résultat.</p>
 */
@Controller
@Profile("demo")
@ConditionalOnProperty(
        prefix = "magiclibrary.demo.reset",
        name = {
                "enabled",
                "manual-enabled"
        },
        havingValue = "true"
)
public class DemoResetController {

    private static final Logger logger =
            LoggerFactory.getLogger(DemoResetController.class);

    private static final String HOME_REDIRECT =
            "redirect:/accueil";

    private final DemoResetService resetService;

    /**
     * Initialise le contrôleur avec l'orchestrateur principal du mécanisme
     * DEMO.
     *
     * @param resetService orchestrateur de réinitialisation
     */
    public DemoResetController(DemoResetService resetService) {
        this.resetService = resetService;
    }

    /**
     * Déclenche manuellement la reconstruction complète des scénarios DEMO.
     *
     * <p>Le verrouillage contre les exécutions concurrentes, la gestion des
     * erreurs et les contrôles de santé sont assurés par l'orchestrateur.</p>
     *
     * @param redirectAttributes attributs temporaires conservés pendant la
     *                           redirection SSR
     * @return redirection vers la page d'accueil
     */
    @PostMapping("/admin/demo/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public String resetDemoScenario(
            RedirectAttributes redirectAttributes
    ) {
        logger.info(
                "Demande administrateur de réinitialisation manuelle DEMO."
        );

        DemoResetReport report =
                resetService.reset(DemoResetTrigger.MANUAL);

        if (report.successful()) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "La démonstration a été réinitialisée avec succès."
            );

            logger.info(
                    "Réinitialisation manuelle DEMO terminée avec succès "
                            + "en {} ms.",
                    report.duration().toMillis()
            );

            return HOME_REDIRECT;
        }

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "La réinitialisation de la démonstration n'a pas pu rétablir "
                        + "l'état attendu."
        );

        logger.error(
                "Échec de la réinitialisation manuelle DEMO. Messages : {}",
                report.messages()
        );

        return HOME_REDIRECT;
    }
}