package com.magiclibrary.mongo.repositories;

import com.magiclibrary.mongo.documents.ContactDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
   - Totalement isolée de MariaDB (aucune dépendance JPA)
   - Utilisée par ContactServiceImpl pour US-12 et US-13
   - Utilisée par DemoContactInitializer pour reconstruire les scénarios
     de démonstration sans impacter les messages réels.

   Important :
   - La méthode historique basée sur la clé métier naturelle est conservée
     temporairement afin d'assurer une transition progressive vers le nouveau
     mécanisme fondé sur demoScenarioCode.
   ================================================================================================= */
@Repository
public interface ContactMongoRepository extends MongoRepository<ContactDocument, String> {

    // =============================================================================================
    // MÉTHODE HISTORIQUE (CONSERVÉE TEMPORAIREMENT)
    // =============================================================================================

    /**
     * Vérifie l'existence d'un document Contact à partir d'une clé métier
     * naturelle.
     *
     * Cette méthode est conservée durant la phase de transition afin de rester
     * compatible avec l'ancien DemoContactInitializer.
     */
    boolean existsByEmailContactAndSubjectContactAndOriginContact(
            String emailContact,
            String subjectContact,
            String originContact
    );

    // =============================================================================================
    // SCÉNARIOS DE DÉMONSTRATION
    // =============================================================================================

    /**
     * Retourne tous les documents Contact appartenant à un scénario de
     * démonstration.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     * @return liste des documents correspondants
     */
    List<ContactDocument> findByDemoScenarioCode(String demoScenarioCode);

    /**
     * Vérifie si un scénario Contact de démonstration existe.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     * @return true si au moins un document existe
     */
    boolean existsByDemoScenarioCode(String demoScenarioCode);

    /**
     * Compte les documents Contact associés à un scénario de démonstration.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     * @return nombre de documents
     */
    long countByDemoScenarioCode(String demoScenarioCode);

    /**
     * Supprime tous les documents Contact appartenant à un scénario de
     * démonstration.
     *
     * Cette méthode est utilisée exclusivement lors de la reconstruction
     * contrôlée des données de démonstration.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     */
    void deleteByDemoScenarioCode(String demoScenarioCode);
}