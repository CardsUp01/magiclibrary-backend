package com.magiclibrary.mongo.demo;

import com.magiclibrary.enums.ContactStatus;

/**
 * =============================================================================
 * SCÉNARIO DE DÉMONSTRATION - CONTACT MONGODB
 * =============================================================================
 *
 * Représente un message de contact prédéfini destiné à alimenter la collection
 * MongoDB CONTACT dans le cadre de la démonstration MagicLibrary.
 *
 * Rôle :
 *      - isoler les données métier de démonstration ;
 *      - éviter de surcharger DemoContactInitializer avec du contenu statique ;
 *      - permettre une initialisation lisible, maintenable et idempotente ;
 *      - conserver une architecture propre pour les futurs environnements.
 *
 * Principes :
 *      - aucun identifiant technique MongoDB n'est utilisé ;
 *      - les utilisateurs SQL sont retrouvés dynamiquement par email ;
 *      - l'idempotence repose sur une clé métier naturelle :
 *        email + sujet + origine ;
 *      - les réponses administrateur sont optionnelles selon le statut.
 *
 * =============================================================================
 */
public record DemoContactScenario(
        String senderEmail,
        String senderName,
        String subject,
        String content,
        String origin,
        ContactStatus status,
        boolean responseSent,
        String responseContent,
        String answeredByAdminEmail,
        long createdDaysAgo,
        Long answeredDaysAgo
) {
}