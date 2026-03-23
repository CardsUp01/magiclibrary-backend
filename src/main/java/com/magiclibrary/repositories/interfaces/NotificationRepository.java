package com.magiclibrary.repositories.interfaces;

import com.magiclibrary.entities.Notification;
import com.magiclibrary.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * =============================================================================
 * REPOSITORY : NotificationRepository
 * =============================================================================
 * Interface d’accès aux données pour l’entité Notification.
 *
 * Rôle :
 *      - récupération, création et mise à jour des notifications
 *      - filtrage par utilisateur pour respecter la sécurité
 *
 * Conformité :
 *      - strictement alignée avec le dictionnaire NOTIFICATION
 *      - utilisée par le service métier NotificationService / NotificationServiceImpl
 *      - supporte les cas d’usage US-07 et US-08 (MVP)
 *
 * Remarque :
 *      - JpaRepository fournit les méthodes CRUD standard
 *      - toute requête spécifique doit respecter les règles métier et la sécurité
 */
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // -------------------------------------------------------------------------
    // Récupération des notifications d’un utilisateur
    // -------------------------------------------------------------------------

    /**
     * Récupère toutes les notifications associées à un utilisateur,
     * triées par date décroissante (plus récentes en premier).
     *
     * @param user utilisateur cible
     * @return liste de notifications
     */
    List<Notification> findByUserOrderByDateNotificationDesc(User user);

    /**
     * Récupère les notifications d’un utilisateur avec pagination,
     * triées par date décroissante.
     *
     * @param user utilisateur cible
     * @param pageable pagination demandée
     * @return page de notifications
     */
    Page<Notification> findByUserOrderByDateNotificationDesc(User user, Pageable pageable);

    /**
     * Récupère toutes les notifications avec pagination,
     * triées par date décroissante.
     *
     * @param pageable pagination demandée
     * @return page de notifications
     */
    Page<Notification> findAllByOrderByDateNotificationDesc(Pageable pageable);
}