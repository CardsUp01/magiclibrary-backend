package com.magiclibrary.controllers;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.util.List;

// -----------------------------------------------------------------------------
// IMPORTS SPRING WEB
// -----------------------------------------------------------------------------
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.dto.item.ItemResponseDTO;
import com.magiclibrary.services.ItemService;

// -----------------------------------------------------------------------------
// IMPORTS SWAGGER / OPENAPI
// -----------------------------------------------------------------------------
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/* =============================================================================
   CONTROLLER : ItemController (Lecture seule - MVP)
   -----------------------------------------------------------------------------
   Gère les endpoints publics de consultation du catalogue numérique MagicLibrary.
   Conformité :
   - Lecture seule (aucun ajout / modification / suppression)
   - Accessible à tous : INVITÉ, MEMBRE, ADMIN
   - Réponses uniformisées via ItemResponseDTO
   =============================================================================
*/
@RestController
@RequestMapping("/items")
@Tag(
        name = "Catalogue (Items)",
        description = "Endpoints publics de consultation du catalogue numérique MagicLibrary (lecture seule, MVP)."
)
public class ItemController {

    // -------------------------------------------------------------------------
    // DÉPENDANCE SERVICE
    // -------------------------------------------------------------------------
    private final ItemService itemService;

    /**
     * Injection du service métier ItemService via le constructeur
     *
     * @param itemService service gérant la récupération des objets du catalogue
     */
    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    // =========================================================================
    // GET /items — LISTE COMPLÈTE (PUBLIC)
    // =========================================================================
    @Operation(
            summary = "Lister tous les objets du catalogue",
            description = "Retourne la liste complète des items du catalogue numérique. Endpoint public, lecture seule."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des items récupérée avec succès.",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = ItemResponseDTO.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "204",
                    description = "Aucun item disponible dans le catalogue.",
                    content = @Content
            )
    })
    @GetMapping
    public ResponseEntity<List<ItemResponseDTO>> getAllItems() {

        // Récupération de la liste complète depuis le service
        List<ItemResponseDTO> items = itemService.getAllItems();

        // Retour 204 No Content si aucune donnée
        if (items.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        // Retour 200 OK avec la liste des items
        return ResponseEntity.ok(items);
    }

    // =========================================================================
    // GET /items/{id} — DÉTAIL ITEM (PUBLIC)
    // =========================================================================
    @Operation(
            summary = "Récupérer un item par son identifiant",
            description = "Retourne le détail d’un item du catalogue numérique à partir de son identifiant unique."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Item trouvé.",
                    content = @Content(schema = @Schema(implementation = ItemResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Item introuvable.",
                    content = @Content(
                            schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponseDTO> getItemById(
            @Parameter(
                    description = "Identifiant unique de l’item (id_item).",
                    example = "1",
                    required = true
            )
            @PathVariable Integer id
    ) {
        // Délégation au service pour récupérer le détail d’un item
        ItemResponseDTO item = itemService.getItemById(id);

        // Retour 200 OK avec l’item
        return ResponseEntity.ok(item);
    }
}