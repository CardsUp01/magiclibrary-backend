package com.magiclibrary.services.auth;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
// DTO pour login et register
import com.magiclibrary.dto.auth.LoginRequestDTO;
import com.magiclibrary.dto.auth.LoginResponseDTO;
import com.magiclibrary.dto.auth.RegisterRequestDTO;

/**
 * =============================================================================
 * SERVICE : AUTHENTIFICATION (INTERFACE)
 * =============================================================================
 * Interface contractuelle définissant les opérations liées à l’authentification
 * dans l’application MagicLibrary.
 *
 * Caractéristiques :
 *      - aucune logique métier ici ;
 *      - aucune dépendance à la persistence ;
 *      - signatures stables utilisées par les contrôleurs REST.
 */
public interface AuthService {

    // -------------------------------------------------------------------------
    // AUTHENTIFICATION — LOGIN (US-01)
    // -------------------------------------------------------------------------

    /**
     * Authentifie un utilisateur avec email et mot de passe.
     *
     * @param request DTO login contenant email et mot de passe
     * @return LoginResponseDTO contenant le token JWT, date d’expiration, id et rôle
     */
    LoginResponseDTO login(LoginRequestDTO request);

    // -------------------------------------------------------------------------
    // INSCRIPTION — REGISTER (US-02 PUBLIC)
    // -------------------------------------------------------------------------

    /**
     * Crée un compte utilisateur de type MEMBRE.
     *
     * Règles métier attendues dans l’implémentation :
     *      - vérification de l’unicité de l’email ;
     *      - mot de passe hashé avec BCrypt ;
     *      - affectation du rôle MEMBRE par défaut ;
     *      - compte actif à la création.
     *
     * @param request DTO contenant les informations d’inscription
     */
    void register(RegisterRequestDTO request);
}