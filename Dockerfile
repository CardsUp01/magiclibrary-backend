# ============================================================================
# MAGICLIBRARY - DOCKERFILE
# ============================================================================
# Image Docker utilisée par Railway lorsque le builder "Dockerfile" est
# sélectionné.
#
# Architecture :
#   Étape 1 : compilation Maven
#   Étape 2 : image d'exécution légère
#
# Java : Eclipse Temurin 21
# Profil Spring utilisé : production
# ============================================================================


# ============================================================================
# ETAPE 1 - BUILD
# ============================================================================
# Compilation complète de l'application avec Maven Wrapper.
# Les tests sont volontairement ignorés lors du déploiement Railway afin
# d'accélérer les builds.
# ============================================================================

FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x mvnw

RUN ./mvnw \
    -B \
    -DskipTests \
    clean package \
    -Pproduction


# ============================================================================
# ETAPE 2 - RUNTIME
# ============================================================================
# Image minimale contenant uniquement le JRE nécessaire à l'exécution.
# ============================================================================

FROM eclipse-temurin:21-jre

WORKDIR /app


# ============================================================================
# COPIE DE L'APPLICATION
# ============================================================================
# Copie du fichier JAR généré lors de l'étape précédente.
# ============================================================================

COPY --from=build /app/target/*.jar app.jar


# ============================================================================
# PORT
# ============================================================================
# Railway injecte automatiquement la variable PORT.
# ============================================================================

EXPOSE 8080


# ============================================================================
# DEMARRAGE DE L'APPLICATION
# ============================================================================
# Le port est fourni automatiquement par Railway.
# Les variables d'environnement (DB, MongoDB, JWT...) sont injectées au
# démarrage.
# ============================================================================

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} $JAVA_OPTS -jar app.jar"]"" 
