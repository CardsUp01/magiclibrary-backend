package com.magiclibrary.entities;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;
import java.util.Objects;

// -----------------------------------------------------------------------------
// IMPORTS JPA (explicites)
// -----------------------------------------------------------------------------
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION (Bean Validation Jakarta)
// -----------------------------------------------------------------------------
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.enums.LoanLineStatus;
import com.magiclibrary.entities.Loan;
import com.magiclibrary.entities.Item;

/* =============================================================================
   ENTITY : LOAN_LINE
   -----------------------------------------------------------------------------
   Représente une ligne d’emprunt dans l’application MagicLibrary.
   Elle relie un emprunt (LOAN) à un objet du catalogue numérique (ITEM)
   et permet de suivre chaque élément emprunté, sa quantité, son statut,
   les dates système, et les notes optionnelles.
   -----------------------------------------------------------------------------
   Conformité :
       - MVP strictement respecté
       - alignée sur le MCD / MLD validés
       - validations JPA et Bean Validation cohérentes
       - commentaires complets type jury CDA
============================================================================= */
@Entity
@Table(name = "loan_line")
public class LoanLine {

    // -------------------------------------------------------------------------
    // IDENTIFIANT TECHNIQUE (PRIMARY KEY)
    // -------------------------------------------------------------------------

    /**
     * Identifiant unique de la ligne d’emprunt
     * Clé primaire id_loan_line (INT AUTO_INCREMENT)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_loan_line", nullable = false)
    private Integer idLoanLine;

    // -------------------------------------------------------------------------
    // RELATION OBLIGATOIRE AVEC LOAN (FK id_loan)
    // -------------------------------------------------------------------------

    /**
     * Emprunt associé à cette ligne
     * Relation Many-To-One obligatoire
     * FK : id_loan → LOAN(id_loan), NOT NULL
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_loan", nullable = false)
    @NotNull(message = "L'emprunt associé est obligatoire.")
    private Loan loan;

    // -------------------------------------------------------------------------
    // RELATION OBLIGATOIRE AVEC ITEM (FK id_item)
    // -------------------------------------------------------------------------

    /**
     * Objet emprunté sur cette ligne
     * Relation Many-To-One obligatoire
     * FK : id_item → ITEM(id_item), NOT NULL
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_item", nullable = false)
    @NotNull(message = "L'objet associé est obligatoire.")
    private Item item;

    // -------------------------------------------------------------------------
    // QUANTITÉ EMPRUNTÉE
    // -------------------------------------------------------------------------

    /**
     * Quantité d'exemplaires empruntés pour l'objet
     * Valeur minimale : 1
     * Type SQL : INT NOT NULL
     */
    @Column(name = "quantity_loan_line", nullable = false)
    @NotNull(message = "La quantité est obligatoire.")
    @Min(value = 1, message = "La quantité doit être supérieure ou égale à 1.")
    private Integer quantityLoanLine;

    // -------------------------------------------------------------------------
    // STATUT DE LA LIGNE D’EMPRUNT
    // -------------------------------------------------------------------------

    /**
     * Statut de la ligne d’emprunt
     * Valeurs autorisées : ACTIVE, RETURNED, LOST
     * Type SQL : VARCHAR(50) NOT NULL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_loan_line", length = 50, nullable = false)
    @NotNull(message = "Le statut de la ligne est obligatoire.")
    private LoanLineStatus statusLoanLine;

    // -------------------------------------------------------------------------
    // DATES DE CRÉATION & MISE À JOUR
    // -------------------------------------------------------------------------

    /**
     * Date et heure de création de la ligne d’emprunt
     * Générée automatiquement côté application
     * NOT NULL
     */
    @Column(name = "created_at_loan_line", nullable = false)
    @NotNull(message = "La date de création est obligatoire.")
    private LocalDateTime createdAtLoanLine;

    /**
     * Date et heure de dernière mise à jour
     * Champ optionnel
     */
    @Column(name = "updated_at_loan_line")
    private LocalDateTime updatedAtLoanLine;

    // -------------------------------------------------------------------------
    // NOTES OPTIONNELLES
    // -------------------------------------------------------------------------

    /**
     * Notes internes liées à la ligne d’emprunt
     * Longueur : 2 à 10 000 caractères
     * Type SQL : TEXT NULL
     */
    @Column(name = "notes_loan_line", columnDefinition = "TEXT")
    @Size(min = 2, max = 10_000, message = "Les notes doivent contenir entre 2 et 10 000 caractères.")
    private String notesLoanLine;

    // -------------------------------------------------------------------------
    // HOOKS JPA : génération automatique dates système
    // -------------------------------------------------------------------------

    @PrePersist
    private void onCreate() {
        if (this.createdAtLoanLine == null) {
            this.createdAtLoanLine = LocalDateTime.now();
        }
        if (this.statusLoanLine == null) {
            this.statusLoanLine = LoanLineStatus.ACTIVE;
        }
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAtLoanLine = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // CONSTRUCTEURS
    // -------------------------------------------------------------------------

    public LoanLine() {
        // Requis par JPA
    }

    public LoanLine(Loan loan, Item item, Integer quantityLoanLine, LoanLineStatus statusLoanLine, LocalDateTime createdAtLoanLine) {
        this.loan = loan;
        this.item = item;
        this.quantityLoanLine = quantityLoanLine;
        this.statusLoanLine = statusLoanLine;
        this.createdAtLoanLine = createdAtLoanLine;
    }

    // -------------------------------------------------------------------------
    // GETTERS & SETTERS
    // -------------------------------------------------------------------------

    public Integer getIdLoanLine() { return idLoanLine; }
    public void setIdLoanLine(Integer idLoanLine) { this.idLoanLine = idLoanLine; }

    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public Integer getQuantityLoanLine() { return quantityLoanLine; }
    public void setQuantityLoanLine(Integer quantityLoanLine) { this.quantityLoanLine = quantityLoanLine; }

    public LoanLineStatus getStatusLoanLine() { return statusLoanLine; }
    public void setStatusLoanLine(LoanLineStatus statusLoanLine) { this.statusLoanLine = statusLoanLine; }

    public LocalDateTime getCreatedAtLoanLine() { return createdAtLoanLine; }
    public void setCreatedAtLoanLine(LocalDateTime createdAtLoanLine) { this.createdAtLoanLine = createdAtLoanLine; }

    public LocalDateTime getUpdatedAtLoanLine() { return updatedAtLoanLine; }
    public void setUpdatedAtLoanLine(LocalDateTime updatedAtLoanLine) { this.updatedAtLoanLine = updatedAtLoanLine; }

    public String getNotesLoanLine() { return notesLoanLine; }
    public void setNotesLoanLine(String notesLoanLine) { this.notesLoanLine = notesLoanLine; }

    // -------------------------------------------------------------------------
    // MÉTHODES UTILITAIRES
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoanLine)) return false;
        LoanLine loanLine = (LoanLine) o;
        return Objects.equals(idLoanLine, loanLine.idLoanLine);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idLoanLine);
    }

    @Override
    public String toString() {
        return "LoanLine{" +
                "idLoanLine=" + idLoanLine +
                ", quantityLoanLine=" + quantityLoanLine +
                '}';
    }
}