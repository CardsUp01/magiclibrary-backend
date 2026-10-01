package com.magiclibrary.services;

import com.magiclibrary.dto.notification.NotificationRequestDTO;
import com.magiclibrary.dto.notification.NotificationResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * =============================================================================
 * SERVICE : NotificationService
 * =============================================================================
 *
 * Contrat métier de gestion des notifications MagicLibrary.
 *
 * Le service distingue désormais trois usages fonctionnels :
 *
 * - REÇUES :
 *      notifications non archivées appartenant au destinataire ;
 *
 * - ARCHIVÉES :
 *      notifications archivées logiquement par leur destinataire ;
 *
 * - ENVOYÉES :
 *      historique partagé des notifications manuelles envoyées
 *      par l'équipe administrative.
 *
 * L'archivage reste strictement logique :
 * aucune suppression physique n'est exposée par ce service.
 */
public interface NotificationService {

    // =========================================================================
    // MÉTHODES HISTORIQUES
    // =========================================================================

    /**
     * Récupère toutes les notifications d'un utilisateur.
     *
     * Cette méthode historique est conservée pendant la migration du chantier.
     *
     * @param idUser identifiant de l'utilisateur
     * @return notifications associées à l'utilisateur
     */
    List<NotificationResponseDTO> getNotificationsForUser(
            Integer idUser
    );

    /**
     * Récupère les notifications d'un utilisateur avec pagination.
     *
     * Cette méthode historique est conservée pour compatibilité avec
     * les contrôleurs existants pendant la migration progressive.
     *
     * @param idUser identifiant de l'utilisateur
     * @param page index de page
     * @param size taille de page
     * @return page de notifications
     */
    Page<NotificationResponseDTO> getNotificationsForUserPaged(
            Integer idUser,
            int page,
            int size
    );

    /**
     * Récupère toutes les notifications de la base avec pagination.
     *
     * Cette méthode historique ne doit pas être utilisée pour contourner
     * le cloisonnement des vues Reçues / Archivées / Envoyées.
     *
     * @param page index de page
     * @param size taille de page
     * @return page de notifications
     */
    Page<NotificationResponseDTO> getAllNotificationsPaged(
            int page,
            int size
    );

    // =========================================================================
    // BOÎTE : NOTIFICATIONS REÇUES
    // =========================================================================

    /**
     * Récupère uniquement les notifications non archivées
     * appartenant à un destinataire.
     *
     * Cette méthode constitue la source de données de la vue :
     *
     * Notifications > Reçues
     *
     * @param idUser identifiant du destinataire
     * @param page index de page
     * @param size taille de page
     * @return page de notifications reçues et non archivées
     */
    Page<NotificationResponseDTO> getReceivedNotificationsForUserPaged(
            Integer idUser,
            int page,
            int size
    );

    // =========================================================================
    // BOÎTE : NOTIFICATIONS ARCHIVÉES
    // =========================================================================

    /**
     * Récupère uniquement les notifications archivées
     * appartenant à un destinataire.
     *
     * Cette méthode constitue la source de données de la vue :
     *
     * Notifications > Archivées
     *
     * @param idUser identifiant du destinataire
     * @param page index de page
     * @param size taille de page
     * @return page de notifications archivées
     */
    Page<NotificationResponseDTO> getArchivedNotificationsForUserPaged(
            Integer idUser,
            int page,
            int size
    );

    // =========================================================================
    // BOÎTE ADMIN : NOTIFICATIONS ENVOYÉES
    // =========================================================================

    /**
     * Récupère l'historique partagé des notifications manuelles
     * envoyées par l'équipe administrative.
     *
     * Le requérant est explicitement transmis au service afin que
     * l'autorisation ADMIN soit contrôlée également au niveau métier.
     *
     * L'archivage effectué par le destinataire n'a aucun effet
     * sur cet historique.
     *
     * @param idRequester identifiant de l'administrateur requérant
     * @param page index de page
     * @param size taille de page
     * @return page des notifications envoyées
     */
    Page<NotificationResponseDTO> getSentNotificationsPaged(
            Integer idRequester,
            int page,
            int size
    );

    // =========================================================================
    // CRÉATION
    // =========================================================================

    /**
     * Crée une notification manuelle envoyée par un administrateur.
     *
     * @param requestDTO données de la notification
     * @param idRequester identifiant de l'administrateur expéditeur
     * @return notification créée
     */
    NotificationResponseDTO createNotification(
            NotificationRequestDTO requestDTO,
            Integer idRequester
    );

    /**
     * Crée une notification système automatique.
     *
     * Une notification système ne possède pas nécessairement
     * d'expéditeur humain.
     *
     * @param requestDTO données de la notification
     * @return notification créée
     */
    NotificationResponseDTO createSystemNotification(
            NotificationRequestDTO requestDTO
    );

    // =========================================================================
    // LECTURE
    // =========================================================================

    /**
     * Marque une notification comme lue.
     *
     * Seul le destinataire propriétaire peut effectuer cette opération.
     *
     * @param idNotification identifiant de la notification
     * @param idRequester identifiant de l'utilisateur effectuant l'action
     * @return notification mise à jour
     */
    NotificationResponseDTO markAsRead(
            Integer idNotification,
            Integer idRequester
    );

    // =========================================================================
    // ARCHIVAGE LOGIQUE
    // =========================================================================

    /**
     * Archive une notification pour son destinataire.
     *
     * L'opération :
     * - ne supprime aucune donnée ;
     * - retire la notification de la vue Reçues ;
     * - la rend disponible dans la vue Archivées ;
     * - ne modifie pas l'historique administratif Envoyées.
     *
     * Seul le destinataire propriétaire peut archiver la notification.
     *
     * @param idNotification identifiant de la notification
     * @param idRequester identifiant du destinataire effectuant l'action
     * @return notification archivée
     */
    NotificationResponseDTO archiveNotification(
            Integer idNotification,
            Integer idRequester
    );

    /**
     * Restaure une notification précédemment archivée.
     *
     * L'opération :
     * - remet archivedByRecipient à false ;
     * - efface la date d'archivage ;
     * - replace la notification dans la vue Reçues.
     *
     * Seul le destinataire propriétaire peut restaurer la notification.
     *
     * @param idNotification identifiant de la notification
     * @param idRequester identifiant du destinataire effectuant l'action
     * @return notification restaurée
     */
    NotificationResponseDTO restoreNotification(
            Integer idNotification,
            Integer idRequester
    );
}