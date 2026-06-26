# 📚 MagicLibrary Backend

> Spring Boot web application for managing a thematic library system.

---

## 🌐 Live Demo

MagicLibrary is deployed online and can be tested directly from a web browser:

👉 **https://magiclibrary-backend-production.up.railway.app**

No local installation is required to access the deployed demo version.

---

## 🚀 Overview

MagicLibrary is a web application designed to manage a digital library system including items, loans, users, notifications and contact messages.

It is built with Spring Boot and follows a clean layered architecture.

The project includes authentication, role-based access control, a library catalog, loan management, notifications and a MongoDB-based contact module.

---

## 🧱 Tech Stack

- Java 21
- Spring Boot 3
- Spring Security
- JWT authentication
- Maven
- MariaDB / MySQL
- MongoDB Atlas for the contact module
- Thymeleaf SSR
- Railway deployment

---

## 👤 Demo Accounts

The deployed demo version can be tested with the following accounts.

### Admin
- Email: admin@example.com
- Password: Admin123!

### 👤 Member 1
- Email: lucas.demo@magiclibrary.fr
- Password: Demo123!

### 👤 Member 2
- Email: sarah.demo@magiclibrary.fr
- Password: Demo123!

---

## 🔐 Authentication

- JWT-based authentication
- BCrypt password hashing
- Role-based access control

Roles:
- ADMIN
- MEMBER
- GUEST

---

## 📦 Main Features

### 👤 User management
- Register / login
- Secure authentication
- Role-based access control

### 📚 Library catalog
- Manage library items
- View available items
- Browse item details

### 📦 Loan system
- Create loans
- Track borrowed items
- Manage loan status

### 🔔 Notifications
- User notifications
- Admin alerts

### 💬 Contact module

The contact module uses MongoDB.

- Send contact messages
- Store messages separately from the relational database
- Admin message consultation

---

## 📸 Screenshots

### Login
![Login](docs/images/login.png)

### Home
![Home](docs/images/home.png)

### Catalog
![Catalog](docs/images/catalogue.png)

### Item details
![Item](docs/images/item-details.png)

### Loans
![Loans](docs/images/loans.png)

### Notifications
![Notifications](docs/images/notifications.png)

### Contact
![Contact](docs/images/contact.png)

### Admin
![Admin](docs/images/admin-dashboard.png)

---

## 🏗️ Architecture

Controller → Service → Repository → Database

- Clean layered architecture
- Separation of concerns
- Spring MVC / SSR structure
- Secure authentication layer
- Relational database for the main business data
- MongoDB storage for the contact module

---

## ⚙️ Configuration

The project uses separated Spring profiles:

- `application-dev.properties`
- `application-prod.properties`

Default profile:

- `dev`

Production configuration is handled through environment variables on Railway.

---

## ▶️ Run project locally

```bash
mvn clean install
mvn spring-boot:run
```

The local environment uses the development profile by default.

---

## 📁 Project Structure

```text
src/main/java/com.magiclibrary/
├── controller
├── service
├── repository
├── security
└── dto

src/main/resources/
├── application.properties
├── application-dev.properties
└── application-prod.properties
```

---

## 🌐 Deployment

The current demo version is deployed online with the following architecture:

- Spring Boot application hosted on Railway
- MariaDB database hosted on Railway
- MongoDB Atlas used for the contact module
- Production profile configured through environment variables
- Public Railway URL available for demonstration

Live application:

👉 **https://magiclibrary-backend-production.up.railway.app**

---

## 📌 Status

- Application deployed online
- Public demo version available
- Backend functional
- Authentication implemented
- JWT security active
- MariaDB integration configured
- MongoDB contact module configured
- Demo accounts available for recruiters
- Portfolio-ready version

---

## 📄 License

Educational / portfolio project.
