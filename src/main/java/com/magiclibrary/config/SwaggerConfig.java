package com.magiclibrary.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * =============================================================================
 *  SWAGGER / OPENAPI CONFIGURATION — VERSION COMPATIBLE SPRING BOOT 3
 * =============================================================================
 *  - Documentation automatique de l’API MagicLibrary
 *  - Support complet du JWT Bearer dans Swagger UI
 *  - Aucune surcharge de ressources (OBLIGATOIRE avec Springdoc 2.x)
 *
 *  Swagger UI :
 *      http://localhost:8080/swagger-ui/index.html
 *
 *  OpenAPI JSON :
 *      http://localhost:8080/v3/api-docs
 *
 *  IMPORTANT :
 *      - NE PAS ajouter de ResourceHandlers.
 *      - NE PAS ajouter de beans inutiles.
 * =============================================================================
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "MagicLibrary API",
                version = "1.0",
                description = "Documentation officielle de l’API REST MagicLibrary (MVP)"
        ),
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {
    // Configuration minimaliste — tout est automatique avec Springdoc 2.x
}
