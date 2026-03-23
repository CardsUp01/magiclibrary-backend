# MagicLibrary Backend

This is a sanitized public version of the project for demonstration purposes. Sensitive configurations have been removed.

Backend of the MagicLibrary project, a web application for managing a thematic library (magic-related items).

## 🚀 Features

- REST API with Spring Boot
- JWT authentication & authorization
- Role management (ADMIN / MEMBER / GUEST)
- Loan management system
- Notification system
- Contact module (MongoDB)

## 🛠️ Tech Stack

- Java 17
- Spring Boot
- Spring Security (JWT)
- MySQL (relational data)
- MongoDB (NoSQL data)
- Maven

## ⚙️ Configuration

The application uses environment-based configuration:

- `application-dev.properties` → development
- `application-prod.properties` → production

Sensitive data is NOT included in this repository.

## ▶️ Run the project

```bash
mvn clean install
mvn spring-boot:run
