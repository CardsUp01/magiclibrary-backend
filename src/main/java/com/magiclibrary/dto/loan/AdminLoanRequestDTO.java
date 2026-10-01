package com.magiclibrary.dto.loan;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// -----------------------------------------------------------------------------
// IMPORTS SPRING
// -----------------------------------------------------------------------------
import org.springframework.format.annotation.DateTimeFormat;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION JAKARTA
// -----------------------------------------------------------------------------
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

// -----------------------------------------------------------------------------
// IMPORTS SWAGGER / OPENAPI
// -----------------------------------------------------------------------------
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * =============================================================================
 * DTO : ADMIN LOAN REQUEST
 * =============================================================================
 *
 * Représente les données saisies par un administrateur lors de la création
 * complète d'un emprunt depuis l'interface SSR d'administration.
 *
 * Contrairement au LoanRequestDTO historique du MVP, ce DTO ne représente pas
 * la création d'un emprunt vide.
 *
 * Il permet de transmettre en une seule requête :
 * - le membre concerné ;
 * - un ou plusieurs objets du catalogue ;
 * - la date réelle de début de l'emprunt ;
 * - une date d'échéance facultative ;
 * - une note facultative.
 *
 * Les données techniques et les statuts métier ne sont jamais fournis par
 * l'administrateur. Ils sont déterminés exclusivement par la couche Service.
 *
 * Exemples de valeurs calculées côté serveur :
 * - returnedLoan = false ;
 * - returnDateLoan = null ;
 * - extendedLoan = false ;
 * - extensionCountLoan = 0 ;
 * - statusLoan = ONGOING ou LATE ;
 * - overdueLoan = false ou true ;
 * - originLoan = ADMIN ;
 * - deletedDateLoan = null ;
 * - demoScenarioCode = null ;
 * - LoanLine.statusLoanLine = ACTIVE ;
 * - LoanLine.quantityLoanLine = 1.
 *
 * La cohérence entre startDateLoan et dueDateLoan est une règle métier
 * inter-champs contrôlée dans LoanServiceImpl lorsqu'une échéance est renseignée.
 *
 * Une échéance absente signifie qu'aucune date limite n'est actuellement connue
 * ou définie pour l'emprunt. Dans ce cas, le prêt peut rester ONGOING jusqu'à
 * sa restitution et ne doit pas être considéré comme en retard uniquement
 * parce qu'aucune échéance n'a été renseignée.
 *
 * Les annotations DateTimeFormat garantissent également la conversion
 * explicite des valeurs provenant des champs HTML :
 * - datetime-local -> LocalDateTime ;
 * - date -> LocalDate.
 * =============================================================================
 */
@Schema(
        description = "Données nécessaires à la création complète d'un emprunt par un administrateur."
)
public class AdminLoanRequestDTO {

    // -------------------------------------------------------------------------
    // MEMBRE CONCERNÉ
    // -------------------------------------------------------------------------

    /**
     * Identifiant du membre auquel l'emprunt doit être associé.
     *
     * La couche Service devra notamment vérifier :
     * - que l'utilisateur existe ;
     * - qu'il est actif ;
     * - qu'il possède un rôle autorisé à recevoir un emprunt.
     */
    @Schema(
            description = "Identifiant du membre auquel l'emprunt est attribué.",
            example = "12",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Le membre est obligatoire.")
    private Integer idUser;

    // -------------------------------------------------------------------------
    // OBJETS EMPRUNTÉS
    // -------------------------------------------------------------------------

    /**
     * Liste des identifiants des objets physiques empruntés.
     *
     * Chaque idItem représente un objet distinct du catalogue.
     *
     * La quantité n'est volontairement pas exposée :
     * une LoanLine sera créée par objet avec quantityLoanLine = 1.
     *
     * La couche Service devra également :
     * - refuser une liste vide ;
     * - refuser les doublons ;
     * - vérifier l'existence de chaque objet ;
     * - vérifier que chaque objet est actif ;
     * - vérifier que chaque objet est disponible ;
     * - vérifier qu'aucune LoanLine ACTIVE ne référence déjà l'objet.
     */
    @Schema(
            description = "Identifiants des objets empruntés.",
            example = "[45, 51]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotEmpty(message = "Au moins un objet doit être sélectionné.")
    @Size(min = 1, message = "Au moins un objet doit être sélectionné.")
    private List<
            @NotNull(message = "Un identifiant d'objet ne peut pas être vide.")
                    Integer
            > itemIds;

    // -------------------------------------------------------------------------
    // DATE RÉELLE DE DÉBUT
    // -------------------------------------------------------------------------

    /**
     * Date et heure réelles auxquelles l'emprunt a commencé.
     *
     * Ce champ est volontairement saisissable afin de permettre à un
     * administrateur d'enregistrer rétroactivement un prêt déjà commencé,
     * comme dans le cas d'un objet emprunté avant sa saisie dans MagicLibrary.
     *
     * Une date future est interdite.
     *
     * Le format ISO DATE_TIME correspond directement à la valeur transmise
     * par un champ HTML de type datetime-local, par exemple :
     *
     * 2026-09-30T18:00
     */
    @Schema(
            description = "Date et heure réelles de début de l'emprunt.",
            example = "2026-09-30T14:30:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "La date de début est obligatoire.")
    @PastOrPresent(message = "La date de début ne peut pas être future.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDateLoan;

    // -------------------------------------------------------------------------
    // DATE D'ÉCHÉANCE FACULTATIVE
    // -------------------------------------------------------------------------

    /**
     * Date prévue de restitution.
     *
     * Ce champ est facultatif.
     *
     * Une valeur null signifie qu'aucune échéance n'est actuellement connue
     * ou définie pour cet emprunt.
     *
     * Lorsqu'une échéance est renseignée, la validation de cohérence avec
     * startDateLoan est volontairement portée par le service métier, car elle
     * implique deux champs différents.
     *
     * Lorsqu'aucune échéance n'est renseignée :
     * - le prêt reste valide ;
     * - il ne doit pas être considéré automatiquement comme en retard ;
     * - il peut rester au statut ONGOING jusqu'à sa restitution.
     *
     * Le format ISO DATE correspond directement à la valeur transmise
     * par un champ HTML de type date, par exemple :
     *
     * 2026-10-30
     */
    @Schema(
            description = "Date d'échéance facultative de l'emprunt.",
            example = "2026-10-30",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateLoan;

    // -------------------------------------------------------------------------
    // NOTES FACULTATIVES
    // -------------------------------------------------------------------------

    /**
     * Notes internes facultatives associées à l'emprunt.
     *
     * Une chaîne vide pourra être normalisée en null par la couche Service.
     */
    @Schema(
            description = "Notes internes facultatives concernant l'emprunt.",
            example = "Objet déjà remis au membre avant son référencement numérique.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(
            max = 10_000,
            message = "Les notes ne peuvent pas dépasser 10 000 caractères."
    )
    private String notesLoan;

    // -------------------------------------------------------------------------
    // CONSTRUCTEUR SANS ARGUMENT
    // -------------------------------------------------------------------------

    /**
     * Constructeur requis notamment pour le binding Spring / Thymeleaf.
     */
    public AdminLoanRequestDTO() {
    }

    // -------------------------------------------------------------------------
    // GETTERS & SETTERS
    // -------------------------------------------------------------------------

    public Integer getIdUser() {
        return idUser;
    }

    public void setIdUser(Integer idUser) {
        this.idUser = idUser;
    }

    public List<Integer> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<Integer> itemIds) {
        this.itemIds = itemIds;
    }

    public LocalDateTime getStartDateLoan() {
        return startDateLoan;
    }

    public void setStartDateLoan(LocalDateTime startDateLoan) {
        this.startDateLoan = startDateLoan;
    }

    public LocalDate getDueDateLoan() {
        return dueDateLoan;
    }

    public void setDueDateLoan(LocalDate dueDateLoan) {
        this.dueDateLoan = dueDateLoan;
    }

    public String getNotesLoan() {
        return notesLoan;
    }

    public void setNotesLoan(String notesLoan) {
        this.notesLoan = notesLoan;
    }
}