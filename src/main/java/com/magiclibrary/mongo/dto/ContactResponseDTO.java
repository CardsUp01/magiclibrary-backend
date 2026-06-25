package com.magiclibrary.mongo.dto;

import java.time.LocalDateTime;

/**
 * DTO de réponse représentant un message de contact.
 *
 * Cette classe regroupe les informations nécessaires à la consultation,
 * au suivi et à l'administration des messages stockés dans MongoDB.
 */
public class ContactResponseDTO {

    private String id;
    private Integer idUser;
    private String name;
    private String email;
    private String subject;
    private String content;
    private String origin;
    private String status;
    private String statusLabel;
    private boolean answered;
    private String statusBadgeClass;
    private String senderRoleLabel;
    private LocalDateTime date;
    private boolean responseSent;
    private String responseContent;
    private Integer answeredByUserId;
    private String answeredByAdminLabel;
    private LocalDateTime updatedAt;

    public ContactResponseDTO() {
    }

    public ContactResponseDTO(
            String id,
            Integer idUser,
            String name,
            String email,
            String subject,
            String content,
            String origin,
            String status,
            String statusLabel,
            boolean answered,
            String statusBadgeClass,
            String senderRoleLabel,
            LocalDateTime date,
            boolean responseSent,
            String responseContent,
            Integer answeredByUserId,
            String answeredByAdminLabel,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.idUser = idUser;
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.content = content;
        this.origin = origin;
        this.status = status;
        this.statusLabel = statusLabel;
        this.answered = answered;
        this.statusBadgeClass = statusBadgeClass;
        this.senderRoleLabel = senderRoleLabel;
        this.date = date;
        this.responseSent = responseSent;
        this.responseContent = responseContent;
        this.answeredByUserId = answeredByUserId;
        this.answeredByAdminLabel = answeredByAdminLabel;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public Integer getIdUser() {
        return idUser;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public String getOrigin() {
        return origin;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public boolean isAnswered() {
        return answered;
    }

    public String getStatusBadgeClass() {
        return statusBadgeClass;
    }

    public String getSenderRoleLabel() {
        return senderRoleLabel;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public boolean isResponseSent() {
        return responseSent;
    }

    public String getResponseContent() {
        return responseContent;
    }

    public Integer getAnsweredByUserId() {
        return answeredByUserId;
    }

    public String getAnsweredByAdminLabel() {
        return answeredByAdminLabel;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}