package com.magiclibrary.controllers;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.magiclibrary.dto.user.UserResponseDTO;
import com.magiclibrary.services.UserService;

/**
 * Contrôleur SSR réservé à l'administration des membres.
 *
 * Cette classe gère l'affichage paginé des utilisateurs actifs,
 * les filtres de recherche et de rôle, le tri ainsi que l'autocomplétion
 * utilisée sur la page d'administration des membres.
 *
 * Les utilisateurs archivés sont volontairement exclus de cette page.
 * Ils seront consultables depuis l'espace dédié aux utilisateurs archivés.
 */
@Controller
public class AdminMembersPageController {

    /*
     * Taille par défaut utilisée pour la pagination de la page SSR
     * d'administration des membres.
     */
    private static final int MEMBERS_PAGE_SIZE = 9;

    private final UserService userService;

    public AdminMembersPageController(UserService userService) {
        this.userService = userService;
    }

    /*
     * Affiche la page d'administration des membres actifs.
     *
     * La méthode prépare les filtres, le tri, la pagination et les indicateurs
     * nécessaires à l'affichage de la liste dans le template Thymeleaf.
     *
     * Important :
     *      la liste principale est désormais strictement limitée aux comptes
     *      actifs. Un paramètre manuel dans l'URL ne peut donc pas permettre
     *      de réafficher ici un utilisateur archivé.
     */
    @GetMapping("/admin/membres")
    @PreAuthorize("hasRole('ADMIN')")
    public String showMembersPage(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            Model model
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : MEMBERS_PAGE_SIZE;

        String resolvedSort =
                sort == null || sort.trim().isEmpty()
                        ? "roleThenLastName"
                        : sort.trim();

        String resolvedSearch =
                search == null
                        ? ""
                        : search.trim();

        /*
         * La page /admin/membres représente maintenant exclusivement
         * les utilisateurs actifs.
         *
         * Le statut ACTIF est donc imposé côté serveur et ne dépend plus
         * d'un paramètre fourni par le navigateur.
         */
        Page<UserResponseDTO> usersPage = userService.getFilteredUsersPaged(
                resolvedSearch,
                role,
                "ACTIF",
                resolvedSort,
                safePage,
                safeSize
        );

        List<UserResponseDTO> users = usersPage.getContent();

        boolean hasSearch = !resolvedSearch.isEmpty();
        boolean paginationEnabled = usersPage.getTotalElements() > safeSize;
        long resultsDisplayCount = usersPage.getTotalElements();

        model.addAttribute("users", users);
        model.addAttribute("search", resolvedSearch);
        model.addAttribute("role", role);

        /*
         * Conservé temporairement pour assurer la compatibilité avec
         * le template actuel jusqu'à la suppression du filtre Actif/Inactif
         * dans admin/membres.html.
         */
        model.addAttribute("status", "ACTIF");

        model.addAttribute("sort", resolvedSort);
        model.addAttribute("pageTitle", "Membres");
        model.addAttribute("activePage", "admin-membres");

        model.addAttribute("hasSearch", hasSearch);
        model.addAttribute("resultsDisplayCount", resultsDisplayCount);

        model.addAttribute("currentPage", usersPage.getNumber());
        model.addAttribute("pageSize", usersPage.getSize());
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("totalElements", usersPage.getTotalElements());
        model.addAttribute("hasPrevious", usersPage.hasPrevious());
        model.addAttribute("hasNext", usersPage.hasNext());
        model.addAttribute("isFirst", usersPage.isFirst());
        model.addAttribute("isLast", usersPage.isLast());
        model.addAttribute("paginationEnabled", paginationEnabled);

        return "admin/membres";
    }

    /*
     * Fournit les suggestions d'utilisateurs actifs pour l'autocomplétion
     * de la page d'administration.
     *
     * Les comptes archivés sont volontairement exclus afin qu'un utilisateur
     * absent de la liste principale ne puisse pas réapparaître indirectement
     * dans les suggestions de recherche.
     */
    @GetMapping(
            value = "/admin/membres/suggest",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MemberSuggestResponse>> suggestMembers(
            @RequestParam(name = "q", required = false) String q
    ) {
        List<MemberSuggestResponse> suggestions = userService.suggestUsers(q)
                .stream()
                .filter(user -> Boolean.TRUE.equals(user.getActiveUser()))
                .map(this::toSuggestResponse)
                .toList();

        return ResponseEntity.ok(suggestions);
    }

    private MemberSuggestResponse toSuggestResponse(UserResponseDTO user) {
        return new MemberSuggestResponse(
                user.getIdUser(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRoleLabel(),
                Boolean.TRUE.equals(user.getActiveUser()) ? "Actif" : "Inactif"
        );
    }

    /*
     * DTO interne utilisé uniquement pour exposer les suggestions
     * de membres au format JSON.
     */
    public static final class MemberSuggestResponse {

        private Integer id;
        private String firstName;
        private String lastName;
        private String email;
        private String role;
        private String status;

        public MemberSuggestResponse() {
        }

        public MemberSuggestResponse(
                Integer id,
                String firstName,
                String lastName,
                String email,
                String role,
                String status
        ) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.role = role;
            this.status = status;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}