package com.magiclibrary.mongo.services;

import com.magiclibrary.mongo.dto.ContactReplyRequestDTO;
import com.magiclibrary.mongo.dto.ContactRequestDTO;
import com.magiclibrary.mongo.dto.ContactResponseDTO;

import java.util.List;

/**
 * =============================================================================================
 * MagicLibrary - Service : CONTACT
 * =============================================================================================
 *
 * Contrat métier du module CONTACT stocké dans MongoDB.
 *
 * Le service gère désormais deux espaces d'archivage totalement indépendants :
 *
 * - espace MEMBRE :
 *      le membre peut archiver et restaurer uniquement ses propres messages ;
 *
 * - espace ADMIN :
 *      l'équipe administrative peut archiver et restaurer les messages
 *      indépendamment de l'état d'archivage choisi par le membre.
 *
 * Important :
 *
 * - l'archivage est strictement logique ;
 * - aucun document MongoDB n'est supprimé ;
 * - l'archivage ne modifie jamais le statut métier NEW / ANSWERED ;
 * - l'archivage membre ne modifie pas l'affichage ADMIN ;
 * - l'archivage ADMIN ne modifie pas l'affichage membre.
 * =============================================================================================
 */
public interface ContactService {

    // =========================================================================================
    // MÉTHODES HISTORIQUES
    // =========================================================================================

    /**
     * US-12 : crée un nouveau message de contact.
     *
     * @param request données du message
     * @return message créé
     */
    ContactResponseDTO createContact(
            ContactRequestDTO request
    );

    /**
     * US-13 : récupère tous les messages de contact.
     *
     * Cette méthode historique est conservée pendant la migration progressive
     * vers les vues Actifs / Archivés.
     *
     * Elle ne doit pas être utilisée par les nouvelles interfaces pour
     * contourner le cloisonnement de l'archivage administratif.
     *
     * @return ensemble des messages de contact
     */
    List<ContactResponseDTO> getAllContacts();

    /**
     * US-13 : récupère tous les messages appartenant à un membre.
     *
     * Cette méthode historique est conservée pendant la migration progressive
     * vers les vues Actifs / Archivés.
     *
     * @param idUser identifiant SQL du membre
     * @return messages appartenant au membre
     */
    List<ContactResponseDTO> getContactsForUser(
            Integer idUser
    );

    /**
     * US-13 : obtient le détail d'un message de contact côté administration.
     *
     * @param id identifiant MongoDB du message
     * @return message demandé
     */
    ContactResponseDTO getContactById(
            String id
    );

    /**
     * US-13 : obtient le détail d'un message de contact appartenant
     * au membre connecté.
     *
     * @param id identifiant MongoDB du message
     * @param idUser identifiant SQL du membre
     * @return message accessible au membre
     */
    ContactResponseDTO getContactByIdForUser(
            String id,
            Integer idUser
    );

    /**
     * US-13 : répond à un message de contact côté administration.
     *
     * @param id identifiant MongoDB du message
     * @param request données de la réponse
     * @return message mis à jour
     */
    ContactResponseDTO replyToContact(
            String id,
            ContactReplyRequestDTO request
    );

    // =========================================================================================
    // ESPACE MEMBRE : MESSAGES ACTIFS
    // =========================================================================================

    /**
     * Récupère les messages actifs appartenant à un membre.
     *
     * Un message est considéré comme actif côté membre lorsque :
     *
     * archivedByMember == false
     *
     * L'état d'archivage administratif n'intervient jamais dans cette vue.
     *
     * @param idUser identifiant SQL du membre
     * @return messages actifs visibles par le membre
     */
    List<ContactResponseDTO> getActiveContactsForUser(
            Integer idUser
    );

    // =========================================================================================
    // ESPACE MEMBRE : MESSAGES ARCHIVÉS
    // =========================================================================================

    /**
     * Récupère les messages archivés par un membre.
     *
     * Un message appartient à cette vue lorsque :
     *
     * archivedByMember == true
     *
     * @param idUser identifiant SQL du membre
     * @return messages archivés par le membre
     */
    List<ContactResponseDTO> getArchivedContactsForUser(
            Integer idUser
    );

    /**
     * Archive logiquement un message dans l'espace du membre.
     *
     * Cette opération :
     * - ne supprime aucune donnée ;
     * - ne modifie pas NEW / ANSWERED ;
     * - ne modifie pas l'archivage ADMIN ;
     * - exige que le message appartienne au membre demandeur.
     *
     * @param id identifiant MongoDB du message
     * @param idUser identifiant SQL du membre demandeur
     * @return message archivé
     */
    ContactResponseDTO archiveContactForUser(
            String id,
            Integer idUser
    );

    /**
     * Restaure un message précédemment archivé par le membre.
     *
     * Cette opération replace le message dans la vue Actifs du membre
     * sans modifier son statut ni son état administratif.
     *
     * @param id identifiant MongoDB du message
     * @param idUser identifiant SQL du membre demandeur
     * @return message restauré
     */
    ContactResponseDTO restoreContactForUser(
            String id,
            Integer idUser
    );

    // =========================================================================================
    // ESPACE ADMIN : MESSAGES ACTIFS
    // =========================================================================================

    /**
     * Récupère les messages actifs dans l'espace administratif.
     *
     * Un message est considéré comme actif côté ADMIN lorsque :
     *
     * archivedByAdmin == false
     *
     * L'état d'archivage choisi par le membre n'intervient jamais
     * dans cette vue.
     *
     * L'identifiant du demandeur est transmis pour permettre également
     * un contrôle métier du rôle ADMIN au niveau du service.
     *
     * @param idRequester identifiant SQL de l'administrateur demandeur
     * @return messages actifs côté administration
     */
    List<ContactResponseDTO> getActiveContactsForAdmin(
            Integer idRequester
    );

    // =========================================================================================
    // ESPACE ADMIN : MESSAGES ARCHIVÉS
    // =========================================================================================

    /**
     * Récupère les messages archivés dans l'espace administratif.
     *
     * @param idRequester identifiant SQL de l'administrateur demandeur
     * @return messages archivés côté administration
     */
    List<ContactResponseDTO> getArchivedContactsForAdmin(
            Integer idRequester
    );

    /**
     * Archive logiquement un message côté administration.
     *
     * Cette opération :
     * - ne supprime aucun document MongoDB ;
     * - ne modifie pas NEW / ANSWERED ;
     * - ne modifie pas l'état d'archivage du membre ;
     * - exige un demandeur possédant le rôle ADMIN.
     *
     * @param id identifiant MongoDB du message
     * @param idRequester identifiant SQL de l'administrateur demandeur
     * @return message archivé côté administration
     */
    ContactResponseDTO archiveContactForAdmin(
            String id,
            Integer idRequester
    );

    /**
     * Restaure un message précédemment archivé côté administration.
     *
     * Le message revient dans la vue Actifs de l'administration
     * sans modifier son éventuel archivage côté membre.
     *
     * @param id identifiant MongoDB du message
     * @param idRequester identifiant SQL de l'administrateur demandeur
     * @return message restauré côté administration
     */
    ContactResponseDTO restoreContactForAdmin(
            String id,
            Integer idRequester
    );
}