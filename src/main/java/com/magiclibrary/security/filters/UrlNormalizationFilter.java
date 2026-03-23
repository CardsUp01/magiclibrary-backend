package com.magiclibrary.security.filters;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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