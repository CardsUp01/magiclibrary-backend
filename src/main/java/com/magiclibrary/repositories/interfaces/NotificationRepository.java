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
 *      - les méthodes de démonstration ciblent exclusivement demoScenarioCode
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

    // -------------------------------------------------------------------------
    // SCÉNARIOS DE DÉMONSTRATION
    // -------------------------------------------------------------------------

    /**
     * Retourne toutes les notifications appartenant à un scénario de
     * démonstration.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     * @return liste des notifications correspondantes
     */
    List<Notification> findByDemoScenarioCode(String demoScenarioCode);

    /**
     * Vérifie si une notification existe pour un scénario de démonstration.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     * @return true si au moins une notification existe
     */
    boolean existsByDemoScenarioCode(String demoScenarioCode);

    /**
     * Compte les notifications associées à un scénario de démonstration.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     * @return nombre de notifications correspondantes
     */
    long countByDemoScenarioCode(String demoScenarioCode);

    /**
     * Retourne les notifications d'un utilisateur appartenant à un scénario de
     * démonstration donné.
     *
     * @param user utilisateur concerné
     * @param demoScenarioCode code fonctionnel de scénario
     * @return liste des notifications correspondantes
     */
    List<Notification> findByUserAndDemoScenarioCode(User user, String demoScenarioCode);

    /**
     * Supprime les notifications appartenant à un scénario de démonstration.
     *
     * Cette méthode est destinée exclusivement à la reconstruction contrôlée des
     * données de démonstration.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     */
    void deleteByDemoScenarioCode(String demoScenarioCode);
}