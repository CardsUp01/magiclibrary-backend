package com.magiclibrary.mongo.controllers;

import com.magiclibrary.mongo.dto.ContactRequestDTO;
import com.magiclibrary.mongo.dto.ContactResponseDTO;
import com.magiclibrary.mongo.services.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contacts")
@Tag(
        name = "Contacts",
        description = "Gestion des messages de contact (MongoDB)."
)
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @Operation(
            summary = "Envoyer un message de contact (US-12 — PUBLIC)",
            description = "Permet à un utilisateur (authentifié ou non) d’envoyer un message de contact."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Message envoyé avec succès.",
                    content = @Content(schema = @Schema(implementation = ContactResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requête invalide : données manquantes ou non conformes.",
                    content = @Content(
                            schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class)
                    )
            )
    })
    @PostMapping
    public ResponseEntity<ContactResponseDTO> createContact(
            @Valid @RequestBody ContactRequestDTO requestDTO
    ) {
        ContactResponseDTO response = contactService.createContact(requestDTO);
        return ResponseEntity.ok(response);
    }
}