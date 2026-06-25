package com.magiclibrary.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrôleur SSR dédié à l'affichage des pages d'erreur de l'application.
 *
 * Cette classe centralise les principales erreurs HTTP gérées par
 * l'interface utilisateur et associe chaque code d'erreur à son
 * template Thymeleaf correspondant.
 */
@Controller
public class ErrorPageController {

    /*
     * Affiche la page correspondant à une requête invalide.
     */
    @RequestMapping(value = "/error/400", method = {RequestMethod.GET, RequestMethod.POST})
    public String badRequest(HttpServletResponse response) {
        response.setStatus(400);
        return "error/400";
    }

    /*
     * Affiche la page correspondant à un accès non authentifié.
     */
    @RequestMapping(value = "/error/401", method = {RequestMethod.GET, RequestMethod.POST})
    public String unauthorized(HttpServletResponse response) {
        response.setStatus(401);
        return "error/401";
    }

    /*
     * Affiche la page correspondant à un accès interdit.
     */
    @RequestMapping(value = "/error/403", method = {RequestMethod.GET, RequestMethod.POST})
    public String forbidden(HttpServletResponse response) {
        response.setStatus(403);
        return "error/403";
    }

    /*
     * Affiche la page correspondant à une ressource introuvable.
     */
    @RequestMapping(value = "/error/404", method = {RequestMethod.GET, RequestMethod.POST})
    public String notFound(HttpServletResponse response) {
        response.setStatus(404);
        return "error/404";
    }

    /*
     * Affiche la page correspondant à une méthode HTTP non autorisée.
     */
    @RequestMapping(value = "/error/405", method = {RequestMethod.GET, RequestMethod.POST})
    public String methodNotAllowed(HttpServletResponse response) {
        response.setStatus(405);
        return "error/405";
    }

    /*
     * Affiche la page correspondant à un délai d'attente dépassé.
     */
    @RequestMapping(value = "/error/408", method = {RequestMethod.GET, RequestMethod.POST})
    public String requestTimeout(HttpServletResponse response) {
        response.setStatus(408);
        return "error/408";
    }

    /*
     * Affiche la page correspondant à un conflit de traitement.
     */
    @RequestMapping(value = "/error/409", method = {RequestMethod.GET, RequestMethod.POST})
    public String conflict(HttpServletResponse response) {
        response.setStatus(409);
        return "error/409";
    }

    /*
     * Affiche la page correspondant à une requête valide
     * mais impossible à traiter.
     */
    @RequestMapping(value = "/error/422", method = {RequestMethod.GET, RequestMethod.POST})
    public String unprocessableEntity(HttpServletResponse response) {
        response.setStatus(422);
        return "error/422";
    }

    /*
     * Affiche la page correspondant à un nombre excessif de requêtes.
     *
     * Lorsque l'information est disponible, le délai d'attente avant
     * une nouvelle tentative est transmis à la vue et à l'en-tête HTTP.
     */
    @RequestMapping(value = "/error/429", method = {RequestMethod.GET, RequestMethod.POST})
    public String tooManyRequests(
            @RequestParam(value = "retryAfter", required = false) Integer retryAfterSeconds,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {

        response.setStatus(429);

        Integer fromAttr = null;
        Object attr = request.getAttribute("retryAfterSeconds");
        if (attr instanceof Integer) {
            fromAttr = (Integer) attr;
        }

        Integer safeRetryAfter = retryAfterSeconds;
        if (safeRetryAfter == null) {
            safeRetryAfter = fromAttr;
        }

        if (safeRetryAfter != null && safeRetryAfter > 0) {
            response.setHeader("Retry-After", String.valueOf(safeRetryAfter));
            model.addAttribute("retryAfterSeconds", safeRetryAfter);
        } else {
            model.addAttribute("retryAfterSeconds", null);
        }

        return "error/429";
    }

    /*
     * Affiche la page correspondant à une erreur interne du serveur.
     */
    @RequestMapping(value = "/error/500", method = {RequestMethod.GET, RequestMethod.POST})
    public String internalServerError(HttpServletResponse response) {
        response.setStatus(500);
        return "error/500";
    }

    /*
     * Affiche la page correspondant à une erreur de passerelle.
     */
    @RequestMapping(value = "/error/502", method = {RequestMethod.GET, RequestMethod.POST})
    public String badGateway(HttpServletResponse response) {
        response.setStatus(502);
        return "error/502";
    }

    /*
     * Affiche la page correspondant à une indisponibilité temporaire du service.
     */
    @RequestMapping(value = "/error/503", method = {RequestMethod.GET, RequestMethod.POST})
    public String serviceUnavailable(HttpServletResponse response) {
        response.setStatus(503);
        return "error/503";
    }

    /*
     * Affiche la page correspondant à un dépassement de délai côté passerelle.
     */
    @RequestMapping(value = "/error/504", method = {RequestMethod.GET, RequestMethod.POST})
    public String gatewayTimeout(HttpServletResponse response) {
        response.setStatus(504);
        return "error/504";
    }
}