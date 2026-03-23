package com.magiclibrary.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.magiclibrary.dto.user.UserResponseDTO;
import com.magiclibrary.dto.user.UserUpdateDTO;
import com.magiclibrary.services.UserService;

import jakarta.validation.Valid;

@Controller
public class AdminMemberEditPageController {

    private final UserService userService;

    public AdminMemberEditPageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/admin/membres/{id}/modifier")
    @PreAuthorize("hasRole('ADMIN')")
    public String showMemberEditPage(
            @PathVariable("id") Integer id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            UserResponseDTO user = userService.getUserById(id);

            if (!model.containsAttribute("userUpdateDTO")) {
                UserUpdateDTO userUpdateDTO = new UserUpdateDTO();
                userUpdateDTO.setCivility(user.getCivility());
                userUpdateDTO.setFirstName(user.getFirstName());
                userUpdateDTO.setLastName(user.getLastName());
                model.addAttribute("userUpdateDTO", userUpdateDTO);
            }

            model.addAttribute("user", user);
            model.addAttribute("pageTitle", "Modifier un utilisateur");

            return "admin/modifier-membre";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/membres";
        }
    }

    @PostMapping("/admin/membres/{id}/modifier")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateMember(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("userUpdateDTO") UserUpdateDTO userUpdateDTO,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        UserResponseDTO user = null;

        try {
            user = userService.getUserById(id);

            if (bindingResult.hasErrors()) {
                model.addAttribute("user", user);
                model.addAttribute("pageTitle", "Modifier un utilisateur");
                model.addAttribute("errorMessage", "Merci de corriger les champs du formulaire.");
                return "admin/modifier-membre";
            }

            userService.updateUserByAdmin(id, userUpdateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "L’utilisateur a été modifié avec succès.");
            return "redirect:/admin/membres/" + id;
        } catch (IllegalStateException e) {
            if (user != null) {
                model.addAttribute("user", user);
                model.addAttribute("pageTitle", "Modifier un utilisateur");
                model.addAttribute("errorMessage", e.getMessage());
                return "admin/modifier-membre";
            }

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/membres";
        }
    }
}