# 02 - Getting Started

This guide will help you set up and run the Auvier project on your local machine.

---

## Prerequisites

Before you begin, ensure you have the following installed:

| Software | Version | Download |
|----------|---------|----------|
| Java JDK | 21 or higher | [Adoptium](https://adoptium.net/) |
| PostgreSQL | 15 or higher | [PostgreSQL](https://www.postgresql.org/download/) |
| Docker (optional) | Latest | [Docker Desktop](https://www.docker.com/products/docker-desktop) |
| Git | Latest | [Git](https://git-scm.com/) |
| IDE | Any | IntelliJ IDEA recommended |

---

## Option 1: Using Docker (Recommended)

The easiest way to run the database is with Docker Compose.

### Step 1: Start the Database

```bash
# From the project root directory
docker-compose up -d
```

This uses the `compose.yaml` file to start PostgreSQL:

```yaml
# compose.yaml
services:
  postgres:
    image: 'postgres:latest'
    environment:
      - POSTGRES_DB=auvier_db
      - POSTGRES_USER=auvier_user
      - POSTGRES_PASSWORD=auvier_pass
    ports:
      - '5432:5432'
```

### Step 2: Run the Application

```bash
# Using Maven wrapper (Windows)
.\mvnw.cmd spring-boot:run

# Using Maven wrapper (Mac/Linux)
./mvnw spring-boot:run
```

### Step 3: Access the Application

- **Store**: http://localhost:2525/
- **Admin Panel**: http://localhost:2525/admin
- **Login**: http://localhost:2525/login

---

## Option 2: Manual PostgreSQL Setup

If you prefer to install PostgreSQL directly:

### Step 1: Create Database

```sql
-- Connect to PostgreSQL as superuser
psql -U postgres

-- Create database and user
CREATE DATABASE auvier_db;
CREATE USER auvier_user WITH PASSWORD 'auvier_pass';
GRANT ALL PRIVILEGES ON DATABASE auvier_db TO auvier_user;

-- Connect to the database
\c auvier_db

-- Grant schema permissions
GRANT ALL ON SCHEMA public TO auvier_user;
```

### Step 2: Configure Application

The `application.properties` file is already configured:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/auvier_db
spring.datasource.username=auvier_user
spring.datasource.password=auvier_pass

# Hibernate will auto-create/update tables
spring.jpa.hibernate.ddl-auto=update

# Application runs on port 2525
server.port=2525
```

### Step 3: Run the Application

```bash
.\mvnw.cmd spring-boot:run
```

---

## Default Credentials

On first run, the application creates a default admin user:

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `admin123` |
| Role | `ADMIN` |

This is configured in `DataInitializer.java`:

```java
@PostConstruct
public void init() {
    if (!userService.existsByUsername("admin")) {
        UserRegistrationDto admin = new UserRegistrationDto();
        admin.setUsername("admin");
        admin.setPassword("admin123");
        admin.setEmail("admin@auvier.com");
        admin.setFirstName("Admin");
        admin.setLastName("User");
        userService.registerAdmin(admin);
    }
}
```

---

## Configuration Options

### application.properties

```properties
# Application name
spring.application.name=Auvier

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/auvier_db
spring.datasource.username=auvier_user
spring.datasource.password=auvier_pass

# Hibernate DDL mode
# - create: Drop and recreate tables on startup
# - create-drop: Same, but drop on shutdown
# - update: Update schema, keep data (RECOMMENDED for dev)
# - validate: Only validate, don't change
# - none: Do nothing
spring.jpa.hibernate.ddl-auto=update

# Server port
server.port=2525

# Logging
logging.level.root=INFO
logging.level.com.auvier=DEBUG
logging.file.name=logs/application.log

# Stripe Payment (get keys from https://dashboard.stripe.com/apikeys)
stripe.api.key=sk_test_your_secret_key_here
stripe.public.key=pk_test_your_public_key_here
stripe.webhook.secret=whsec_your_webhook_secret_here

# File Upload
file.upload-dir=uploads
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

---

## IDE Setup (IntelliJ IDEA)

### Step 1: Import Project

1. Open IntelliJ IDEA
2. File → Open → Select the `Auvier` folder
3. Wait for Maven to download dependencies

### Step 2: Enable Annotation Processing

This is required for Lombok and MapStruct:

1. File → Settings (Ctrl+Alt+S)
2. Build, Execution, Deployment → Compiler → Annotation Processors
3. Check "Enable annotation processing"
4. Click OK

### Step 3: Run Configuration

The IDE should auto-detect the Spring Boot application. If not:

1. Run → Edit Configurations
2. Add New → Spring Boot
3. Main class: `com.auvier.AuvierApplication`
4. Click OK
5. Run with the green play button

---

## Troubleshooting

### Port Already in Use

```
Web server failed to start. Port 2525 was already in use.
```

**Solution**: Find and kill the process:

```powershell
# Windows PowerShell
netstat -ano | findstr :2525
taskkill /PID <PID_NUMBER> /F

# Or change the port in application.properties
server.port=3000
```

### Database Connection Failed

```
Unable to obtain connection from database
```

**Solutions**:
1. Ensure PostgreSQL is running
2. Verify credentials in `application.properties`
3. Check if database `auvier_db` exists
4. Try connecting with psql or pgAdmin to verify

### Lombok Not Working

**Symptoms**: Getters/setters not found, red underlines

**Solutions**:
1. Enable annotation processing (see IDE Setup)
2. Install Lombok plugin in IDE
3. Rebuild project: Build → Rebuild Project

### MapStruct Not Generating

**Symptoms**: Mapper implementations not found

**Solutions**:
1. Clean and rebuild: `.\mvnw.cmd clean compile`
2. Check `target/generated-sources/annotations` folder
3. Ensure annotation processing is enabled

---

## Development Workflow

### Making Changes

1. **Java Files**: Changes are auto-compiled with DevTools
2. **Templates**: Refresh browser (no restart needed)
3. **Static Files (CSS/JS)**: Refresh browser
4. **application.properties**: Requires restart

### Useful Maven Commands

```bash
# Compile
.\mvnw.cmd compile

# Clean and compile
.\mvnw.cmd clean compile

# Run tests
.\mvnw.cmd test

# Package as JAR
.\mvnw.cmd package

# Run the packaged JAR
java -jar target/auvier-0.0.1-SNAPSHOT.jar
```

---

## Project Health Check

After starting, verify these URLs work:

| URL | Expected |
|-----|----------|
| http://localhost:2525/ | Store home page |
| http://localhost:2525/shop | Product listing |
| http://localhost:2525/login | Login form |
| http://localhost:2525/admin | Admin dashboard (after login) |

---

## Next Steps

- Read [03-PROJECT-STRUCTURE.md](./03-PROJECT-STRUCTURE.md) to understand the codebase
- Or jump to [07-ADMIN-PANEL.md](./07-ADMIN-PANEL.md) to start using the admin features
