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
 *
 * Interface d'accès aux données pour l'entité Notification.
 *
 * Rôle :
 *      - récupération, création et mise à jour des notifications ;
 *      - filtrage par destinataire afin de respecter le cloisonnement utilisateur ;
 *      - récupération distincte des notifications reçues et archivées ;
 *      - récupération de l'historique des notifications envoyées manuellement ;
 *      - gestion des scénarios techniques de démonstration.
 *
 * =============================================================================
 * ORGANISATION DES BOÎTES DE NOTIFICATIONS
 * =============================================================================
 *
 * REÇUES
 * ------
 * Une notification appartient à la boîte "Reçues" lorsque :
 *
 *      notification.user = utilisateur connecté
 *      ET
 *      archivedByRecipient = false
 *
 * ARCHIVÉES
 * ----------
 * Une notification appartient à la boîte "Archivées" lorsque :
 *
 *      notification.user = utilisateur connecté
 *      ET
 *      archivedByRecipient = true
 *
 * ENVOYÉES
 * ---------
 * L'historique administratif "Envoyées" repose sur la présence d'un
 * expéditeur humain :
 *
 *      sentByUser IS NOT NULL
 *
 * Dans l'architecture actuelle de MagicLibrary, seules les notifications
 * manuelles créées par un ADMIN renseignent cet expéditeur.
 *
 * IMPORTANT :
 * l'archivage réalisé par le destinataire n'a aucun effet sur la boîte
 * "Envoyées". Une notification reste donc traçable côté administration
 * même lorsqu'elle a été archivée par son destinataire.
 *
 * =============================================================================
 * SÉCURITÉ
 * =============================================================================
 *
 * Les méthodes destinées aux boîtes "Reçues" et "Archivées" exigent toujours
 * explicitement le User propriétaire.
 *
 * La boîte "Envoyées" est un historique partagé entre les administrateurs ;
 * elle ne doit donc jamais être reconstruite à partir d'un findAll générique.
 *
 * =============================================================================
 * COMPATIBILITÉ
 * =============================================================================
 *
 * Les anciennes méthodes findByUser... sont temporairement conservées car
 * elles sont encore utilisées par NotificationServiceImpl au moment de cette
 * étape du développement.
 *
 * Elles pourront être réévaluées une fois l'ensemble du chantier migré et
 * validé en DEV.
 *
 * Les méthodes de démonstration continuent quant à elles de cibler
 * exclusivement demoScenarioCode.
 * =============================================================================
 */
public interface NotificationRepository
        extends JpaRepository<Notification, Integer> {

    // =========================================================================
    // MÉTHODES HISTORIQUES : NOTIFICATIONS D'UN UTILISATEUR
    // =========================================================================

    /**
     * Récupère toutes les notifications associées à un utilisateur,
     * triées par date décroissante.
     *
     * Cette méthode historique ne distingue pas encore les notifications
     * actives des notifications archivées.
     *
     * Elle est conservée temporairement pour compatibilité avec le service
     * actuel pendant la migration progressive du chantier.
     *
     * @param user utilisateur destinataire
     * @return liste de notifications
     */
    List<Notification> findByUserOrderByDateNotificationDesc(
            User user
    );

    /**
     * Récupère les notifications d'un utilisateur avec pagination,
     * triées par date décroissante.
     *
     * Cette méthode historique ne filtre pas encore l'état d'archivage.
     *
     * @param user utilisateur destinataire
     * @param pageable pagination demandée
     * @return page de notifications
     */
    Page<Notification> findByUserOrderByDateNotificationDesc(
            User user,
            Pageable pageable
    );

    /**
     * Récupère toutes les notifications avec pagination,
     * triées par date décroissante.
     *
     * IMPORTANT :
     * cette méthode générique ne doit pas être utilisée pour reconstruire
     * les boîtes sécurisées "Reçues", "Archivées" ou "Envoyées".
     *
     * Elle reste temporairement présente pour compatibilité avec
     * NotificationServiceImpl.
     *
     * @param pageable pagination demandée
     * @return page de notifications
     */
    Page<Notification> findAllByOrderByDateNotificationDesc(
            Pageable pageable
    );

    // =========================================================================
    // BOÎTE : NOTIFICATIONS REÇUES
    // =========================================================================

    /**
     * Récupère les notifications NON archivées d'un destinataire,
     * avec pagination et tri par date décroissante.
     *
     * Cette requête constitue la source de données de la future vue :
     *
     *      Notifications > Reçues
     *
     * Le filtre porte simultanément sur :
     * - le propriétaire de la notification ;
     * - l'état d'archivage personnel du destinataire.
     *
     * @param user destinataire propriétaire des notifications
     * @param pageable pagination demandée
     * @return page des notifications reçues et non archivées
     */
    Page<Notification>
    findByUserAndArchivedByRecipientFalseOrderByDateNotificationDesc(
            User user,
            Pageable pageable
    );

    // =========================================================================
    // BOÎTE : NOTIFICATIONS ARCHIVÉES
    // =========================================================================

    /**
     * Récupère les notifications archivées par un destinataire,
     * avec pagination et tri par date décroissante.
     *
     * Cette requête constitue la source de données de la future vue :
     *
     *      Notifications > Archivées
     *
     * L'archivage reste strictement personnel au destinataire.
     *
     * @param user destinataire propriétaire des notifications
     * @param pageable pagination demandée
     * @return page des notifications archivées
     */
    Page<Notification>
    findByUserAndArchivedByRecipientTrueOrderByDateNotificationDesc(
            User user,
            Pageable pageable
    );

    // =========================================================================
    // BOÎTE ADMIN : NOTIFICATIONS ENVOYÉES
    // =========================================================================

    /**
     * Récupère l'historique des notifications possédant un expéditeur humain,
     * avec pagination et tri par date décroissante.
     *
     * Dans le fonctionnement actuellement validé de MagicLibrary :
     *
     * - une notification manuelle envoyée par un ADMIN possède sentByUser ;
     * - une notification système automatique conserve sentByUser = null ;
     * - une ancienne notification créée avant la traçabilité conserve
     *   également sentByUser = null.
     *
     * Cette requête fournit donc l'historique partagé :
     *
     *      ADMIN > Notifications > Envoyées
     *
     * Aucun filtre sur archivedByRecipient n'est volontairement appliqué :
     * une notification reste dans l'historique administratif même lorsque
     * son destinataire l'archive.
     *
     * @param pageable pagination demandée
     * @return page des notifications envoyées manuellement
     */
    Page<Notification>
    findBySentByUserIsNotNullOrderByDateNotificationDesc(
            Pageable pageable
    );

    // =========================================================================
    // SCÉNARIOS DE DÉMONSTRATION
    // =========================================================================

    /**
     * Retourne toutes les notifications appartenant à un scénario de
     * démonstration.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     * @return liste des notifications correspondantes
     */
    List<Notification> findByDemoScenarioCode(
            String demoScenarioCode
    );

    /**
     * Vérifie si une notification existe pour un scénario de démonstration.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     * @return true si au moins une notification existe
     */
    boolean existsByDemoScenarioCode(
            String demoScenarioCode
    );

    /**
     * Compte les notifications associées à un scénario de démonstration.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     * @return nombre de notifications correspondantes
     */
    long countByDemoScenarioCode(
            String demoScenarioCode
    );

    /**
     * Retourne les notifications d'un utilisateur appartenant à un scénario
     * de démonstration donné.
     *
     * @param user utilisateur concerné
     * @param demoScenarioCode code fonctionnel de scénario
     * @return liste des notifications correspondantes
     */
    List<Notification> findByUserAndDemoScenarioCode(
            User user,
            String demoScenarioCode
    );

    /**
     * Supprime les notifications appartenant à un scénario de démonstration.
     *
     * Cette méthode reste destinée exclusivement à la reconstruction contrôlée
     * des données temporaires DEMO.
     *
     * Elle ne constitue en aucun cas un mécanisme de suppression fonctionnelle
     * accessible aux utilisateurs de MagicLibrary.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     */
    void deleteByDemoScenarioCode(
            String demoScenarioCode
    );
}