package com.magiclibrary.mongo.repositories;

import com.magiclibrary.mongo.documents.ContactDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/* =================================================================================================
   MagicLibrary - Repository MongoDB : CONTACT
   -------------------------------------------------------------------------------------------------
   Ce repository assure la gestion des opérations CRUD sur les documents CONTACT stockés dans
   la base NoSQL magiclibrary_nosql. Il exploite l’interface MongoRepository fournie par Spring
   Data MongoDB, qui génère automatiquement toutes les méthodes standard :

   - save(document)              → création / mise à jour
   - findById(id)                → recherche d’un document (ObjectId → String)
   - findAll()                   → récupération de tous les messages
   - deleteById(id)              → suppression d’un document
   - count()                     → nombre total de documents
   - existsById(id)              → vérification d’existence

   Rôle dans MagicLibrary :
   - Couche d’accès aux données MongoDB dédiée au module CONTACT
   - Totalement isolée de MySQL (aucune dépendance JPA)
   - Utilisée par ContactServiceImpl pour US-12 et US-13
   - Utilisée par DemoContactInitializer pour garantir l'idempotence des messages
     de démonstration sans dépendre des identifiants MongoDB

   Méthodes personnalisées :
   - existsByEmailContactAndSubjectContactAndOriginContact(...)
     → vérifie l'existence d'un message à partir d'une clé métier naturelle
       utilisée pour éviter les doublons lors de l'initialisation de démonstration.
   ================================================================================================= */
@Repository
public interface ContactMongoRepository extends MongoRepository<ContactDocument, String> {

    /* =============================================================================================
       Vérification d'existence par clé métier naturelle.
       Utilisée par DemoContactInitializer pour éviter toute création de doublon
       sans dépendre de l'identifiant technique MongoDB.
       ============================================================================================= */
    boolean existsByEmailContactAndSubjectContactAndOriginContact(
            String emailContact,
            String subjectContact,
            String originContact
    );
}