package com.magiclibrary.security.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.magiclibrary.security.auth.CustomUserDetailsService;
import com.magiclibrary.security.filters.UrlNormalizationFilter;
import com.magiclibrary.security.jwt.JwtAuthenticationFilter;

/**
 * Configuration centrale de la sécurité Spring Security.
 *
 * Cette classe sépare volontairement la sécurité REST basée sur JWT
 * et la sécurité SSR basée sur session afin de conserver deux comportements
 * adaptés aux usages de l'application.
 *
 * Elle configure également les règles CORS, les réponses d'erreur de sécurité,
 * la normalisation des URL et la limitation progressive des tentatives
 * de connexion SSR.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.cors.allowed-origins:http://localhost:8080,https://*.up.railway.app}")
    private String corsAllowedOrigins;

    /*
     * Durée de conservation des tentatives de connexion échouées.
     * Au-delà de cette durée, le compteur associé à une clé est réinitialisé.
     */
    private static final long LOGIN_ATTEMPTS_TTL_MILLIS = TimeUnit.MINUTES.toMillis(15);

    /*
     * Délais progressifs appliqués après les échecs de connexion SSR.
     * Le délai augmente avec le nombre d'échecs afin de ralentir
     * les tentatives répétées sans bloquer immédiatement l'utilisateur.
     */
    private static final long[] LOGIN_THROTTLE_DELAYS_MILLIS = new long[]{
            0L,
            0L,
            250L,
            500L,
            1000L,
            1500L,
            2000L
    };

    /*
     * Seuil de déclenchement du blocage temporaire HTTP 429
     * et amplitude maximale du délai aléatoire ajouté au throttling.
     */
    private static final int SSR_HARD_THROTTLE_MIN_ATTEMPTS = 6;
    private static final int THROTTLE_JITTER_MAX_MILLIS = 250;

    /*
     * Stockage mémoire des tentatives échouées.
     * La clé combine l'identifiant saisi et l'adresse IP afin de limiter
     * les attaques répétées sur un même compte ou depuis une même origine.
     */
    private final Map<String, LoginAttemptEntry> loginAttemptsByKey = new ConcurrentHashMap<>();
    private final AtomicInteger loginAttemptsCleanupCounter = new AtomicInteger(0);

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomUserDetailsService customUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customUserDetailsService = customUserDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    /*
     * Enregistre le filtre de normalisation des URL avant les filtres Spring.
     * Cela permet de traiter les chemins entrants de manière homogène
     * avant leur prise en charge par la chaîne de sécurité.
     */
    @Bean
    public FilterRegistrationBean<UrlNormalizationFilter> urlNormalizationFilterRegistration() {
        FilterRegistrationBean<UrlNormalizationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UrlNormalizationFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /*
     * Fournisseur d'authentification basé sur les utilisateurs applicatifs
     * et le PasswordEncoder configuré dans le projet.
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(daoAuthenticationProvider());
    }

    /*
     * Chaîne de sécurité dédiée aux endpoints REST.
     *
     * Elle fonctionne sans session serveur, désactive le formulaire Spring,
     * utilise le filtre JWT et retourne des réponses JSON pour les erreurs
     * d'authentification ou d'autorisation.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain restJwtSecurityFilterChain(HttpSecurity http) throws Exception {

        http
                .securityMatcher(
                        "/auth/**",
                        "/users/**",
                        "/items/**",
                        "/loans/**",
                        "/loan-lines/**",
                        "/notifications/**",
                        "/contacts/**"
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationManager(authenticationManager())
                .authenticationProvider(daoAuthenticationProvider())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::handleUnauthorized)
                        .accessDeniedHandler(this::handleForbidden)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/items", "/items/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/contacts").permitAll()
                        .requestMatchers(HttpMethod.GET, "/contacts", "/contacts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/contacts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users/me").hasAnyRole("MEMBRE", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/users/me").hasAnyRole("MEMBRE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/loans/me").hasAnyRole("MEMBRE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/loans/**").hasAnyRole("MEMBRE", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/loans").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/loans/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/loan-lines").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/notifications", "/notifications/**").hasAnyRole("MEMBRE", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/notifications/**").hasAnyRole("MEMBRE", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/notifications").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.TRACE, "/**").denyAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /*
     * Chaîne de sécurité dédiée aux pages SSR.
     *
     * Elle conserve le mécanisme de session Spring Security,
     * active le formulaire de connexion personnalisé et applique
     * une protection progressive contre les échecs répétés de connexion.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain ssrSecurityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> {})
                .authenticationProvider(daoAuthenticationProvider())
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(this::handleSsrForbidden)
                )
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/favicon.ico"
                        ).permitAll()

                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/error",
                                "/error/**"
                        ).permitAll()

                        .requestMatchers("/", "/login").permitAll()

                        .requestMatchers(
                                "/mentions-legales",
                                "/confidentialite",
                                "/accessibilite",
                                "/cgu"
                        ).permitAll()

                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {

                            String username = request.getParameter("username");
                            String ipAddress = resolveClientIpAddress(request);

                            String keyUserIp = buildLoginAttemptKey(username, ipAddress);
                            String keyIpOnly = buildLoginAttemptKey("*", ipAddress);

                            resetLoginAttempts(keyUserIp);
                            resetLoginAttempts(keyIpOnly);

                            response.sendRedirect(request.getContextPath() + "/accueil");
                        })
                        .failureHandler((request, response, exception) -> {

                            String username = request.getParameter("username");
                            String ipAddress = resolveClientIpAddress(request);

                            String keyUserIp = buildLoginAttemptKey(username, ipAddress);
                            String keyIpOnly = buildLoginAttemptKey("*", ipAddress);

                            int attemptsUserIp = registerFailedLoginAttempt(keyUserIp);
                            int attemptsIpOnly = registerFailedLoginAttempt(keyIpOnly);

                            int attempts = Math.max(attemptsUserIp, attemptsIpOnly);

                            long delayMillis = computeThrottleDelayMillis(attempts);

                            if (attempts >= SSR_HARD_THROTTLE_MIN_ATTEMPTS) {
                                int retryAfterSeconds = computeRetryAfterSeconds(delayMillis);
                                sendTooManyRequests429(request, response, retryAfterSeconds);
                                return;
                            }

                            applyThrottleDelay(withJitter(delayMillis));

                            response.sendRedirect(request.getContextPath() + "/login?error=true");
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessUrl("/login?logout=true")
                );

        return http.build();
    }

    /*
     * Retourne une réponse HTTP 429 pour les tentatives de connexion SSR
     * trop nombreuses et transmet le délai d'attente à la page d'erreur.
     */
    private void sendTooManyRequests429(HttpServletRequest request, HttpServletResponse response, int retryAfterSeconds)
            throws IOException, ServletException {

        int safeRetryAfter = Math.max(1, retryAfterSeconds);

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(safeRetryAfter));

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");

        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 429);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
        request.setAttribute("retryAfterSeconds", safeRetryAfter);

        RequestDispatcher dispatcher = request.getRequestDispatcher("/error/429");
        dispatcher.forward(request, response);
    }

    /*
     * Gère les refus d'accès côté SSR en redirigeant vers la page 403
     * sans exposer de détail technique à l'utilisateur.
     */
    private void handleSsrForbidden(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.access.AccessDeniedException ex
    ) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpServletResponse.SC_FORBIDDEN);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());

        RequestDispatcher dispatcher = request.getRequestDispatcher("/error/403");
        dispatcher.forward(request, response);
    }

    /*
     * Convertit le délai de throttling en valeur Retry-After.
     * La valeur retournée reste bornée pour éviter un délai excessif côté client.
     */
    private int computeRetryAfterSeconds(long delayMillis) {
        long ms = Math.max(1000L, delayMillis);
        return (int) Math.min(60L, TimeUnit.MILLISECONDS.toSeconds(ms));
    }

    /*
     * Ajoute une variation aléatoire au délai de throttling.
     * Cette variation rend les tentatives automatisées moins prévisibles.
     */
    private long withJitter(long baseDelayMillis) {
        if (baseDelayMillis <= 0L) {
            return 0L;
        }
        int jitter = ThreadLocalRandom.current().nextInt(0, THROTTLE_JITTER_MAX_MILLIS + 1);
        return baseDelayMillis + jitter;
    }

    /*
     * Gère les appels REST non authentifiés avec une réponse JSON 401.
     */
    private void handleUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.AuthenticationException ex
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        String body = "{"
                + "\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":401,"
                + "\"error\":\"Unauthorized\","
                + "\"message\":\"Accès non authentifié.\","
                + "\"path\":\"" + request.getRequestURI() + "\""
                + "}";

        response.getWriter().write(body);
    }

    /*
     * Gère les appels REST authentifiés mais non autorisés avec une réponse JSON 403.
     */
    private void handleForbidden(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.access.AccessDeniedException ex
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        String body = "{"
                + "\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":403,"
                + "\"error\":\"Forbidden\","
                + "\"message\":\"Accès interdit.\","
                + "\"path\":\"" + request.getRequestURI() + "\""
                + "}";

        response.getWriter().write(body);
    }

    /*
     * Configure les règles CORS appliquées aux endpoints REST.
     * Cette configuration limite explicitement les origines, méthodes
     * et en-têtes acceptés par l'application.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(resolveAllowedOriginPatterns());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /*
     * Résout les origines CORS autorisées à partir de la configuration.
     * La valeur par défaut conserve le fonctionnement local et autorise
     * les domaines Railway utilisés pour le déploiement de production.
     */
    private List<String> resolveAllowedOriginPatterns() {

        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        if (origins.isEmpty()) {
            return List.of("http://localhost:8080", "https://*.up.railway.app");
        }

        return origins;
    }

    /*
     * Construit une clé de suivi des tentatives de connexion.
     * L'association utilisateur + IP permet de limiter les attaques ciblées
     * tout en conservant un suivi global par adresse IP.
     */
    private String buildLoginAttemptKey(String username, String ipAddress) {

        String safeUsername = (username == null) ? "" : username.trim().toLowerCase();
        String safeIpAddress = (ipAddress == null) ? "" : ipAddress.trim();

        return safeUsername + "|" + safeIpAddress;
    }

    /*
     * Résout l'adresse IP du client.
     * Les en-têtes de proxy sont pris en compte avant l'adresse distante
     * fournie directement par la requête HTTP.
     */
    private String resolveClientIpAddress(HttpServletRequest request) {

        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            if (!first.isBlank()) {
                return first;
            }
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }

    /*
     * Enregistre une tentative de connexion échouée.
     * Le compteur est réinitialisé automatiquement lorsque la dernière
     * tentative est plus ancienne que la durée de conservation prévue.
     */
    private int registerFailedLoginAttempt(String key) {

        long now = System.currentTimeMillis();

        LoginAttemptEntry entry = loginAttemptsByKey.compute(key, (k, existing) -> {

            if (existing == null) {
                return new LoginAttemptEntry(1, now);
            }

            if (now - existing.lastAttemptEpochMillis > LOGIN_ATTEMPTS_TTL_MILLIS) {
                return new LoginAttemptEntry(1, now);
            }

            return new LoginAttemptEntry(existing.attempts + 1, now);
        });

        cleanupExpiredLoginAttemptsIfNeeded(now);

        return entry.attempts;
    }

    private void resetLoginAttempts(String key) {
        loginAttemptsByKey.remove(key);
    }

    /*
     * Détermine le délai à appliquer selon le nombre d'échecs de connexion.
     * Lorsque le nombre d'échecs dépasse le tableau configuré,
     * le délai maximal défini est réutilisé.
     */
    private long computeThrottleDelayMillis(int attempts) {

        int index = attempts - 1;

        if (index < 0) {
            return 0L;
        }

        if (index >= LOGIN_THROTTLE_DELAYS_MILLIS.length) {
            return LOGIN_THROTTLE_DELAYS_MILLIS[LOGIN_THROTTLE_DELAYS_MILLIS.length - 1];
        }

        return LOGIN_THROTTLE_DELAYS_MILLIS[index];
    }

    /*
     * Applique le délai de ralentissement sur le traitement de la requête.
     * En cas d'interruption, le statut du thread est restauré.
     */
    private void applyThrottleDelay(long delayMillis) {

        if (delayMillis <= 0L) {
            return;
        }

        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /*
     * Nettoie périodiquement les anciennes tentatives de connexion.
     * Le nettoyage n'est pas lancé à chaque échec afin d'éviter un coût
     * inutile sur les tentatives successives.
     */
    private void cleanupExpiredLoginAttemptsIfNeeded(long now) {

        int count = loginAttemptsCleanupCounter.incrementAndGet();

        if (count % 50 != 0) {
            return;
        }

        loginAttemptsByKey.entrySet().removeIf(entry ->
                now - entry.getValue().lastAttemptEpochMillis > LOGIN_ATTEMPTS_TTL_MILLIS
        );
    }

    /*
     * Représente l'état minimal conservé pour une clé de tentative de connexion.
     */
    private static final class LoginAttemptEntry {

        private final int attempts;
        private final long lastAttemptEpochMillis;

        private LoginAttemptEntry(int attempts, long lastAttemptEpochMillis) {
            this.attempts = attempts;
            this.lastAttemptEpochMillis = lastAttemptEpochMillis;
        }
    }
}