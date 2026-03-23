package com.magiclibrary.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.magiclibrary.dto.user.UserCreateDTO;
import com.magiclibrary.services.UserService;

import jakarta.validation.Valid;

@Controller
public class AdminMemberCreatePageController {

    private final UserService userService;

    public AdminMemberCreatePageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/admin/membres/ajouter")
    @PreAuthorize("hasRole('ADMIN')")
    public String showMemberCreatePage(Model model) {
        if (!model.containsAttribute("userCreateDTO")) {
            model.addAttribute("userCreateDTO", new UserCreateDTO());
        }

        populatePageModel(model);
        return "admin/ajout-membre";
    }

    @PostMapping("/admin/membres/ajouter")
    @PreAuthorize("hasRole('ADMIN')")
    public String createMember(
            @Valid @ModelAttribute("userCreateDTO") UserCreateDTO userCreateDTO,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populatePageModel(model);
            model.addAttribute("errorMessage", "Merci de corriger les champs du formulaire.");
            return "admin/ajout-membre";
        }

        try {
            userService.createUser(userCreateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "L’utilisateur a été créé avec succès.");
            return "redirect:/admin/membres";
        } catch (IllegalStateException e) {
            populatePageModel(model);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/ajout-membre";
        }
    }

    private void populatePageModel(Model model) {
        model.addAttribute("pageTitle", "Ajouter un utilisateur");
    }
}