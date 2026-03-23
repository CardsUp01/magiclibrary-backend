package com.magiclibrary.dto.auth;

import java.util.Objects;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Données envoyées par un utilisateur lors de la tentative de connexion (US-01).")
public class LoginRequestDTO {

    @Schema(description = "Adresse email utilisée pour l’authentification.", example = "membre@example.com")
    @NotBlank(message = "L'adresse email est obligatoire.")
    @Email(message = "Le format de l'adresse email est invalide.")
    @Size(max = 255, message = "L'adresse email ne doit pas dépasser 255 caractères.")
    private String email;

    @Schema(description = "Mot de passe envoyé par l’utilisateur. Jamais renvoyé dans les réponses.")
    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(min = 8, max = 150, message = "Le mot de passe doit contenir entre 8 et 150 caractères.")
    private String password;

    @Schema(description = "Indique si l’utilisateur souhaite rester connecté.", defaultValue = "false")
    private boolean rememberMe;

    public LoginRequestDTO() {}

    public LoginRequestDTO(String email, String password) {
        this.email = email;
        this.password = password;
        this.rememberMe = false;
    }

    public LoginRequestDTO(String email, String password, boolean rememberMe) {
        this.email = email;
        this.password = password;
        this.rememberMe = rememberMe;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoginRequestDTO)) return false;
        LoginRequestDTO that = (LoginRequestDTO) o;
        return Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return "LoginRequestDTO{" +
                "email='" + email + '\'' +
                '}';
    }
}