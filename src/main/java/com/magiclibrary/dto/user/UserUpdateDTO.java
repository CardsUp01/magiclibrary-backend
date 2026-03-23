package com.magiclibrary.dto.user;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Données autorisées pour la mise à jour du profil utilisateur (PUT /users/me).")
public class UserUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(
            description = "Nouvelle civilité de l’utilisateur (optionnelle).",
            example = "Mme",
            maxLength = 20,
            nullable = true
    )
    @Size(max = 20, message = "La civilité ne doit pas dépasser 20 caractères.")
    private String civility;

    @Schema(
            description = "Nouveau prénom de l’utilisateur (optionnel).",
            example = "Jean",
            minLength = 2,
            maxLength = 150,
            nullable = true
    )
    @Size(min = 2, max = 150, message = "Le prénom doit contenir entre 2 et 150 caractères.")
    private String firstName;

    @Schema(
            description = "Nouveau nom de l’utilisateur (optionnel).",
            example = "Martin",
            minLength = 2,
            maxLength = 150,
            nullable = true
    )
    @Size(min = 2, max = 150, message = "Le nom doit contenir entre 2 et 150 caractères.")
    private String lastName;

    public String getCivility() {
        return civility;
    }

    public void setCivility(String civility) {
        this.civility = civility;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}