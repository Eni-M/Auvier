# Auvier Project Documentation

**Welcome to the Auvier E-Commerce Platform Documentation**

This comprehensive guide covers every aspect of the Auvier project - a full-stack e-commerce application built with Spring Boot, Thymeleaf, PostgreSQL, and Stripe for payments.

---

## 📚 Documentation Index

### Core Documentation

| # | Document | Description |
|---|----------|-------------|
| 01 | [Project Overview](./01-PROJECT-OVERVIEW.md) | What is Auvier, tech stack, architecture overview |
| 02 | [Getting Started](./02-GETTING-STARTED.md) | How to set up, run, and configure the project |
| 03 | [Project Structure](./03-PROJECT-STRUCTURE.md) | Folder organization and file purposes |
| 04 | [Database Design](./04-DATABASE-DESIGN.md) | Entity relationships, schema, and why designed this way |
| 05 | [Architecture Patterns](./05-ARCHITECTURE-PATTERNS.md) | Layered architecture, DTOs, Mappers, Services |
| 06 | [Security & Authentication](./06-SECURITY-AUTHENTICATION.md) | Spring Security, roles, login flow |
| 07 | [Admin Panel](./07-ADMIN-PANEL.md) | Admin features, CRUD operations, activity logging |
| 08 | [Store Frontend](./08-STORE-FRONTEND.md) | Customer-facing pages, shopping flow |
| 09 | [Payment Integration](./09-PAYMENT-INTEGRATION.md) | Stripe setup, checkout flow, webhooks |
| 10 | [API Reference](./10-API-REFERENCE.md) | All endpoints, request/response formats |
| 11 | [Frontend Guide](./11-FRONTEND-GUIDE.md) | CSS structure, JavaScript, Thymeleaf templates |
| 12 | [Error Handling](./12-ERROR-HANDLING.md) | Exception handling, error pages, logging |
| 13 | [Testing Guide](./13-TESTING-GUIDE.md) | How to test, what to test |
| 14 | [Deployment Guide](./14-DEPLOYMENT-GUIDE.md) | How to deploy to production |
| 15 | [Future Improvements](./15-FUTURE-IMPROVEMENTS.md) | Roadmap and enhancement ideas |
| 16 | [Technical Deep Dive](./16-TECHNICAL-DEEP-DIVE.md) | **Code walkthroughs**, request flow, how orders work |

---

## 🚀 Quick Links

- **Run the app:** `./mvnw spring-boot:run`
- **Admin panel:** `http://localhost:2525/admin`
- **Store:** `http://localhost:2525/`
- **Default admin:** `admin` / `admin123`

---

## 🏗️ Tech Stack at a Glance

| Layer | Technology |
|-------|------------|
| Backend Framework | Spring Boot 4.0.1 |
| Template Engine | Thymeleaf |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security 6 |
| Payments | Stripe |
| Build Tool | Maven |
| Java Version | 21 |

---

## 📁 Project Structure Overview

```
Auvier/
├── src/main/java/com/auvier/
│   ├── config/          # Configuration classes
│   ├── controllers/     # HTTP request handlers
│   ├── dtos/            # Data Transfer Objects
│   ├── entities/        # JPA entities (database tables)
│   ├── enums/           # Enumeration types
│   ├── exception/       # Custom exceptions
│   ├── infrastructure/  # Services (business logic)
│   ├── mappers/         # Entity ↔ DTO converters
│   └── repositories/    # Database access layer
├── src/main/resources/
│   ├── templates/       # Thymeleaf HTML templates
│   ├── static/          # CSS, JS, images
│   └── application.properties
└── Project Info/        # This documentation
```

---

## 🎯 Key Concepts

1. **Layered Architecture**: Controllers → Services → Repositories → Database
2. **DTO Pattern**: Never expose entities directly to views
3. **MapStruct**: Automatic entity ↔ DTO conversion
4. **Role-Based Access**: ADMIN and CUSTOMER roles
5. **Activity Logging**: All admin actions are tracked

---

**Last Updated:** February 3, 2026
