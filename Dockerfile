# ============================================================================
# 🧙 MAGICLIBRARY - DOCKERFILE (RAILWAY PRODUCTION READY)
# ============================================================================
# 🎯 Objectif :
# Construire et exécuter une application Spring Boot de manière fiable
# sur Railway en environnement cloud containerisé.
#
# 🧱 Architecture en 2 étapes :
#   1. Build Maven (compilation + packaging JAR)
#   2. Runtime léger (JRE minimal pour exécution)
#
# ☁️ Spécificités Railway :
# - Port injecté dynamiquement via variable $PORT
# - Environnement stateless (pas de stockage local persistant)
# - Déploiement basé sur container Docker standard
# ============================================================================


# ============================================================================
# 🏗️ ETAPE 1 — BUILD MAVEN (COMPILATION)
# ============================================================================
# 📌 Image JDK complète utilisée uniquement pour compiler l'application
# 📌 Maven Wrapper garantit la cohérence du build sans dépendance locale
# 📌 -DskipTests accélère le pipeline CI/CD Railway
# ============================================================================

FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# 👉 Copie de l'intégralité du projet dans l'image de build
COPY . .

# 👉 Rend le Maven Wrapper exécutable (sécurité Linux container)
RUN chmod +x mvnw

# 👉 Compilation + packaging Spring Boot en mode production
# ⚠️ IMPORTANT : pas de profil Maven forcé (évite erreurs Railway)
RUN ./mvnw \
    -B \
    -DskipTests \
    clean package


# ============================================================================
# 🚀 ETAPE 2 — RUNTIME (EXÉCUTION LÉGÈRE)
# ============================================================================
# 📌 Image minimale JRE uniquement (pas de compilation ici)
# 📌 Réduction taille container + surface d’attaque
# ============================================================================

FROM eclipse-temurin:21-jre

WORKDIR /app

# 👉 Récupération du JAR généré lors de l'étape de build
COPY --from=build /app/target/*.jar app.jar


# ============================================================================
# 🌐 EXPOSITION DU PORT
# ============================================================================
# 📌 Railway injecte automatiquement la variable d’environnement PORT
# 📌 EXPOSE reste informatif pour Docker mais non bloquant
# ============================================================================

EXPOSE 8080


# ============================================================================
# ▶️ DÉMARRAGE DE L'APPLICATION (CRITIQUE POUR RAILWAY)
# ============================================================================
# ⚠️ Utilisation de "sh -c" obligatoire :
# → permet l’expansion de ${PORT} au runtime
# → évite les erreurs de type ENTRYPOINT non interprété
#
# 🔥 Fallback sécurisé :
# - Si PORT non défini → 8080 par défaut
# ============================================================================

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]