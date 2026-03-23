package com.magiclibrary.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.magiclibrary.dto.user.UserResponseDTO;
import com.magiclibrary.services.UserService;

@Controller
public class AdminMemberDetailPageController {

    private final UserService userService;

    public AdminMemberDetailPageController(UserService userService) {
        this.userService = userService;
    }

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