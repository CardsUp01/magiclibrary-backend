package com.magiclibrary.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Configuration de persistance de l'application.
 *
 * Active les repositories JPA pour les données relationnelles
 * stockées dans MariaDB ainsi que les repositories MongoDB
 * utilisés par le module CONTACT.
 *
 * Cette configuration permet à Spring de détecter correctement
 * les entités et repositories des deux technologies de stockage.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.magiclibrary.repositories")
@EnableMongoRepositories(basePackages = "com.magiclibrary.mongo.repositories")
@EntityScan(basePackages = "com.magiclibrary.entities")
public class PersistenceConfig {
}