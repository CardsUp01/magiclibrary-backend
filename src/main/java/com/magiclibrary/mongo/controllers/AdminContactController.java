package com.magiclibrary.mongo.controllers;

import com.magiclibrary.mongo.dto.ContactReplyRequestDTO;
import com.magiclibrary.mongo.dto.ContactResponseDTO;
import com.magiclibrary.mongo.services.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST réservé à l'administration des messages de contact.
 *
 * Cette classe expose les endpoints permettant aux administrateurs
 * de consulter les messages stockés dans MongoDB et d'y répondre.
 */
@RestController
@RequestMapping("/contacts")
@Tag(
        name = "Admin Contacts",
        description = "Gestion administrative des messages de contact (MongoDB)."
)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContactController {

    private final ContactService contactService;

    public AdminContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /*
     * Retourne l'ensemble des messages de contact pour l'administration.
     */
    @Operation(
            summary = "Lister tous les messages de contact (ADMIN)",
            description = "Retourne l’ensemble des messages de contact enregistrés en base MongoDB."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des messages récupérée avec succès.",
                    content = @Content(schema = @Schema(implementation = ContactResponseDTO.class))
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
    @GetMapping
    public ResponseEntity<List<ContactResponseDTO>> getAllContacts() {
        List<ContactResponseDTO> messages = contactService.getAllContacts();
        return ResponseEntity.ok(messages);
    }

    /*
     * Retourne le détail d'un message de contact à partir de son identifiant MongoDB.
     */
    @Operation(
            summary = "Consulter un message de contact (ADMIN)",
            description = "Retourne le détail d’un message de contact à partir de son identifiant MongoDB."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Message trouvé.",
                    content = @Content(schema = @Schema(implementation = ContactResponseDTO.class))
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
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Message introuvable.",
                    content = @Content(
                            schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ContactResponseDTO> getContactById(
            @Parameter(
                    description = "Identifiant MongoDB du message de contact.",
                    example = "65f2c9a2e3b1a92d4c8f1234",
                    required = true
            )
            @PathVariable String id
    ) {
        ContactResponseDTO message = contactService.getContactById(id);
        return ResponseEntity.ok(message);
    }

    /*
     * Enregistre la réponse administrateur à un message de contact existant.
     */
    @Operation(
            summary = "Répondre à un message de contact (ADMIN)",
            description = "Permet à un Administrateur de répondre à un message de contact existant."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Réponse envoyée avec succès.",
                    content = @Content(schema = @Schema(implementation = ContactResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requête invalide : réponse vide ou non conforme.",
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
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Message introuvable.",
                    content = @Content(
                            schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class)
                    )
            )
    })
    @PutMapping("/{id}/reply")
    public ResponseEntity<ContactResponseDTO> replyToContact(
            @Parameter(
                    description = "Identifiant MongoDB du message de contact.",
                    example = "65f2c9a2e3b1a92d4c8f1234",
                    required = true
            )
            @PathVariable String id,
            @Valid @RequestBody ContactReplyRequestDTO requestDTO
    ) {
        ContactResponseDTO response = contactService.replyToContact(id, requestDTO);
        return ResponseEntity.ok(response);
    }
}