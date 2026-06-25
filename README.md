# MagicLibrary Backend

> Backend Spring Boot application for managing a thematic library (MagicLibrary)

---

## 🚀 Overview

MagicLibrary is a web application designed to manage a digital library system, including items, loans, users, notifications, and contact messages.

It is built as a REST backend with Spring Boot and follows a layered architecture.

---

## 🧱 Tech Stack

- Java 17+
- Spring Boot
- Spring Security (JWT authentication)
- Maven
- MySQL / MariaDB (relational data)
- MongoDB (Contact module)
- REST API architecture

---

## 🔐 Authentication

- JWT-based authentication
- Roles:
  - ADMIN
  - MEMBER
  - GUEST

---

## 📦 Main Features

### 👤 User management
- Register / login
- Role-based access control

### 📚 Library items
- Manage library catalog
- View available items

### 📦 Loan system
- Create loans
- Track borrowed items
- Loan status management

### 🔔 Notifications
- System notifications for users
- Admin alerts

### 💬 Contact module (MongoDB)
- Send messages
- Admin responses

---

## 🏗️ Architecture

```
Controller → Service → Repository → Database
```

- Clean layered architecture
- Separation of concerns
- RESTful endpoints

---

## ⚙️ Configuration

Profiles available:

- `application-dev.properties`
- `application-prod.properties`

Default profile:
```
dev
```

---

## ▶️ Run project

```bash
mvn clean install
mvn spring-boot:run
```

---

## 📁 Project Structure

```
src/
 ├── main/java
 │    └── com.magiclibrary
 │         ├── controller
 │         ├── service
 │         ├── repository
 │         ├── security
 │         └── dto
 ├── main/resources
 │    ├── application.properties
 │    ├── application-dev.properties
 │    └── application-prod.properties
```

---

## 🌐 Deployment

Compatible with:

- Railway
- Render
- Any cloud hosting supporting Java Spring Boot

---

## 📌 Status

✔ Backend functional  
✔ Authentication implemented  
✔ Database integration ready  
✔ Demo-ready version

---

## 📄 License

Educational / Portfolio project
