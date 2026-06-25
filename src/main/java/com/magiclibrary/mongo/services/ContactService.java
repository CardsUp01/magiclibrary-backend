package com.magiclibrary.mongo.services;

import com.magiclibrary.mongo.dto.ContactReplyRequestDTO;
import com.magiclibrary.mongo.dto.ContactRequestDTO;
import com.magiclibrary.mongo.dto.ContactResponseDTO;

import java.util.List;

/* =============================================================================================
   MagicLibrary - Service : CONTACT (Interface)
   ---------------------------------------------------------------------------------------------
   Contrat métier du module CONTACT stocké dans MongoDB.
   ============================================================================================= */

public interface ContactService {

    // US-12 : Créer un message de contact (PUBLIC)
    ContactResponseDTO createContact(ContactRequestDTO request);

    // US-13 : Lister tous les messages de contact (ADMIN)
    List<ContactResponseDTO> getAllContacts();

    // US-13 : Lister les messages de contact d’un membre
    List<ContactResponseDTO> getContactsForUser(Integer idUser);

    // US-13 : Obtenir le détail d’un message de contact (ADMIN)
    ContactResponseDTO getContactById(String id);

    // US-13 : Obtenir le détail d’un message de contact accessible au membre connecté
    ContactResponseDTO getContactByIdForUser(String id, Integer idUser);

    // US-13 : Répondre à un message de contact (ADMIN)
    ContactResponseDTO replyToContact(String id, ContactReplyRequestDTO request);
}