# 📚 MagicLibrary

![MagicLibrary](docs/images/home.png)

> Application web de gestion d'une bibliothèque associative — version V1 publique

---

## 🚀 Présentation du projet

MagicLibrary est une application web complète permettant la gestion d’une bibliothèque associative.

Elle couvre l’ensemble des besoins métier :
- Gestion du catalogue numérique
- Gestion des emprunts et retours
- Gestion des membres
- Gestion des notifications
- Formulaire de contact
- Interface d’administration

Le projet est développé dans un contexte réel associatif avec une architecture moderne full-stack.

---

## 🎯 Objectif

Fournir une plateforme interne sécurisée et ergonomique permettant :
- La gestion des ressources de la bibliothèque
- Le suivi des emprunts
- La communication entre membres et administrateurs
- L’administration centralisée des utilisateurs et contenus

---

## 🧰 Stack technique

- Java 21
- Spring Boot 3
- Spring Security
- JWT
- Thymeleaf (SSR)
- Maven
- MariaDB
- MongoDB
- API REST

---

## 🏗️ Architecture

Navigateur
→ Thymeleaf SSR
→ Spring Boot (Controllers / Services)
→ Spring Security + JWT
→ MariaDB (données relationnelles)
→ MongoDB (module contact)

---

## 🔐 Sécurité

- Authentification JWT
- Gestion des rôles (ADMIN / MEMBRE)
- Protection des endpoints
- Chiffrement des mots de passe avec BCrypt
- Accès contrôlé par session sécurisée

---

## 📸 Aperçu de l’application

### 🔐 Connexion
![Login](docs/images/login.png)

### 🏠 Accueil
![Accueil](docs/images/home.png)

### 📚 Catalogue
![Catalogue](docs/images/catalogue.png)

### 📄 Détail d’un objet
![Item](docs/images/item-details.png)

### 📦 Emprunts
![Emprunts](docs/images/loans.png)

### 🔔 Notifications
![Notifications](docs/images/notifications.png)

### ✉️ Contact
![Contact](docs/images/contact.png)

### 🛠 Administration
![Administration](docs/images/admin-dashboard.png)

### 👥 Gestion des membres
![Membres](docs/images/admin-members.png)

---

## ⚙️ Installation locale

```bash
git clone https://github.com/CardsUp01/magiclibrary-backend.git
cd magiclibrary-backend
mvn clean install
mvn spring-boot:run
```

---

## ▶️ Lancement

http://localhost:8080

---

## 👤 Comptes de démonstration

### Administrateur
- Email : admin@example.com
- Mot de passe : Admin123!

### Membre
- Email : lucas.demo@magiclibrary.fr
- Mot de passe : Demo123!

---

## 📦 Fonctionnalités principales

- Authentification sécurisée avec JWT
- Catalogue dynamique avec recherche et tri
- Gestion des emprunts multi-objets
- Espace d’administration complet
- Gestion des membres
- Notifications internes
- Formulaire de contact connecté au système
- Interface responsive (SSR)

---

## 🧠 Organisation du code

- Controllers (REST + SSR)
- Services (logique métier)
- Repositories (accès aux données)
- DTO (transfert de données)
- Security (JWT + Spring Security)

---

## 📈 Perspectives d’évolution

- Tableau de bord analytique
- Réservations d’ouvrages
- Notifications en temps réel (WebSocket)
- Amélioration de l’interface mobile
- Export PDF des emprunts
- Système de cotisation avancé

---

## 👨‍💻 Auteur

Hugo VERON  
Développeur Java / Spring Boot

---

## 📄 Licence

Projet de démonstration — usage libre pour évaluation technique.
