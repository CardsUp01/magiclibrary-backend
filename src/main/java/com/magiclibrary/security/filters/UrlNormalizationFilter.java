package com.magiclibrary.security.filters;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtre chargé de normaliser les URL contenant des séparateurs
 * de chemin dupliqués avant leur traitement par l'application.
 *
 * Lorsqu'une URL contient plusieurs caractères '/' consécutifs,
 * une redirection est effectuée vers la version normalisée afin
 * de garantir une URL unique et cohérente.
 */
public class UrlNormalizationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI();
        if (uri == null || uri.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        if (!uri.contains("//")) {
            chain.doFilter(request, response);
            return;
        }

        String normalized = normalizePath(uri);

        if (normalized.equals(uri)) {
            chain.doFilter(request, response);
            return;
        }

        String query = req.getQueryString();
        String target = (query == null || query.isBlank()) ? normalized : normalized + "?" + query;

        res.setStatus(HttpServletResponse.SC_FOUND);
        res.setHeader("Location", target);
    }

    /*
     * Supprime les séparateurs de chemin dupliqués
     * et garantit la présence du caractère '/' initial.
     */
    private static String normalizePath(String path) {
        String out = path;
        while (out.contains("//")) {
            out = out.replace("//", "/");
        }
        if (out.isEmpty()) {
            return "/";
        }
        if (out.charAt(0) != '/') {
            out = "/" + out;
        }
        return out;
    }
}