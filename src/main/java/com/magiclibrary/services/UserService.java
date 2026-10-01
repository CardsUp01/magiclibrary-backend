package com.magiclibrary.services;

import java.util.List;

import org.springframework.data.domain.Page;

import com.magiclibrary.dto.user.UserCreateDTO;
import com.magiclibrary.dto.user.UserResponseDTO;
import com.magiclibrary.dto.user.UserUpdateDTO;

public interface UserService {

    UserResponseDTO createUser(UserCreateDTO userCreateDTO);

    UserResponseDTO getAuthenticatedUser(Integer userId);

    UserResponseDTO getUserById(Integer userId);

    UserResponseDTO updateAuthenticatedUser(Integer userId, UserUpdateDTO userUpdateDTO);

    UserResponseDTO updateUserByAdmin(Integer userId, UserUpdateDTO userUpdateDTO);

    // -------------------------------------------------------------------------
    // ARCHIVAGE / RESTAURATION DES UTILISATEURS
    // -------------------------------------------------------------------------

    /**
     * Archive logiquement un utilisateur.
     *
     * L'archivage repose sur le statut activeUser :
     *      - true  : utilisateur actif ;
     *      - false : utilisateur archivé.
     *
     * Cette opération applique les garde-fous métier nécessaires avant
     * désactivation du compte, notamment :
     *      - absence d'emprunt non restitué ;
     *      - interdiction pour un administrateur de s'archiver lui-même ;
     *      - conservation d'au moins un administrateur actif.
     *
     * Aucune donnée utilisateur ni aucun historique n'est supprimé.
     *
     * @param userId identifiant de l'utilisateur à archiver
     * @param authenticatedAdminEmail email de l'administrateur à l'origine
     *                                de l'opération
     * @return utilisateur après archivage
     */
    UserResponseDTO archiveUser(Integer userId, String authenticatedAdminEmail);

    /**
     * Restaure un utilisateur précédemment archivé.
     *
     * La restauration réactive exactement le même compte en repositionnant
     * activeUser à true. L'identifiant, le rôle et l'ensemble de l'historique
     * sont conservés.
     *
     * @param userId identifiant de l'utilisateur à restaurer
     * @return utilisateur après restauration
     */
    UserResponseDTO restoreUser(Integer userId);

    List<UserResponseDTO> getAllUsers();

    List<UserResponseDTO> getFilteredUsers(String search, String role, String status, String sort);

    Page<UserResponseDTO> getFilteredUsersPaged(
            String search,
            String role,
            String status,
            String sort,
            int page,
            int size
    );

    List<UserResponseDTO> suggestUsers(String query);
}