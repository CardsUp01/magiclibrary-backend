package com.magiclibrary.controllers;

// -----------------------------------------------------------------------------
// IMPORTS SPRING / SÉCURITÉ
// -----------------------------------------------------------------------------
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION
// -----------------------------------------------------------------------------
import jakarta.validation.Valid;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.services.UserService;
import com.magiclibrary.dto.user.UserCreateDTO;
import com.magiclibrary.dto.user.UserUpdateDTO;
import com.magiclibrary.dto.user.UserResponseDTO;
import com.magiclibrary.exceptions.custom.UnauthorizedException;

// -----------------------------------------------------------------------------
// IMPORTS SWAGGER / OPENAPI
// -----------------------------------------------------------------------------
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/* =============================================================================
   CONTROLLER : UserController
   -----------------------------------------------------------------------------
   Endpoints gérés :
       • POST /users       → création d’un utilisateur par ADMIN
       • GET  /users/me    → consultation du profil connecté (ADMIN, MEMBRE)
       • PUT  /users/me    → mise à jour du profil connecté (ADMIN, MEMBRE)

   Règles principales :
       - sécurisation via JWT + rôles
       - exposition API strictement en camelCase
       - validation @Valid côté DTO
       - documentation Swagger/OpenAPI complète
   =============================================================================
*/
@RestController
@RequestMapping("/users")
@Tag(
        name = "Utilisateurs",
        description = "Gestion des utilisateurs du MVP : création par Administrateur, consultation et mise à jour du profil connecté."
)
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    // -------------------------------------------------------------------------
    // DEPENDANCE SERVICE
    // -------------------------------------------------------------------------
    private final UserService userService;

    /**
     * Injection constructeur (final service)
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // =========================================================================
    // POST /users — Création utilisateur (ADMIN)
    // =========================================================================
    @Operation(
            summary = "Créer un nouvel utilisateur (ADMIN)",
            description = "Crée un utilisateur dans l’application. Endpoint réservé au rôle Administrateur."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Utilisateur créé avec succès.",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requête invalide : données manquantes ou non conformes.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié : JWT absent ou invalide.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit : rôle Administrateur requis.",
                    content = @Content
            )
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        // Appel au service pour création
        UserResponseDTO response = userService.createUser(dto);
        // Retour 201 + DTO
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // GET /users/me — Profil utilisateur connecté
    // =========================================================================
    @Operation(
            summary = "Consulter le profil de l’utilisateur connecté (ADMIN, MEMBRE)",
            description = "Retourne le profil de l’utilisateur authentifié (extrait depuis le JWT)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profil récupéré avec succès.",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié : JWT absent ou invalide.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit : rôle Administrateur ou Membre requis.",
                    content = @Content
            )
    })
    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MEMBRE')")
    public ResponseEntity<UserResponseDTO> getAuthenticatedUser(
            @Parameter(hidden = true) Authentication authentication
    ) {
        // Extraction userId depuis JWT / Authentication
        Integer userId = extractUserId(authentication);
        // Appel service pour récupération profil
        UserResponseDTO response = userService.getAuthenticatedUser(userId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // PUT /users/me — Mise à jour profil utilisateur connecté
    // =========================================================================
    @Operation(
            summary = "Mettre à jour le profil de l’utilisateur connecté (ADMIN, MEMBRE)",
            description = "Met à jour les informations du profil de l’utilisateur authentifié (extrait depuis le JWT)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profil mis à jour avec succès.",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requête invalide : données manquantes ou non conformes.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié : JWT absent ou invalide.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit : rôle Administrateur ou Membre requis.",
                    content = @Content
            )
    })
    @PutMapping("/me")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MEMBRE')")
    public ResponseEntity<UserResponseDTO> updateAuthenticatedUser(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody UserUpdateDTO dto
    ) {
        // Extraction userId depuis JWT
        Integer userId = extractUserId(authentication);
        // Appel service pour mise à jour du profil
        UserResponseDTO response = userService.updateAuthenticatedUser(userId, dto);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // MÉTHODE INTERNE : extraction robuste userId depuis Authentication
    // =========================================================================
    /**
     * Extrait l’identifiant utilisateur depuis l’objet Authentication.
     * Lance UnauthorizedException si JWT absent, invalide ou authentification non valide.
     *
     * @param authentication objet Spring Security Authentication
     * @return userId associé au JWT
     */
    private Integer extractUserId(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("JWT invalide ou absent.");
        }

        Object details = authentication.getDetails();

        if (!(details instanceof Integer)) {
            // Cas typiques :
            // - details = WebAuthenticationDetails (JWT non injecté)
            // - details = null (token absent)
            throw new UnauthorizedException("JWT invalide ou absent.");
        }

        return (Integer) details;
    }
}