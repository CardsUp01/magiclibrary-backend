package com.magiclibrary.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.magiclibrary.repositories")
@EnableMongoRepositories(basePackages = "com.magiclibrary.mongo.repositories")
@EntityScan(basePackages = "com.magiclibrary.entities")
public class PersistenceConfig {
}