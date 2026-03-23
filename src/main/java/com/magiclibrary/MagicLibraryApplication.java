package com.magiclibrary;

// ============================================================================
// IMPORTS SPRING BOOT
// ============================================================================
// Classe principale pour démarrage automatique de l'application Spring Boot
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée principal de l'application backend MagicLibrary.
 *
 * Cette classe :
 *  - Démarre le contexte Spring Boot
 *  - Active la configuration automatique (@SpringBootApplication)
 *  - Prépare le backend pour exposer les endpoints REST et la sécurité
 */
@SpringBootApplication
public class MagicLibraryApplication {

    /**
     * Méthode main.
     *
     * Lancement de l'application Spring Boot en utilisant SpringApplication.run().
     *
     * @param args arguments passés à l'application (non utilisés dans le MVP)
     */
    public static void main(String[] args) {
        SpringApplication.run(MagicLibraryApplication.class, args);
    }

}