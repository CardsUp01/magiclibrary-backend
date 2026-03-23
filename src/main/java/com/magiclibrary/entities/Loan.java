package com.magiclibrary.entities;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

// -----------------------------------------------------------------------------
// IMPORTS JPA (explicites, jamais de wildcard)
// -----------------------------------------------------------------------------
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION (Bean Validation Jakarta)
// -----------------------------------------------------------------------------
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.enums.LoanStatus;

/**
 * =============================================================================
 *  ENTITY : LOAN
 * =============================================================================
 *  Représente un emprunt réalisé dans l’application MagicLibrary.
 *
 *  Cette entité matérialise le lien fonctionnel entre :
 *      - un utilisateur (obligatoire) ;
 *      - une ou plusieurs lignes d'emprunt (LOAN_LINE) ;
 *      - les règles métier associées au suivi d'un emprunt.
 *
 *  Conformité stricte :
 *      - Dictionnaire de données LOAN (MVP final) ;
 *      - MCD / MLD / MPD validés ;
 *      - Contraintes JPA & Bean Validation ;
 *      - Règles de cohérence MagicLibrary (CDA 30/20).
 *
 *  Aucun attribut n’est inventé. Aucune logique métier n’est présente.
 * =============================================================================
 */
@Entity
@Table(name = "loan")
public class Loan {

    // -------------------------------------------------------------------------
    // IDENTIFIANT TECHNIQUE (PRIMARY KEY)
    // -------------------------------------------------------------------------

    /**
     * Identifiant unique de l’emprunt.
     * Clé primaire : id_loan (INT AUTO_INCREMENT).
     * Toujours obligatoire.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_loan", nullable = false)
    private Integer idLoan;

    // -------------------------------------------------------------------------
    // RELATION OBLIGATOIRE AVEC USER (FK id_user)
    // -------------------------------------------------------------------------

    /**
     * Utilisateur à qui appartient cet emprunt.
     * Relation Many-To-One obligatoire.
     * Clé étrangère : id_user → USER(id_user).
     *
     * NOTE IMPORTANTE :
     *      - Loan et User se trouvent dans le même package (com.magiclibrary.entities) ;
     *      - dans ce cas, aucun import explicite n’est nécessaire pour le type User ;
     *      - l’absence d’import ou sa présence n’a aucun impact sur Hibernate
     *        ni sur les erreurs 500 : c’est purement une question de style.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    @NotNull(message = "L'utilisateur associé à l'emprunt est obligatoire.")
    private User user;

    // -------------------------------------------------------------------------
    // DATES DE L’EMPRUNT
    // -------------------------------------------------------------------------

    /**
     * Date et heure de début de l’emprunt.
     * Générée par l'application lors de la création.
     * Type SQL : DATETIME NOT NULL.
     */
    @Column(name = "start_date_loan", nullable = false)
    @NotNull(message = "La date de début est obligatoire.")
    @PastOrPresent(message = "La date de début ne peut pas être future.")
    private LocalDateTime startDateLoan;

    /**
     * Date d’échéance théorique de l’emprunt.
     * Type SQL : DATE NOT NULL.
     */
    @Column(name = "due_date_loan", nullable = false)
    @NotNull(message = "La date d'échéance est obligatoire.")
    private LocalDate dueDateLoan;

    /**
     * Date et heure réelle de restitution.
     * Champ optionnel.
     * Type SQL : DATETIME NULL.
     */
    @Column(name = "return_date_loan")
    @PastOrPresent(message = "La date de retour ne peut pas être future.")
    private LocalDateTime returnDateLoan;

    // -------------------------------------------------------------------------
    // ÉTATS BOOLÉENS LIÉS AU SUIVI DE L’EMPRUNT
    // -------------------------------------------------------------------------

    /**
     * Indique si l’emprunt est restitué.
     * Valeur par défaut : 0 (non restitué).
     * Type SQL : BOOLEAN NOT NULL.
     */
    @Column(name = "returned_loan", nullable = false)
    @NotNull(message = "Le statut 'restitué' est obligatoire.")
    private Boolean returnedLoan;

    /**
     * Indique si l’emprunt est en retard.
     * Calculé par la logique applicative.
     * Type SQL : BOOLEAN NOT NULL.
     */
    @Column(name = "overdue_loan", nullable = false)
    @NotNull(message = "Le statut 'en retard' est obligatoire.")
    private Boolean overdueLoan;

    /**
     * Indique si l’emprunt a été prolongé.
     * Valeur par défaut : 0.
     * Type SQL : BOOLEAN NOT NULL.
     */
    @Column(name = "extended_loan", nullable = false)
    @NotNull(message = "Le statut 'prolongation' est obligatoire.")
    private Boolean extendedLoan;

    // -------------------------------------------------------------------------
    // NOMBRE DE PROLONGATIONS
    // -------------------------------------------------------------------------

    /**
     * Nombre total de prolongations appliquées à cet emprunt.
     * Toujours supérieur ou égal à 0.
     * Type SQL : INT NOT NULL.
     */
    @Column(name = "extension_count_loan", nullable = false)
    @NotNull(message = "Le compteur de prolongations est obligatoire.")
    private Integer extensionCountLoan;

    // -------------------------------------------------------------------------
    // STATUT MÉTIER (ENUM)
    // -------------------------------------------------------------------------

    /**
     * Statut métier de l’emprunt.
     * Liste fermée définie par LoanStatus :
     *      - ONGOING  : emprunt en cours ;
     *      - RETURNED : emprunt restitué ;
     *      - LATE     : emprunt en retard.
     * Type SQL : VARCHAR(50) NOT NULL.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_loan", length = 50, nullable = false)
    @NotNull(message = "Le statut de l'emprunt est obligatoire.")
    private LoanStatus statusLoan;

    // -------------------------------------------------------------------------
    // ORIGINE DE LA CRÉATION
    // -------------------------------------------------------------------------

    /**
     * Origine de création de l’emprunt :
     *      - USER   : créé par le membre lui-même ;
     *      - ADMIN  : créé par un administrateur ;
     *      - SYSTEM : créé automatiquement.
     * Type SQL : VARCHAR(50) NOT NULL.
     */
    @Column(name = "origin_loan", length = 50, nullable = false)
    @NotNull(message = "L'origine de l'emprunt est obligatoire.")
    @Size(min = 2, max = 50,
            message = "L'origine doit contenir entre 2 et 50 caractères.")
    private String originLoan;

    // -------------------------------------------------------------------------
    // SUPPRESSION LOGIQUE
    // -------------------------------------------------------------------------

    /**
     * Date de suppression logique.
     * Aucune suppression physique dans MagicLibrary.
     * Type SQL : DATE NULL.
     */
    @Column(name = "deleted_date_loan")
    private LocalDate deletedDateLoan;

    // -------------------------------------------------------------------------
    // NOTES LIBRES
    // -------------------------------------------------------------------------

    /**
     * Notes optionnelles associées à l’emprunt.
     * Longueur contrôlée applicativement.
     * Type SQL : TEXT NULL.
     */
    @Column(name = "notes_loan", columnDefinition = "TEXT")
    @Size(min = 2, max = 10_000,
            message = "Les notes doivent contenir entre 2 et 10 000 caractères.")
    private String notesLoan;

    // -------------------------------------------------------------------------
    // CONSTRUCTEURS
    // -------------------------------------------------------------------------

    /**
     * Constructeur sans argument requis par JPA.
     * Ne doit contenir aucune logique métier.
     * Utilisé automatiquement par Hibernate lors de l’instanciation.
     */
    public Loan() {
    }

    /**
     * Constructeur complet permettant d'instancier un emprunt avec tous les
     * champs obligatoires définis dans le dictionnaire LOAN.
     *
     * Ce constructeur n’applique aucune règle métier :
     * il sert uniquement à initialiser une entité en mémoire.
     */
    public Loan(
            User user,
            LocalDateTime startDateLoan,
            LocalDate dueDateLoan,
            Boolean returnedLoan,
            Boolean overdueLoan,
            Boolean extendedLoan,
            Integer extensionCountLoan,
            LoanStatus statusLoan,
            String originLoan
    ) {
        this.user = user;
        this.startDateLoan = startDateLoan;
        this.dueDateLoan = dueDateLoan;
        this.returnedLoan = returnedLoan;
        this.overdueLoan = overdueLoan;
        this.extendedLoan = extendedLoan;
        this.extensionCountLoan = extensionCountLoan;
        this.statusLoan = statusLoan;
        this.originLoan = originLoan;
    }

    // -------------------------------------------------------------------------
    // GETTERS & SETTERS (entièrement documentés – niveau CDA 30/20)
    // -------------------------------------------------------------------------

    /**
     * Retourne l'identifiant unique de l’emprunt.
     *
     * @return identifiant technique id_loan.
     */
    public Integer getIdLoan() {
        return idLoan;
    }

    /**
     * Définit l'identifiant de l’emprunt.
     * Ne doit jamais être modifié manuellement en dehors des tests.
     *
     * @param idLoan identifiant technique à affecter.
     */
    public void setIdLoan(Integer idLoan) {
        this.idLoan = idLoan;
    }

    /**
     * Retourne l'utilisateur auquel appartient cet emprunt.
     *
     * @return entité User associée.
     */
    public User getUser() {
        return user;
    }

    /**
     * Associe un utilisateur à l’emprunt.
     * Champ obligatoire (NOT NULL).
     *
     * @param user utilisateur propriétaire de l’emprunt.
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Retourne la date et heure de création de l’emprunt.
     *
     * @return date/heure de début.
     */
    public LocalDateTime getStartDateLoan() {
        return startDateLoan;
    }

    /**
     * Définit la date et l’heure de début de l’emprunt.
     *
     * @param startDateLoan date/heure de début à enregistrer.
     */
    public void setStartDateLoan(LocalDateTime startDateLoan) {
        this.startDateLoan = startDateLoan;
    }

    /**
     * Retourne la date d’échéance théorique.
     *
     * @return date de fin prévue.
     */
    public LocalDate getDueDateLoan() {
        return dueDateLoan;
    }

    /**
     * Définit la date d’échéance planifiée de l’emprunt.
     *
     * @param dueDateLoan date d’échéance à enregistrer.
     */
    public void setDueDateLoan(LocalDate dueDateLoan) {
        this.dueDateLoan = dueDateLoan;
    }

    /**
     * Retourne la date réelle de restitution.
     *
     * @return date/heure de retour ou null si non restitué.
     */
    public LocalDateTime getReturnDateLoan() {
        return returnDateLoan;
    }

    /**
     * Définit la date réelle de restitution.
     *
     * @param returnDateLoan date/heure de retour.
     */
    public void setReturnDateLoan(LocalDateTime returnDateLoan) {
        this.returnDateLoan = returnDateLoan;
    }

    /**
     * Indique si l’emprunt est marqué comme restitué.
     *
     * @return true si restitué, false sinon.
     */
    public Boolean getReturnedLoan() {
        return returnedLoan;
    }

    /**
     * Modifie le statut de restitution de l’emprunt.
     *
     * @param returnedLoan valeur booléenne (true = restitué).
     */
    public void setReturnedLoan(Boolean returnedLoan) {
        this.returnedLoan = returnedLoan;
    }

    /**
     * Indique si l’emprunt est actuellement en retard.
     *
     * @return true si en retard, sinon false.
     */
    public Boolean getOverdueLoan() {
        return overdueLoan;
    }

    /**
     * Met à jour le statut "en retard" de l’emprunt.
     *
     * @param overdueLoan valeur booléenne du retard.
     */
    public void setOverdueLoan(Boolean overdueLoan) {
        this.overdueLoan = overdueLoan;
    }

    /**
     * Indique si l’emprunt a déjà été prolongé.
     *
     * @return true si prolongé au moins une fois.
     */
    public Boolean getExtendedLoan() {
        return extendedLoan;
    }

    /**
     * Modifie le statut de prolongation de l’emprunt.
     *
     * @param extendedLoan valeur booléenne (true = prolongé).
     */
    public void setExtendedLoan(Boolean extendedLoan) {
        this.extendedLoan = extendedLoan;
    }

    /**
     * Retourne le nombre de prolongations appliquées.
     *
     * @return compteur de prolongations (>= 0).
     */
    public Integer getExtensionCountLoan() {
        return extensionCountLoan;
    }

    /**
     * Définit le nombre de prolongations appliquées à l’emprunt.
     *
     * @param extensionCountLoan compteur de prolongations.
     */
    public void setExtensionCountLoan(Integer extensionCountLoan) {
        this.extensionCountLoan = extensionCountLoan;
    }

    /**
     * Retourne le statut métier de l’emprunt.
     *
     * @return valeur de l’énumération LoanStatus.
     */
    public LoanStatus getStatusLoan() {
        return statusLoan;
    }

    /**
     * Modifie le statut métier de l’emprunt.
     *
     * @param statusLoan nouveau statut (ONGOING, RETURNED, LATE).
     */
    public void setStatusLoan(LoanStatus statusLoan) {
        this.statusLoan = statusLoan;
    }

    /**
     * Retourne l’origine de création de l’emprunt.
     *
     * @return USER, ADMIN ou SYSTEM.
     */
    public String getOriginLoan() {
        return originLoan;
    }

    /**
     * Définit l’origine de création de l’emprunt.
     *
     * @param originLoan chaîne décrivant l’origine (USER, ADMIN, SYSTEM).
     */
    public void setOriginLoan(String originLoan) {
        this.originLoan = originLoan;
    }

    /**
     * Retourne la date de suppression logique de l’emprunt.
     *
     * @return date de suppression ou null si actif.
     */
    public LocalDate getDeletedDateLoan() {
        return deletedDateLoan;
    }

    /**
     * Définit la date de suppression logique.
     *
     * @param deletedDateLoan date de suppression à enregistrer.
     */
    public void setDeletedDateLoan(LocalDate deletedDateLoan) {
        this.deletedDateLoan = deletedDateLoan;
    }

    /**
     * Retourne les notes associées à l’emprunt.
     *
     * @return texte libre ou null.
     */
    public String getNotesLoan() {
        return notesLoan;
    }

    /**
     * Définit les notes libres associées à l’emprunt.
     *
     * @param notesLoan contenu textuel des notes.
     */
    public void setNotesLoan(String notesLoan) {
        this.notesLoan = notesLoan;
    }

    // -------------------------------------------------------------------------
    // MÉTHODES UTILITAIRES : equals, hashCode, toString
    // -------------------------------------------------------------------------

    /**
     * Deux emprunts sont considérés égaux s’ils partagent le même identifiant
     * technique. Cette méthode garantit un comportement cohérent dans
     * l’ensemble du projet (List, Set, Hibernate, etc.).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Loan)) return false;
        Loan loan = (Loan) o;
        return Objects.equals(idLoan, loan.idLoan);
    }

    /**
     * Hash basé sur la clé primaire, conforme à equals().
     */
    @Override
    public int hashCode() {
        return Objects.hash(idLoan);
    }

    /**
     * Représentation lisible et utile en logs / debug.
     */
    @Override
    public String toString() {
        return "Loan{" +
                "idLoan=" + idLoan +
                ", statusLoan=" + statusLoan +
                '}';
    }
}
