# 01 - Project Overview

## What is Auvier?

**Auvier** is a full-featured e-commerce platform designed as a luxury fashion store. It consists of two main parts:

1. **Admin Panel** - A dark-themed, futuristic dashboard for managing products, categories, orders, and users
2. **Customer Store** - A clean, minimalist storefront for browsing and purchasing products

---

## Why Was This Project Created?

This project serves as:
- A production-ready e-commerce template
- A learning resource for Spring Boot + Thymeleaf development
- A demonstration of clean architecture principles
- A showcase of modern admin panel design

---

## Tech Stack

### Backend

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 | Programming language (latest LTS) |
| **Spring Boot** | 4.0.1 | Application framework |
| **Spring Security** | 6.x | Authentication & authorization |
| **Spring Data JPA** | 3.x | Database access abstraction |
| **Hibernate** | 6.x | ORM (Object-Relational Mapping) |
| **PostgreSQL** | 15+ | Relational database |
| **Thymeleaf** | 3.1 | Server-side template engine |
| **MapStruct** | 1.6.3 | DTO ↔ Entity mapping |
| **Lombok** | 1.18.34 | Boilerplate code reduction |
| **Stripe SDK** | 26.1.0 | Payment processing |

### Frontend

| Technology | Purpose |
|------------|---------|
| **Thymeleaf** | HTML templating with server-side rendering |
| **CSS3** | Custom styling (no frameworks like Bootstrap) |
| **Vanilla JavaScript** | Interactivity (no React/Vue) |
| **Bootstrap Icons** | Icon library |
| **Google Fonts** | Typography (Montserrat, Cormorant Garamond) |

### Development Tools

| Tool | Purpose |
|------|---------|
| **Maven** | Build tool and dependency management |
| **Docker Compose** | Local PostgreSQL database |
| **Spring DevTools** | Hot reload during development |
| **Flyway** | Database migrations (optional) |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                       │
│  ┌──────────────────┐  ┌──────────────────┐  ┌───────────────┐  │
│  │  Admin Panel     │  │  Customer Store  │  │  REST APIs    │  │
│  │  (Thymeleaf)     │  │  (Thymeleaf)     │  │  (JSON)       │  │
│  └────────┬─────────┘  └────────┬─────────┘  └───────┬───────┘  │
└───────────┼─────────────────────┼────────────────────┼──────────┘
            │                     │                    │
            ▼                     ▼                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                         CONTROLLER LAYER                         │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  @Controller classes handle HTTP requests               │   │
│  │  - Receive requests                                      │   │
│  │  - Call services                                         │   │
│  │  - Return views or JSON                                  │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                         SERVICE LAYER                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Business logic lives here                               │   │
│  │  - Validation                                            │   │
│  │  - Calculations                                          │   │
│  │  - Transaction management                                │   │
│  │  - Orchestration of multiple repositories               │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                       REPOSITORY LAYER                           │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Spring Data JPA repositories                            │   │
│  │  - Auto-generated CRUD methods                           │   │
│  │  - Custom query methods                                  │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                        DATABASE LAYER                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  PostgreSQL Database                                     │   │
│  │  - Tables mapped from JPA entities                       │   │
│  │  - Hibernate handles SQL generation                      │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Key Design Decisions

### 1. Server-Side Rendering (Not SPA)

**Why Thymeleaf instead of React/Vue?**

- **SEO Friendly**: HTML is rendered on server, search engines see full content
- **Simpler Architecture**: No separate frontend build, no API versioning
- **Faster Initial Load**: No JavaScript bundle to download
- **Better for E-commerce**: Product pages need to be indexable

### 2. Custom CSS (Not Bootstrap)

**Why custom CSS?**

- **Unique Design**: Bootstrap sites look similar; this stands out
- **Smaller Bundle**: Only load what we need
- **Full Control**: No fighting with framework defaults
- **Learning**: Better understanding of CSS

### 3. PostgreSQL (Not MySQL)

**Why PostgreSQL?**

- **Better JSON Support**: Useful for flexible product attributes
- **Better Performance**: Especially for complex queries
- **ACID Compliant**: Data integrity for orders
- **Industry Standard**: Used by major companies

### 4. MapStruct (Not Manual Mapping)

**Why MapStruct?**

- **Type Safety**: Compile-time errors if mapping is wrong
- **Performance**: Generated code is as fast as manual
- **Less Boilerplate**: No repetitive getter/setter calls
- **Maintainability**: Changes propagate automatically

### 5. Layered Architecture

**Why this separation?**

- **Single Responsibility**: Each layer has one job
- **Testability**: Mock dependencies easily
- **Flexibility**: Change database without touching controllers
- **Team Collaboration**: Frontend devs work on controllers, backend on services

---

## Feature Overview

### Admin Panel Features

| Feature | Description |
|---------|-------------|
| Dashboard | Overview with statistics |
| Products | CRUD operations, variants |
| Categories | Hierarchical (parent/child) |
| Orders | View, update status |
| Users | Manage customers/admins |
| Activity Log | Audit trail of all actions |

### Customer Store Features

| Feature | Description |
|---------|-------------|
| Home | Hero banner, featured products |
| Shop | Product listing with filters |
| Product Detail | Images, variants, add to cart |
| Cart | View, modify quantities |
| Checkout | Address, payment (Stripe) |
| Orders | View order history |
| Profile | Update personal info |

---

## What Makes This Project Special

1. **Production-Ready**: Not just a tutorial; handles real scenarios
2. **Clean Code**: Follows best practices and patterns
3. **Beautiful UI**: Both admin and store are professionally designed
4. **Secure**: Proper authentication, authorization, CSRF protection
5. **Documented**: You're reading it!
6. **Auditable**: Activity logging tracks all admin actions

---

## Next Steps

- Continue to [02-GETTING-STARTED.md](./02-GETTING-STARTED.md) to set up the project
- Or jump to [03-PROJECT-STRUCTURE.md](./03-PROJECT-STRUCTURE.md) to understand the codebase
