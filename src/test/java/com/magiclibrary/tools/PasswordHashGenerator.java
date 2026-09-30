package com.magiclibrary.tools;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.util.Scanner;

// -----------------------------------------------------------------------------
// IMPORTS SPRING SECURITY
// -----------------------------------------------------------------------------
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * =============================================================================
 * OUTIL DE DÉVELOPPEMENT : GÉNÉRATEUR DE HASH BCRYPT
 * =============================================================================
 * Utilitaire technique destiné exclusivement aux opérations de développement
 * et de test de MagicLibrary.
 *
 * Cette classe permet de générer manuellement un hash BCrypt à partir d'un mot
 * de passe saisi dans la console.
 *
 * CAS D'USAGE
 * -----------------------------------------------------------------------------
 * Cet outil peut notamment être utilisé pour :
 * - préparer un mot de passe destiné à un compte de test local ;
 * - insérer manuellement un utilisateur dans une base DEV ;
 * - vérifier le comportement de l'authentification Spring Security ;
 * - éviter de stocker un mot de passe en clair dans les scripts SQL ;
 * - produire ponctuellement un hash compatible avec le PasswordEncoder utilisé
 *   par MagicLibrary.
 *
 * EMPLACEMENT
 * -----------------------------------------------------------------------------
 * Cette classe est volontairement placée sous :
 *
 *     src/test/java/com/magiclibrary/tools
 *
 * Elle ne constitue pas une fonctionnalité métier de l'application et ne doit
 * donc pas faire partie du code principal livré en production.
 *
 * SÉCURITÉ
 * -----------------------------------------------------------------------------
 * Le mot de passe :
 * - est saisi directement dans la console ;
 * - n'est pas défini en dur dans le code source ;
 * - n'est pas sauvegardé par cette classe ;
 * - est immédiatement transformé avec BCrypt.
 *
 * Seul le hash généré est affiché.
 *
 * BCrypt génère automatiquement un sel aléatoire. Deux exécutions utilisant le
 * même mot de passe produiront donc normalement deux hashes différents, tout en
 * restant tous les deux valides pour la vérification du même mot de passe.
 *
 * IMPORTANT :
 * cet outil ne doit pas être utilisé pour afficher, retrouver ou déchiffrer un
 * mot de passe existant. BCrypt est un mécanisme de hachage unidirectionnel.
 * =============================================================================
 */
public class PasswordHashGenerator {

    /**
     * Point d'entrée autonome de l'utilitaire.
     *
     * Le traitement suit quatre étapes simples :
     * 1. création du moteur BCrypt ;
     * 2. lecture du mot de passe depuis l'entrée standard ;
     * 3. génération du hash ;
     * 4. affichage du hash dans la console.
     *
     * @param args arguments de ligne de commande non utilisés
     */
    public static void main(String[] args) {

        // ---------------------------------------------------------------------
        // INITIALISATION DU MOTEUR DE HACHAGE
        // ---------------------------------------------------------------------
        // BCryptPasswordEncoder utilise BCrypt, le même type de mécanisme que
        // celui attendu par Spring Security pour les mots de passe MagicLibrary.
        //
        // Aucun sel n'a besoin d'être fourni manuellement : BCrypt le génère
        // automatiquement lors de chaque appel à encode().
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // ---------------------------------------------------------------------
        // LECTURE DU MOT DE PASSE
        // ---------------------------------------------------------------------
        // Le mot de passe est fourni au moment de l'exécution afin qu'aucune
        // valeur sensible ne soit écrite directement dans le fichier source.
        Scanner scanner = new Scanner(System.in);

        System.out.print("Mot de passe : ");

        String password = scanner.nextLine();

        // ---------------------------------------------------------------------
        // GÉNÉRATION ET AFFICHAGE DU HASH
        // ---------------------------------------------------------------------
        // Le résultat peut ensuite être utilisé, par exemple, dans le champ
        // password_user d'un compte de test en base DEV.
        //
        // Le mot de passe original n'est jamais affiché par cette classe.
        System.out.println();
        System.out.println("Hash BCrypt :");
        System.out.println(encoder.encode(password));

        // ---------------------------------------------------------------------
        // LIBÉRATION DE LA RESSOURCE
        // ---------------------------------------------------------------------
        // Fermeture explicite du Scanner à la fin du traitement.
        scanner.close();
    }
}