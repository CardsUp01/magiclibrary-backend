package com.magiclibrary.controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.magiclibrary.dto.user.UserResponseDTO;
import com.magiclibrary.services.UserService;

/**
 * Contrôleur SSR dédié à l'archivage et à la restauration des utilisateurs.
 *
 * Responsabilités :
 *      - afficher les utilisateurs archivés ;
 *      - permettre leur recherche, leur filtrage et leur tri ;
 *      - archiver logiquement un utilisateur actif ;
 *      - restaurer un utilisateur précédemment archivé.
 *
 * L'archivage ne supprime aucune donnée.
 * Il repose exclusivement sur le champ activeUser :
 *
 *      true  -> utilisateur actif
 *      false -> utilisateur archivé
 *
 * Toutes les routes sont strictement réservées aux administrateurs.
 */
@Controller
public class AdminMemberArchivePageController {

    /*
     * Taille standard utilisée pour rester cohérent avec
     * la page principale /admin/membres.
     */
    private static final int ARCHIVED_MEMBERS_PAGE_SIZE = 9;

    private final UserService userService;

    public AdminMemberArchivePageController(UserService userService) {
        this.userService = userService;
    }

    // -------------------------------------------------------------------------
    // LISTE DES UTILISATEURS ARCHIVÉS
    // -------------------------------------------------------------------------

    /**
     * Affiche la liste paginée des utilisateurs archivés.
     *
     * Le statut INACTIF est imposé côté serveur :
     * il n'est donc pas possible de faire apparaître ici un utilisateur actif
     * en manipulant simplement les paramètres de l'URL.
     */
    @GetMapping("/admin/membres/archives")
    @PreAuthorize("hasRole('ADMIN')")
    public String showArchivedMembersPage(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            Model model
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : ARCHIVED_MEMBERS_PAGE_SIZE;

        String resolvedSearch =
                search == null
                        ? ""
                        : search.trim();

        String resolvedSort =
                sort == null || sort.trim().isEmpty()
                        ? "roleThenLastName"
                        : sort.trim();

        /*
         * La page d'archives ne contient volontairement que les utilisateurs
         * dont activeUser = false.
         */
        Page<UserResponseDTO> usersPage = userService.getFilteredUsersPaged(
                resolvedSearch,
                role,
                "INACTIF",
                resolvedSort,
                safePage,
                safeSize
        );

        List<UserResponseDTO> users = usersPage.getContent();

        boolean hasSearch = !resolvedSearch.isEmpty();
        boolean paginationEnabled =
                usersPage.getTotalElements() > safeSize;

        model.addAttribute("users", users);
        model.addAttribute("search", resolvedSearch);
        model.addAttribute("role", role);
        model.addAttribute("sort", resolvedSort);

        model.addAttribute("pageTitle", "Utilisateurs archivés");
        model.addAttribute("activePage", "admin-membres");

        model.addAttribute("hasSearch", hasSearch);
        model.addAttribute(
                "resultsDisplayCount",
                usersPage.getTotalElements()
        );

        model.addAttribute(
                "currentPage",
                usersPage.getNumber()
        );

        model.addAttribute(
                "pageSize",
                usersPage.getSize()
        );

        model.addAttribute(
                "totalPages",
                usersPage.getTotalPages()
        );

        model.addAttribute(
                "totalElements",
                usersPage.getTotalElements()
        );

        model.addAttribute(
                "hasPrevious",
                usersPage.hasPrevious()
        );

        model.addAttribute(
                "hasNext",
                usersPage.hasNext()
        );

        model.addAttribute(
                "isFirst",
                usersPage.isFirst()
        );

        model.addAttribute(
                "isLast",
                usersPage.isLast()
        );

        model.addAttribute(
                "paginationEnabled",
                paginationEnabled
        );

        return "admin/membres-archives";
    }

    // -------------------------------------------------------------------------
    // ARCHIVAGE
    // -------------------------------------------------------------------------

    /**
     * Archive logiquement un utilisateur.
     *
     * L'adresse email de l'administrateur connecté est transmise au service
     * afin que la règle interdisant l'auto-archivage d'un ADMIN puisse être
     * appliquée côté métier.
     */
    @PostMapping("/admin/membres/{id}/archiver")
    @PreAuthorize("hasRole('ADMIN')")
    public String archiveMember(
            @PathVariable("id") Integer id,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            /*
             * Une route protégée par Spring Security doit toujours disposer
             * d'un Principal authentifié. Ce contrôle supplémentaire évite
             * néanmoins d'exécuter une opération sensible sans identité.
             */
            if (principal == null
                    || principal.getName() == null
                    || principal.getName().isBlank()) {

                throw new IllegalStateException(
                        "Impossible d’identifier l’administrateur connecté."
                );
            }

            userService.archiveUser(
                    id,
                    principal.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "L’utilisateur a été archivé avec succès."
            );

            return "redirect:/admin/membres";

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            /*
             * En cas de refus métier, retour sur la fiche de l'utilisateur
             * afin que l'administrateur comprenne immédiatement pourquoi
             * l'archivage n'a pas été réalisé.
             */
            return "redirect:/admin/membres/" + id;
        }
    }

    // -------------------------------------------------------------------------
    // RESTAURATION
    // -------------------------------------------------------------------------

    /**
     * Restaure un utilisateur précédemment archivé.
     *
     * La restauration conserve exactement la même ligne USER :
     * aucun compte n'est recréé et aucun historique n'est perdu.
     */
    @PostMapping("/admin/membres/{id}/restaurer")
    @PreAuthorize("hasRole('ADMIN')")
    public String restoreMember(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.restoreUser(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "L’utilisateur a été restauré avec succès."
            );

            return "redirect:/admin/membres/archives";

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/admin/membres/archives";
        }
    }
}