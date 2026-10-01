package com.magiclibrary.mongo.documents;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * =============================================================================
 * DOCUMENT MONGODB : ContactDocument
 * =============================================================================
 *
 * Représente un message de contact dans la base NoSQL MagicLibrary.
 *
 * Rôle :
 *      - stockage des messages envoyés via le formulaire web ;
 *      - gestion des réponses par un administrateur ;
 *      - suivi des statuts et dates ;
 *      - archivage logique indépendant côté membre et côté administration.
 *
 * Important :
 *
 * L'archivage ne modifie jamais le statut métier du message.
 *
 * Exemple :
 *      - un message ANSWERED peut rester actif ;
 *      - un message ANSWERED peut être archivé par le membre ;
 *      - le même message peut rester actif côté ADMIN ;
 *      - l'administration peut ensuite l'archiver indépendamment.
 *
 * Aucun archivage ne provoque de suppression physique du document MongoDB.
 */
@Document(collection = "contact")
public class ContactDocument {

    // -------------------------------------------------------------------------
    // IDENTIFIANT TECHNIQUE
    // -------------------------------------------------------------------------

    /**
     * Identifiant unique MongoDB du message.
     */
    @Id
    private String id;

    /**
     * Identifiant SQL de l'utilisateur ayant envoyé le message.
     *
     * Peut être null pour d'anciens usages ou des messages ne provenant pas
     * directement d'un compte utilisateur identifié.
     */
    @Field("id_user")
    private Integer idUser;

    // -------------------------------------------------------------------------
    // INFORMATIONS DE CONTACT
    // -------------------------------------------------------------------------

    /**
     * Nom de l'expéditeur du message.
     */
    @Field("name_contact")
    private String nameContact;

    /**
     * Email de l'expéditeur.
     */
    @Field("email_contact")
    private String emailContact;

    /**
     * Sujet du message.
     */
    @Field("subject_contact")
    private String subjectContact;

    /**
     * Contenu du message.
     */
    @Field("content_contact")
    private String contentContact;

    /**
     * Origine du message.
     *
     * Exemple : formulaire web.
     */
    @Field("origin_contact")
    private String originContact;

    /**
     * Statut métier du message.
     *
     * Exemples :
     *      - NEW
     *      - ANSWERED
     *
     * Ce statut reste indépendant de l'archivage.
     */
    @Field("status_contact")
    private String statusContact;

    // -------------------------------------------------------------------------
    // DATES / RÉPONSE ADMINISTRATIVE
    // -------------------------------------------------------------------------

    /**
     * Date de création du message.
     */
    @Field("date_contact")
    private LocalDateTime dateContact;

    /**
     * Indique si une réponse administrative a été envoyée.
     */
    @Field("response_sent_contact")
    private boolean responseSentContact;

    /**
     * Contenu de la réponse envoyée par l'administrateur.
     */
    @Field("response_content_contact")
    private String responseContentContact;

    /**
     * Identifiant SQL de l'administrateur ayant répondu.
     */
    @Field("answered_by_user_id")
    private Integer answeredByUserId;

    /**
     * Date de dernière mise à jour du message ou de sa réponse.
     */
    @Field("updated_at_contact")
    private LocalDateTime updatedAtContact;

    // -------------------------------------------------------------------------
    // ARCHIVAGE LOGIQUE CÔTÉ MEMBRE
    // -------------------------------------------------------------------------

    /**
     * Indique si l'auteur du message a archivé ce message dans son espace.
     *
     * Cet état est strictement personnel au membre.
     *
     * Une valeur true :
     *      - retire le message de la vue "Actifs" du membre ;
     *      - le place dans sa vue "Archivés" ;
     *      - n'a aucun effet sur la visibilité côté ADMIN.
     *
     * Les anciens documents MongoDB qui ne possèdent pas encore ce champ
     * sont naturellement interprétés comme non archivés.
     */
    @Field("archived_by_member")
    private boolean archivedByMember;

    /**
     * Date à laquelle le membre a archivé le message.
     *
     * Null lorsque le message n'est pas archivé côté membre.
     */
    @Field("archived_at_by_member")
    private LocalDateTime archivedAtByMember;

    // -------------------------------------------------------------------------
    // ARCHIVAGE LOGIQUE CÔTÉ ADMINISTRATION
    // -------------------------------------------------------------------------

    /**
     * Indique si l'équipe administrative a archivé ce message.
     *
     * Cet état est indépendant de l'archivage éventuellement réalisé
     * par le membre.
     *
     * Une valeur true :
     *      - retire le message de la vue "Actifs" côté ADMIN ;
     *      - le place dans la vue "Archivés" côté ADMIN ;
     *      - n'a aucun effet sur l'espace personnel du membre.
     */
    @Field("archived_by_admin")
    private boolean archivedByAdmin;

    /**
     * Date à laquelle le message a été archivé côté administration.
     *
     * Null lorsque le message n'est pas archivé côté ADMIN.
     */
    @Field("archived_at_by_admin")
    private LocalDateTime archivedAtByAdmin;

    // -------------------------------------------------------------------------
    // MARQUEUR TECHNIQUE DE DÉMONSTRATION
    // -------------------------------------------------------------------------

    /**
     * Code de scénario de démonstration associé au document Contact.
     *
     * Ce champ permet d'identifier les messages Contact MongoDB recréables sans
     * dépendre de l'ObjectId MongoDB, de l'email, du sujet, de l'origine ou du
     * contenu textuel du message.
     *
     * Les messages réels conservent une valeur null.
     */
    @Field("demoScenarioCode")
    private String demoScenarioCode;

    // -------------------------------------------------------------------------
    // CONSTRUCTEUR PAR DÉFAUT
    // -------------------------------------------------------------------------

    /**
     * Constructeur par défaut requis par Spring Data MongoDB.
     */
    public ContactDocument() {
    }

    // -------------------------------------------------------------------------
    // GETTERS / SETTERS
    // -------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public Integer getIdUser() {
        return idUser;
    }

    public void setIdUser(Integer idUser) {
        this.idUser = idUser;
    }

    public String getNameContact() {
        return nameContact;
    }

    public void setNameContact(String nameContact) {
        this.nameContact = nameContact;
    }

    public String getEmailContact() {
        return emailContact;
    }

    public void setEmailContact(String emailContact) {
        this.emailContact = emailContact;
    }

    public String getSubjectContact() {
        return subjectContact;
    }

    public void setSubjectContact(String subjectContact) {
        this.subjectContact = subjectContact;
    }

    public String getContentContact() {
        return contentContact;
    }

    public void setContentContact(String contentContact) {
        this.contentContact = contentContact;
    }

    public String getOriginContact() {
        return originContact;
    }

    public void setOriginContact(String originContact) {
        this.originContact = originContact;
    }

    public String getStatusContact() {
        return statusContact;
    }

    public void setStatusContact(String statusContact) {
        this.statusContact = statusContact;
    }

    public LocalDateTime getDateContact() {
        return dateContact;
    }

    public void setDateContact(LocalDateTime dateContact) {
        this.dateContact = dateContact;
    }

    public boolean isResponseSentContact() {
        return responseSentContact;
    }

    public void setResponseSentContact(boolean responseSentContact) {
        this.responseSentContact = responseSentContact;
    }

    public String getResponseContentContact() {
        return responseContentContact;
    }

    public void setResponseContentContact(String responseContentContact) {
        this.responseContentContact = responseContentContact;
    }

    public Integer getAnsweredByUserId() {
        return answeredByUserId;
    }

    public void setAnsweredByUserId(Integer answeredByUserId) {
        this.answeredByUserId = answeredByUserId;
    }

    public LocalDateTime getUpdatedAtContact() {
        return updatedAtContact;
    }

    public void setUpdatedAtContact(LocalDateTime updatedAtContact) {
        this.updatedAtContact = updatedAtContact;
    }

    public boolean isArchivedByMember() {
        return archivedByMember;
    }

    public void setArchivedByMember(boolean archivedByMember) {
        this.archivedByMember = archivedByMember;
    }

    public LocalDateTime getArchivedAtByMember() {
        return archivedAtByMember;
    }

    public void setArchivedAtByMember(LocalDateTime archivedAtByMember) {
        this.archivedAtByMember = archivedAtByMember;
    }

    public boolean isArchivedByAdmin() {
        return archivedByAdmin;
    }

    public void setArchivedByAdmin(boolean archivedByAdmin) {
        this.archivedByAdmin = archivedByAdmin;
    }

    public LocalDateTime getArchivedAtByAdmin() {
        return archivedAtByAdmin;
    }

    public void setArchivedAtByAdmin(LocalDateTime archivedAtByAdmin) {
        this.archivedAtByAdmin = archivedAtByAdmin;
    }

    public String getDemoScenarioCode() {
        return demoScenarioCode;
    }

    public void setDemoScenarioCode(String demoScenarioCode) {
        this.demoScenarioCode = demoScenarioCode;
    }
}