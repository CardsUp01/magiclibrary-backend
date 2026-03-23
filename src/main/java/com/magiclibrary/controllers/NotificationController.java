package com.magiclibrary.controllers;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.dto.notification.NotificationRequestDTO;
import com.magiclibrary.dto.notification.NotificationResponseDTO;
import com.magiclibrary.entities.User;
import com.magiclibrary.exceptions.custom.UserNotFoundException;
import com.magiclibrary.repositories.interfaces.UserRepository;
import com.magiclibrary.services.NotificationService;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION
// -----------------------------------------------------------------------------
import jakarta.validation.Valid;

// -----------------------------------------------------------------------------
// IMPORTS SPRING WEB / SECURITY
// -----------------------------------------------------------------------------
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.util.List;

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
   CONTROLLER : NotificationController
   -----------------------------------------------------------------------------
   Gestion des notifications utilisateur pour le MVP :
       • Consultation de toutes les notifications pour l’utilisateur connecté
       • Marquer une notification comme lue
       • Création de notification par Administrateur

   Règles principales :
       - Sécurisé via JWT
       - Rôles : MEMBRE / ADMIN (lecture), ADMIN (création)
       - DTO strictement exposés en camelCase
       - Validation côté DTO (@Valid)
       - Documentation Swagger complète
   =============================================================================
*/
@RestController
@RequestMapping("/notifications")
@Tag(
        name = "Notifications",
        description = "Gestion des notifications utilisateur (consultation, lecture, création)."
)
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    // -------------------------------------------------------------------------
    // DÉPENDANCES SERVICE & REPOSITORY
    // -------------------------------------------------------------------------
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * Injection via constructeur
     *
     * @param notificationService service métier pour notifications
     * @param userRepository      repository utilisateur pour résolution userId
     */
    public NotificationController(
            NotificationService notificationService,
            UserRepository userRepository
    ) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    // =========================================================================
    // GET /notifications — LISTER LES NOTIFICATIONS (MEMBRE, ADMIN)
    // =========================================================================
    @Operation(
            summary = "Lister les notifications de l’utilisateur connecté",
            description = "Retourne toutes les notifications associées à l’utilisateur authentifié."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des notifications récupérée avec succès.",
                    content = @Content(
                            schema = @Schema(implementation = NotificationResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié : JWT absent ou invalide.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit : rôle Membre ou Administrateur requis.",
                    content = @Content
            )
    })
    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getNotifications(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Résolution de l'utilisateur connecté via son email dans JWT
        User user = userRepository.findByEmailUser(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));

        // Récupération de toutes les notifications pour cet utilisateur
        List<NotificationResponseDTO> notifications =
                notificationService.getNotificationsForUser(user.getIdUser());

        return ResponseEntity.ok(notifications);
    }

    // =========================================================================
    // PUT /notifications/{id}/read — MARQUER COMME LUE (MEMBRE, ADMIN)
    // =========================================================================
    @Operation(
            summary = "Marquer une notification comme lue",
            description = "Marque une notification spécifique comme lue pour l’utilisateur connecté."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification marquée comme lue.",
                    content = @Content(
                            schema = @Schema(implementation = NotificationResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié : JWT absent ou invalide.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit : rôle Membre ou Administrateur requis.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Notification introuvable.",
                    content = @Content(
                            schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class)
                    )
            )
    })
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(
            @PathVariable("id") Integer idNotification,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Résolution de l'utilisateur connecté
        User requester = userRepository.findByEmailUser(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));

        // Marquer la notification comme lue via le service
        NotificationResponseDTO dto =
                notificationService.markAsRead(idNotification, requester.getIdUser());

        return ResponseEntity.ok(dto);
    }

    // =========================================================================
    // POST /notifications — CRÉATION NOTIFICATION (ADMIN)
    // =========================================================================
    @Operation(
            summary = "Créer une notification (ADMIN)",
            description = "Crée une notification pour un utilisateur. Endpoint réservé au rôle Administrateur."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Notification créée avec succès.",
                    content = @Content(
                            schema = @Schema(implementation = NotificationResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requête invalide : données manquantes ou non conformes.",
                    content = @Content(
                            schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class)
                    )
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
    public ResponseEntity<NotificationResponseDTO> createNotification(
            @Valid @RequestBody NotificationRequestDTO requestDTO,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Résolution de l'utilisateur admin créateur
        User requester = userRepository.findByEmailUser(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));

        // Création de la notification via le service
        NotificationResponseDTO response =
                notificationService.createNotification(requestDTO, requester.getIdUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}