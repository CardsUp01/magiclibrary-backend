package com.magiclibrary.security.jwt;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.io.IOException;

// -----------------------------------------------------------------------------
// IMPORTS SERVLET / SPRING SECURITY
// -----------------------------------------------------------------------------
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.security.auth.CustomUserDetailsService;
import com.magiclibrary.exceptions.custom.UnauthorizedException;

/**
 * =============================================================================
 *  FILTER : JWT AUTHENTICATION
 * =============================================================================
 *  Rôle :
 *      - lire le header Authorization: Bearer <token>
 *      - valider techniquement le JWT (signature / expiration)
 *      - charger l'utilisateur (UserDetailsService)
 *      - poser l'Authentication dans le SecurityContext
 *
 *  RÈGLE CRITIQUE (stabilité) :
 *      - ce filtre NE DOIT JAMAIS lever une exception qui pourrait générer un 500
 *        avant le contrôleur (RestControllerAdvice ne gère pas les exceptions
 *        levées en amont du DispatcherServlet)
 *
 *  Donc :
 *      - JWT absent → on laisse passer (endpoints publics)
 *      - JWT invalide / claims manquants / user introuvable → on NE CONNECTE PAS
 *        et on laisse Spring Security produire le 401 sur les endpoints protégés
 * =============================================================================
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            CustomUserDetailsService userDetailsService
    ) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Pas de JWT → on laisse passer (endpoints publics possibles)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Auth déjà établie → on ne ré-authentifie pas
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // JWT invalide (signature / expiration / format) → on n'authentifie pas
            if (!jwtUtil.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtUtil.extractEmail(token);
            Integer userId = jwtUtil.extractUserId(token);

            // Claims attendus absents → JWT inutilisable
            if (email == null || userId == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Charge l'utilisateur (peut refuser si compte inactif / rôle invalide)
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // Détail optionnel : ID utilisateur accessible si besoin côté controllers
            authentication.setDetails(userId);

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (UsernameNotFoundException | UnauthorizedException ex) {
            // Utilisateur introuvable / compte refusé → on n'authentifie pas
            // (Spring Security renverra 401 sur les endpoints protégés)
        } catch (Exception ex) {
            // Sécurité : jamais de 500 depuis le filtre
        }

        filterChain.doFilter(request, response);
    }
}