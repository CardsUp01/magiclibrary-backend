package com.magiclibrary.controllers;

// -----------------------------------------------------------------------------
// IMPORTS SPRING
// -----------------------------------------------------------------------------
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION
// -----------------------------------------------------------------------------
import jakarta.validation.Valid;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.dto.auth.LoginRequestDTO;
import com.magiclibrary.dto.auth.LoginResponseDTO;
import com.magiclibrary.dto.auth.RegisterRequestDTO;
import com.magiclibrary.services.auth.AuthService;
import com.magiclibrary.exceptions.model.ApiErrorResponse;

// -----------------------------------------------------------------------------
// IMPORTS SWAGGER / OPENAPI
// -----------------------------------------------------------------------------
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/* =============================================================================
   CONTROLLER : AuthController
   -----------------------------------------------------------------------------
   Gère l’authentification et l’inscription utilisateur.
   Endpoints MVP :
       - POST /auth/login    → connexion (US-01)
       - POST /auth/register → inscription publique (US-02)
   Conformité :
       - Validation DTO via Bean Validation
       - Réponses uniformisées avec LoginResponseDTO / ApiErrorResponse
       - Endpoint register accessible sans JWT
   =============================================================================
*/
@RestController
@RequestMapping("/auth")
@Validated
@Tag(
        name = "Authentification",
        description = "Endpoints liés à l’authentification et à la création d’un compte utilisateur."
)
public class AuthController {

    // -------------------------------------------------------------------------
    // DÉPENDANCE SERVICE
    // -------------------------------------------------------------------------
    private final AuthService authService;

    /**
     * Injection du service AuthService via constructeur.
     *
     * @param authService service métier pour login et register
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // =========================================================================
    // POST /auth/login — AUTHENTIFICATION (US-01)
    // =========================================================================
    @Operation(
            summary = "Connexion utilisateur (US-01)",
            description = "Authentifie un utilisateur via email et mot de passe, puis retourne un JWT Bearer Token."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Authentification réussie : JWT retourné.",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requête invalide : données manquantes ou non conformes.",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentification échouée : identifiants invalides.",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO requestDTO
    ) {
        // Délégation complète au service métier pour authentification
        LoginResponseDTO response = authService.login(requestDTO);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // POST /auth/register — INSCRIPTION PUBLIQUE (US-02)
    // =========================================================================
    @Operation(
            summary = "Inscription utilisateur (US-02 — PUBLIC)",
            description = "Crée un compte avec le rôle Membre. Endpoint public : aucune authentification requise."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Compte créé avec succès."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requête invalide : données manquantes ou non conformes.",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflit : un compte existe déjà avec cet email.",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequestDTO requestDTO
    ) {
        // Délégation complète au service pour création utilisateur
        authService.register(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}