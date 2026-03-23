package com.magiclibrary.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ErrorPageController {

    @RequestMapping(value = "/error/400", method = {RequestMethod.GET, RequestMethod.POST})
    public String badRequest(HttpServletResponse response) {
        response.setStatus(400);
        return "error/400";
    }

    @RequestMapping(value = "/error/401", method = {RequestMethod.GET, RequestMethod.POST})
    public String unauthorized(HttpServletResponse response) {
        response.setStatus(401);
        return "error/401";
    }

    @RequestMapping(value = "/error/403", method = {RequestMethod.GET, RequestMethod.POST})
    public String forbidden(HttpServletResponse response) {
        response.setStatus(403);
        return "error/403";
    }

    @RequestMapping(value = "/error/404", method = {RequestMethod.GET, RequestMethod.POST})
    public String notFound(HttpServletResponse response) {
        response.setStatus(404);
        return "error/404";
    }

    @RequestMapping(value = "/error/405", method = {RequestMethod.GET, RequestMethod.POST})
    public String methodNotAllowed(HttpServletResponse response) {
        response.setStatus(405);
        return "error/405";
    }

    @RequestMapping(value = "/error/408", method = {RequestMethod.GET, RequestMethod.POST})
    public String requestTimeout(HttpServletResponse response) {
        response.setStatus(408);
        return "error/408";
    }

    @RequestMapping(value = "/error/409", method = {RequestMethod.GET, RequestMethod.POST})
    public String conflict(HttpServletResponse response) {
        response.setStatus(409);
        return "error/409";
    }

    @RequestMapping(value = "/error/422", method = {RequestMethod.GET, RequestMethod.POST})
    public String unprocessableEntity(HttpServletResponse response) {
        response.setStatus(422);
        return "error/422";
    }

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

    @RequestMapping(value = "/error/500", method = {RequestMethod.GET, RequestMethod.POST})
    public String internalServerError(HttpServletResponse response) {
        response.setStatus(500);
        return "error/500";
    }

    @RequestMapping(value = "/error/502", method = {RequestMethod.GET, RequestMethod.POST})
    public String badGateway(HttpServletResponse response) {
        response.setStatus(502);
        return "error/502";
    }

    @RequestMapping(value = "/error/503", method = {RequestMethod.GET, RequestMethod.POST})
    public String serviceUnavailable(HttpServletResponse response) {
        response.setStatus(503);
        return "error/503";
    }

    @RequestMapping(value = "/error/504", method = {RequestMethod.GET, RequestMethod.POST})
    public String gatewayTimeout(HttpServletResponse response) {
        response.setStatus(504);
        return "error/504";
    }
}