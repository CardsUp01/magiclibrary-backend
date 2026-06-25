package com.magiclibrary.mappers;

import org.springframework.stereotype.Component;

import com.magiclibrary.dto.user.UserCreateDTO;
import com.magiclibrary.dto.user.UserResponseDTO;
import com.magiclibrary.dto.user.UserUpdateDTO;
import com.magiclibrary.entities.Role;
import com.magiclibrary.entities.User;

/**
 * Mapper chargé des conversions entre l'entité User
 * et les DTO utilisés par les API et les interfaces.
 *
 * Cette classe centralise la création, la mise à jour
 * et la transformation des données utilisateur.
 */
@Component
public class UserMapper {

    /*
     * Construit une entité User à partir des données
     * fournies lors de la création d'un utilisateur.
     */
    public User toEntity(UserCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();
        user.setCivilityUser(dto.getCivility());
        user.setFirstNameUser(dto.getFirstName());
        user.setLastNameUser(dto.getLastName());
        user.setEmailUser(dto.getEmail());
        user.setPasswordUser(dto.getPassword());

        return user;
    }

    /*
     * Met à jour les champs modifiables d'un utilisateur
     * à partir du DTO reçu.
     */
    public void updateEntityFromDTO(User user, UserUpdateDTO dto) {
        if (user == null || dto == null) {
            return;
        }

        if (dto.getCivility() != null) {
            user.setCivilityUser(dto.getCivility());
        }

        if (dto.getFirstName() != null) {
            user.setFirstNameUser(dto.getFirstName());
        }

        if (dto.getLastName() != null) {
            user.setLastNameUser(dto.getLastName());
        }
    }

    /*
     * Transforme une entité User en DTO de réponse.
     */
    public UserResponseDTO toResponseDTO(User user) {
        if (user == null) {
            return null;
        }

        UserResponseDTO dto = new UserResponseDTO();

        dto.setIdUser(user.getIdUser());
        dto.setCivility(user.getCivilityUser());
        dto.setFirstName(formatFirstName(user.getFirstNameUser()));
        dto.setLastName(formatLastName(user.getLastNameUser()));
        dto.setEmail(user.getEmailUser());
        dto.setPhone(user.getPhoneUser());
        dto.setAddress(user.getAddressUser());
        dto.setAvatar(user.getAvatarUser());

        Role role = user.getRole();
        if (role != null) {
            dto.setIdRole(role.getIdRole());
            dto.setRoleLabel(role.getLabelRole());
        }

        dto.setActiveUser(user.getActiveUser());
        dto.setSubscriptionUser(user.getSubscriptionUser());
        dto.setDepositUser(user.getDepositUser());
        dto.setEmailVerified(user.getEmailVerifiedUser());
        dto.setFfapMember(user.getFfapMemberUser());
        dto.setFfapNumber(user.getFfapNumberUser());
        dto.setBio(user.getBioUser());
        dto.setNotes(user.getNotesUser());

        dto.setSignupDateUser(user.getSignupDateUser());
        dto.setAssociationJoinDateUser(user.getAssociationJoinDateUser());
        dto.setLastLoginUser(user.getLastLoginUser());
        dto.setUpdatedAtUser(user.getUpdatedAtUser());

        return dto;
    }

    /*
     * Normalise l'affichage du prénom avec une majuscule initiale.
     */
    private String formatFirstName(String firstName) {
        if (firstName == null || firstName.isBlank()) {
            return firstName;
        }

        String normalized = firstName.trim().toLowerCase();
        return normalized.substring(0, 1).toUpperCase() + normalized.substring(1);
    }

    /*
     * Normalise l'affichage du nom en majuscules.
     */
    private String formatLastName(String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return lastName;
        }

        return lastName.trim().toUpperCase();
    }
}