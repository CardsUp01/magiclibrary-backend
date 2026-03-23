package com.magiclibrary.entities;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.util.Objects;

// -----------------------------------------------------------------------------
// IMPORTS JPA (explicites, jamais de wildcard)
// -----------------------------------------------------------------------------
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * =============================================================================
 *  ENTITY : ROLE
 * =============================================================================
 *  Cette entité représente un rôle applicatif au sein de MagicLibrary.
 *
 *  Un rôle définit le niveau d'autorisation attribué à un utilisateur et sert
 *  de base à l’ensemble du système de sécurité (JWT, Spring Security,
 *  accès REST, restrictions métier).
 *
 *  Trois rôles fondamentaux existent dans le MVP :
 *      - ADMIN   : droits complets sur l’application ;
 *      - MEMBRE  : utilisateur authentifié standard ;
 *      - INVITE  : accès restreint en lecture seule.
 *
 *  Cette classe est strictement conforme :
 *      - au dictionnaire de données validé ;
 *      - au MCD / MLD / MPD ;
 *      - aux bonnes pratiques JPA et conventions MagicLibrary.
 *
 *  Rappel : la table ROLE contient uniquement deux champs :
 *      - id_role (clé primaire auto-incrémentée)
 *      - label_role (libellé textuel unique)
 * =============================================================================
 */
@Entity
@Table(name = "role")
public class Role {

    // -------------------------------------------------------------------------
    // IDENTIFIANT DU RÔLE (PRIMARY KEY)
    // -------------------------------------------------------------------------

    /**
     * Identifiant unique du rôle.
     * Généré automatiquement par la base de données.
     * Correspond à la clé primaire id_role (INT AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role", nullable = false)
    private Integer idRole;

    // -------------------------------------------------------------------------
    // LIBELLÉ DU RÔLE (ADMIN / MEMBRE / INVITE)
    // -------------------------------------------------------------------------

    /**
     * Libellé textuel du rôle.
     * Valeurs possibles dans le MVP : ADMIN, MEMBRE, INVITE.
     *
     * Contraintes :
     *      - obligatoire (NOT NULL)
     *      - unique (UNIQUE)
     *      - longueur : jusqu’à 20 caractères
     *
     * Le libellé doit correspondre EXACTEMENT aux valeurs utilisées
     * dans la couche de sécurité et dans l’énumération ERole.
     */
    @Column(name = "label_role", length = 20, nullable = false, unique = true)
    private String labelRole;

    // -------------------------------------------------------------------------
    // CONSTRUCTEURS
    // -------------------------------------------------------------------------

    /**
     * Constructeur sans argument requis par JPA.
     * Ne contient aucune logique métier.
     */
    public Role() {
    }

    /**
     * Constructeur principal permettant de créer un rôle via son libellé.
     *
     * @param labelRole libellé du rôle à initialiser.
     */
    public Role(String labelRole) {
        this.labelRole = labelRole;
    }

    // -------------------------------------------------------------------------
    // GETTERS & SETTERS
    // -------------------------------------------------------------------------

    /**
     * Retourne l'identifiant unique du rôle.
     *
     * @return identifiant id_role.
     */
    public Integer getIdRole() {
        return idRole;
    }

    /**
     * Définit l’identifiant unique du rôle.
     * Utilisé uniquement par JPA.
     *
     * @param idRole identifiant auto-incrémenté.
     */
    public void setIdRole(Integer idRole) {
        this.idRole = idRole;
    }

    /**
     * Retourne le libellé du rôle.
     *
     * @return libellé (ADMIN / MEMBRE / INVITE).
     */
    public String getLabelRole() {
        return labelRole;
    }

    /**
     * Définit le libellé du rôle.
     *
     * @param labelRole valeur textuelle du rôle.
     */
    public void setLabelRole(String labelRole) {
        this.labelRole = labelRole;
    }

    // -------------------------------------------------------------------------
    // MÉTHODES UTILITAIRES : equals, hashCode, toString
    // -------------------------------------------------------------------------

    /**
     * Compare deux instances de Role sur la base de leur identifiant unique.
     * Bonnes pratiques JPA :
     *      - comparaison réalisée uniquement sur la clé primaire persistée ;
     *      - aucune prise en compte de champs non persistants.
     *
     * @param o objet à comparer.
     * @return true si les idRole sont identiques.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return Objects.equals(idRole, role.idRole);
    }

    /**
     * Génère un hashCode basé exclusivement sur l'identifiant persistant.
     * Recommandation JPA : ne jamais inclure d’attributs mutables.
     *
     * @return hashCode basé sur idRole.
     */
    @Override
    public int hashCode() {
        return Objects.hash(idRole);
    }

    /**
     * Représentation textuelle concise du rôle.
     * Utile pour le logging, les traces système et le debugging.
     *
     * @return chaîne descriptive incluant idRole et labelRole.
     */
    @Override
    public String toString() {
        return "Role{" +
                "idRole=" + idRole +
                ", labelRole='" + labelRole + '\'' +
                '}';
    }
}
