package com.magiclibrary.mappers;

import com.magiclibrary.dto.notification.NotificationRequestDTO;
import com.magiclibrary.dto.notification.NotificationResponseDTO;
import com.magiclibrary.entities.Notification;
import com.magiclibrary.entities.User;

/* ============================================================================
   MAPPER : NotificationMapper
   ---------------------------------------------------------------------------
   Rôle :
   Convertit les données entre :
   - l’entité Notification (SQL)
   - les DTO Request et Response utilisés par l’API REST.
   Caractéristiques :
   - Classe stateless : aucune donnée stockée
   - Conversion explicite pour une maîtrise totale
   ============================================================================ */
public final class NotificationMapper {

    private NotificationMapper() {}

    /* ------------------------------------------------------------------------
       Conversion RequestDTO → Entity (création)
       L’utilisateur cible est déjà chargé par le service avant l’appel.
       ------------------------------------------------------------------------ */
    public static Notification toEntity(NotificationRequestDTO dto, User user) {

        if (dto == null) {
            return null;
        }
        if (user == null) {
            throw new IllegalArgumentException("L'utilisateur cible est obligatoire pour créer une notification.");
        }

        Notification entity = new Notification();

        entity.setUser(user);
        entity.setTitleNotification(dto.getTitleNotification());
        entity.setMessageNotification(dto.getMessageNotification());
        entity.setTargetLinkNotification(dto.getTargetLinkNotification());
        entity.setTypeNotification(dto.getTypeNotification());
        entity.setCategoryNotification(dto.getCategoryNotification());
        entity.setPriorityNotification(dto.getPriorityNotification());

        // Toujours false lors de la création
        entity.setReadNotification(false);

        // date_notification : gérée par l'entité (ou la DB)
        return entity;
    }

    /* ------------------------------------------------------------------------
       Conversion Entity → ResponseDTO
       ------------------------------------------------------------------------ */
    public static NotificationResponseDTO toResponseDTO(Notification entity) {

        if (entity == null) {
            return null;
        }

        NotificationResponseDTO dto = new NotificationResponseDTO();

        dto.setIdNotification(entity.getIdNotification());

        User user = entity.getUser();
        dto.setIdUser(user != null ? user.getIdUser() : null);

        dto.setTitleNotification(entity.getTitleNotification());
        dto.setMessageNotification(entity.getMessageNotification());
        dto.setTargetLinkNotification(entity.getTargetLinkNotification());
        dto.setReadNotification(entity.getReadNotification());
        dto.setDateNotification(entity.getDateNotification());
        dto.setTypeNotification(entity.getTypeNotification());
        dto.setCategoryNotification(entity.getCategoryNotification());
        dto.setPriorityNotification(entity.getPriorityNotification());

        return dto;
    }
}