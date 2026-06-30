package com.magiclibrary.mongo.documents;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * =============================================================================
 * DOCUMENT MONGODB : ContactDocument
 * =============================================================================
 * Représente un message de contact dans la base NoSQL magiclibrary_nosql.
 *
 * Rôle :
 *      - stockage des messages envoyés via le formulaire web
 *      - gestion des réponses par un administrateur
 *      - suivi des statuts et dates pour US-12 / US-13
 *
 * Remarques :
 *      - tous les champs système (dates, statuts) sont gérés côté backend
 *      - aucun champ sensible non nécessaire pour l’API n’est exposé
 *      - utilisé par ContactServiceImpl et ContactMongoRepository
 */
@Document(collection = "contact")
public class ContactDocument {

    // -------------------------------------------------------------------------
    // IDENTIFIANT TECHNIQUE
    // -------------------------------------------------------------------------
    /** Identifiant unique MongoDB (ObjectId) */
    @Id
    private String id;

    /** Identifiant de l’utilisateur ayant envoyé le message, null si invité */
    @Field("id_user")
    private Integer idUser;

    // -------------------------------------------------------------------------
    // INFORMATIONS DE CONTACT
    // -------------------------------------------------------------------------
    /** Nom de l’expéditeur du message */
    @Field("name_contact")
    private String nameContact;

    /** Email de l’expéditeur */
    @Field("email_contact")
    private String emailContact;

    /** Sujet du message */
    @Field("subject_contact")
    private String subjectContact;

    /** Contenu du message */
    @Field("content_contact")
    private String contentContact;

    /** Origine du message (ex : formulaire web) */
    @Field("origin_contact")
    private String originContact;

    /** Statut du message (NEW, ANSWERED, etc.) */
    @Field("status_contact")
    private String statusContact;

    // -------------------------------------------------------------------------
    // DATES / TIMESTAMPS
    // -------------------------------------------------------------------------
    /** Date de création du message */
    @Field("date_contact")
    private LocalDateTime dateContact;

    /** Indique si une réponse a été envoyée */
    @Field("response_sent_contact")
    private boolean responseSentContact;

    /** Contenu de la réponse envoyée par l’administrateur */
    @Field("response_content_contact")
    private String responseContentContact;

    /** Identifiant SQL de l’administrateur ayant répondu */
    @Field("answered_by_user_id")
    private Integer answeredByUserId;

    /** Date de dernière mise à jour du message / réponse */
    @Field("updated_at_contact")
    private LocalDateTime updatedAtContact;

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
    /** Constructeur par défaut requis par Spring */
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

    public String getDemoScenarioCode() {
        return demoScenarioCode;
    }

    public void setDemoScenarioCode(String demoScenarioCode) {
        this.demoScenarioCode = demoScenarioCode;
    }
}