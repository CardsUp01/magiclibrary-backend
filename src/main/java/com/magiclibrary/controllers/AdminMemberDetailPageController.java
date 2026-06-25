package com.magiclibrary.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.magiclibrary.dto.user.UserResponseDTO;
import com.magiclibrary.services.UserService;

/**
 * Contrôleur SSR réservé à la consultation détaillée d'un membre.
 *
 * Cette classe permet à un administrateur d'afficher la fiche
 * complète d'un utilisateur existant.
 */
@Controller
public class AdminMemberDetailPageController {

    private final UserService userService;

    public AdminMemberDetailPageController(UserService userService) {
        this.userService = userService;
    }

    /*
     * Affiche la fiche détaillée d'un membre.
     *
     * Si l'utilisateur demandé n'existe pas ou n'est plus disponible,
     * un message d'erreur est affiché puis l'administrateur est redirigé
     * vers la liste des membres.
     */
    @GetMapping("/admin/membres/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String showMemberDetailPage(
            @PathVariable("id") Integer id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            UserResponseDTO user = userService.getUserById(id);

            model.addAttribute("user", user);
            model.addAttribute("pageTitle", "Fiche membre");

            return "admin/fiche-membre";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/membres";
        }
    }
}