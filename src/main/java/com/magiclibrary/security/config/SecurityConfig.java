package com.magiclibrary.security.config;

import java.io.IOException;
import java.time.LocalDateTime;
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

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    private static final long LOGIN_ATTEMPTS_TTL_MILLIS = TimeUnit.MINUTES.toMillis(15);

    private static final long[] LOGIN_THROTTLE_DELAYS_MILLIS = new long[]{
            0L,
            0L,
            250L,
            500L,
            1000L,
            1500L,
            2000L
    };

    private static final int SSR_HARD_THROTTLE_MIN_ATTEMPTS = 6;
    private static final int THROTTLE_JITTER_MAX_MILLIS = 250;

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

    @Bean
    public FilterRegistrationBean<UrlNormalizationFilter> urlNormalizationFilterRegistration() {
        FilterRegistrationBean<UrlNormalizationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UrlNormalizationFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

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

    private int computeRetryAfterSeconds(long delayMillis) {
        long ms = Math.max(1000L, delayMillis);
        return (int) Math.min(60L, TimeUnit.MILLISECONDS.toSeconds(ms));
    }

    private long withJitter(long baseDelayMillis) {
        if (baseDelayMillis <= 0L) {
            return 0L;
        }
        int jitter = ThreadLocalRandom.current().nextInt(0, THROTTLE_JITTER_MAX_MILLIS + 1);
        return baseDelayMillis + jitter;
    }

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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:8080"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    private String buildLoginAttemptKey(String username, String ipAddress) {

        String safeUsername = (username == null) ? "" : username.trim().toLowerCase();
        String safeIpAddress = (ipAddress == null) ? "" : ipAddress.trim();

        return safeUsername + "|" + safeIpAddress;
    }

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

    private void cleanupExpiredLoginAttemptsIfNeeded(long now) {

        int count = loginAttemptsCleanupCounter.incrementAndGet();

        if (count % 50 != 0) {
            return;
        }

        loginAttemptsByKey.entrySet().removeIf(entry ->
                now - entry.getValue().lastAttemptEpochMillis > LOGIN_ATTEMPTS_TTL_MILLIS
        );
    }

    private static final class LoginAttemptEntry {

        private final int attempts;
        private final long lastAttemptEpochMillis;

        private LoginAttemptEntry(int attempts, long lastAttemptEpochMillis) {
            this.attempts = attempts;
            this.lastAttemptEpochMillis = lastAttemptEpochMillis;
        }
    }
}