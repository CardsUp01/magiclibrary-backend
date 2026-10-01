package com.magiclibrary.mappers;

import com.magiclibrary.dto.notification.NotificationRequestDTO;
import com.magiclibrary.dto.notification.NotificationResponseDTO;
import com.magiclibrary.entities.Notification;
import com.magiclibrary.entities.User;

/**
 * =============================================================================
 * MAPPER : NotificationMapper
 * =============================================================================
 *
 * Convertit les données entre :
 *
 * - l'entité Notification persistée en base ;
 * - NotificationRequestDTO utilisé lors de la création ;
 * - NotificationResponseDTO utilisé par les interfaces et API.
 *
 * =============================================================================
 * PRINCIPES DE SÉCURITÉ
 * =============================================================================
 *
 * Le mapper de création reçoit uniquement :
 *
 * - les données métier du NotificationRequestDTO ;
 * - le destinataire déjà résolu et contrôlé par le service.
 *
 * L'expéditeur humain d'une notification n'est volontairement PAS obtenu
 * depuis NotificationRequestDTO.
 *
 * Pour une notification administrative manuelle, l'expéditeur est déterminé
 * exclusivement dans NotificationServiceImpl à partir du requérant
 * authentifié et contrôlé côté backend.
 *
 * Cela empêche le navigateur ou un appel API de choisir arbitrairement
 * l'identité de l'expéditeur.
 *
 * =============================================================================
 * TRAÇABILITÉ
 * =============================================================================
 *
 * Lors de la conversion Entity -> ResponseDTO, le mapper expose :
 *
 * - l'identité du destinataire ;
 * - l'identité éventuelle de l'expéditeur humain ;
 * - l'état lu / non lu ;
 * - l'état d'archivage du destinataire ;
 * - les différentes métadonnées de la notification.
 *
 * Une notification système ou une ancienne notification peut parfaitement
 * ne posséder aucun sentByUser.
 * =============================================================================
 */
public final class NotificationMapper {

    // -------------------------------------------------------------------------
    // CONSTRUCTEUR PRIVÉ
    // -------------------------------------------------------------------------

    private NotificationMapper() {
        /*
         * Classe utilitaire stateless :
         * aucune instanciation n'est nécessaire.
         */
    }

    // =========================================================================
    // CONVERSION : REQUEST DTO -> ENTITY
    // =========================================================================

    /**
     * Convertit une demande de création en entité Notification.
     *
     * Le destinataire est obligatoirement chargé et contrôlé par le service
     * avant l'appel au mapper.
     *
     * L'expéditeur éventuel n'est volontairement pas renseigné ici :
     * NotificationServiceImpl s'en charge après authentification et contrôle
     * du rôle ADMIN.
     *
     * @param dto données métier de création
     * @param user utilisateur destinataire
     * @return nouvelle entité Notification
     */
    public static Notification toEntity(
            NotificationRequestDTO dto,
            User user
    ) {
        if (dto == null) {
            return null;
        }

        if (user == null) {
            throw new IllegalArgumentException(
                    "L'utilisateur cible est obligatoire pour créer une notification."
            );
        }

        Notification entity =
                new Notification();

        // ---------------------------------------------------------------------
        // DESTINATAIRE
        // ---------------------------------------------------------------------

        entity.setUser(
                user
        );

        // ---------------------------------------------------------------------
        // CONTENU MÉTIER
        // ---------------------------------------------------------------------

        entity.setTitleNotification(
                dto.getTitleNotification()
        );

        entity.setMessageNotification(
                dto.getMessageNotification()
        );

        entity.setTargetLinkNotification(
                dto.getTargetLinkNotification()
        );

        entity.setTypeNotification(
                dto.getTypeNotification()
        );

        entity.setCategoryNotification(
                dto.getCategoryNotification()
        );

        entity.setPriorityNotification(
                dto.getPriorityNotification()
        );

        // ---------------------------------------------------------------------
        // CHAMPS SYSTÈME INITIAUX
        // ---------------------------------------------------------------------

        /*
         * Une nouvelle notification est toujours non lue.
         *
         * NotificationServiceImpl réapplique également cette règle lors de
         * l'initialisation des champs système avant persistance.
         */
        entity.setReadNotification(
                false
        );

        /*
         * Une nouvelle notification doit apparaître dans la boîte "Reçues".
         *
         * L'archivage est une action ultérieure et explicite du destinataire.
         */
        entity.setArchivedByRecipient(
                false
        );

        entity.setArchivedAtByRecipient(
                null
        );

        /*
         * sentByUser n'est volontairement jamais alimenté ici.
         *
         * - création manuelle ADMIN :
         *      NotificationServiceImpl renseigne l'administrateur requérant ;
         *
         * - création système :
         *      sentByUser reste null.
         */

        /*
         * dateNotification reste gérée par l'entité et/ou par
         * NotificationServiceImpl avant la persistance.
         */
        return entity;
    }

    // =========================================================================
    // CONVERSION : ENTITY -> RESPONSE DTO
    // =========================================================================

    /**
     * Convertit une entité Notification en DTO de réponse.
     *
     * Cette conversion expose désormais les informations nécessaires
     * aux différentes boîtes :
     *
     * - Reçues ;
     * - Envoyées ;
     * - Archivées.
     *
     * @param entity notification persistée
     * @return DTO destiné au front-end
     */
    public static NotificationResponseDTO toResponseDTO(
            Notification entity
    ) {
        if (entity == null) {
            return null;
        }

        NotificationResponseDTO dto =
                new NotificationResponseDTO();

        // ---------------------------------------------------------------------
        // IDENTIFIANT DE LA NOTIFICATION
        // ---------------------------------------------------------------------

        dto.setIdNotification(
                entity.getIdNotification()
        );

        // ---------------------------------------------------------------------
        // DESTINATAIRE
        // ---------------------------------------------------------------------

        User recipient =
                entity.getUser();

        if (recipient != null) {

            dto.setIdUser(
                    recipient.getIdUser()
            );

            dto.setRecipientFirstName(
                    recipient.getFirstNameUser()
            );

            dto.setRecipientLastName(
                    recipient.getLastNameUser()
            );

            dto.setRecipientEmail(
                    recipient.getEmailUser()
            );

        } else {

            /*
             * Une notification valide doit normalement toujours posséder
             * un destinataire.
             *
             * Le mapper reste cependant défensif afin de ne pas provoquer
             * lui-même une NullPointerException sur une donnée incohérente.
             */
            dto.setIdUser(
                    null
            );

            dto.setRecipientFirstName(
                    null
            );

            dto.setRecipientLastName(
                    null
            );

            dto.setRecipientEmail(
                    null
            );
        }

        // ---------------------------------------------------------------------
        // EXPÉDITEUR HUMAIN ÉVENTUEL
        // ---------------------------------------------------------------------

        User sender =
                entity.getSentByUser();

        if (sender != null) {

            dto.setSentByUserId(
                    sender.getIdUser()
            );

            dto.setSenderFirstName(
                    sender.getFirstNameUser()
            );

            dto.setSenderLastName(
                    sender.getLastNameUser()
            );

            dto.setSenderEmail(
                    sender.getEmailUser()
            );

        } else {

            /*
             * Cas parfaitement légitimes :
             *
             * - notification système automatique ;
             * - notification historique antérieure à l'introduction
             *   de la traçabilité de l'expéditeur.
             */
            dto.setSentByUserId(
                    null
            );

            dto.setSenderFirstName(
                    null
            );

            dto.setSenderLastName(
                    null
            );

            dto.setSenderEmail(
                    null
            );
        }

        // ---------------------------------------------------------------------
        // CONTENU
        // ---------------------------------------------------------------------

        dto.setTitleNotification(
                entity.getTitleNotification()
        );

        dto.setMessageNotification(
                entity.getMessageNotification()
        );

        dto.setTargetLinkNotification(
                entity.getTargetLinkNotification()
        );

        // ---------------------------------------------------------------------
        // LECTURE
        // ---------------------------------------------------------------------

        dto.setReadNotification(
                entity.getReadNotification()
        );

        // ---------------------------------------------------------------------
        // DATE DE CRÉATION / ENVOI
        // ---------------------------------------------------------------------

        dto.setDateNotification(
                entity.getDateNotification()
        );

        // ---------------------------------------------------------------------
        // ARCHIVAGE DU DESTINATAIRE
        // ---------------------------------------------------------------------

        dto.setArchivedByRecipient(
                entity.getArchivedByRecipient()
        );

        dto.setArchivedAtByRecipient(
                entity.getArchivedAtByRecipient()
        );

        // ---------------------------------------------------------------------
        // TYPE / CATÉGORIE / PRIORITÉ
        // ---------------------------------------------------------------------

        dto.setTypeNotification(
                entity.getTypeNotification()
        );

        dto.setCategoryNotification(
                entity.getCategoryNotification()
        );

        dto.setPriorityNotification(
                entity.getPriorityNotification()
        );

        return dto;
    }
}