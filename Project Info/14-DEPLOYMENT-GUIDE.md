# 14 - Deployment Guide

This document explains how to deploy the Auvier application to production.

---

## Prerequisites

- Java 21+ runtime
- PostgreSQL 15+ database
- Domain name (optional but recommended)
- SSL certificate (required for Stripe)

---

## Building for Production

### 1. Create Production Properties

Create `application-prod.properties`:

```properties
# Server
server.port=8080

# Database (use environment variables)
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USER}
spring.datasource.password=${DATABASE_PASSWORD}

# Hibernate - validate only in production
spring.jpa.hibernate.ddl-auto=validate

# Stripe (use live keys)
stripe.api.key=${STRIPE_SECRET_KEY}
stripe.public.key=${STRIPE_PUBLIC_KEY}
stripe.webhook.secret=${STRIPE_WEBHOOK_SECRET}

# Logging
logging.level.root=WARN
logging.level.com.auvier=INFO
logging.file.name=/var/log/auvier/application.log

# Security
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
```

### 2. Build the JAR

```bash
# Clean and build
./mvnw clean package -DskipTests

# JAR is created at:
# target/auvier-0.0.1-SNAPSHOT.jar
```

### 3. Run with Production Profile

```bash
java -jar target/auvier-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## Deployment Options

### Option 1: Traditional VPS (DigitalOcean, AWS EC2, etc.)

#### Server Setup

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Java 21
sudo apt install openjdk-21-jdk -y

# Verify
java -version

# Install PostgreSQL
sudo apt install postgresql postgresql-contrib -y

# Create database
sudo -u postgres psql
CREATE DATABASE auvier_db;
CREATE USER auvier_user WITH PASSWORD 'strong_password_here';
GRANT ALL PRIVILEGES ON DATABASE auvier_db TO auvier_user;
\q
```

#### Application Setup

```bash
# Create app directory
sudo mkdir -p /opt/auvier
sudo mkdir -p /var/log/auvier

# Create app user
sudo useradd -r -s /bin/false auvier

# Upload JAR (from local machine)
scp target/auvier-0.0.1-SNAPSHOT.jar user@server:/opt/auvier/

# Set permissions
sudo chown -R auvier:auvier /opt/auvier
sudo chown -R auvier:auvier /var/log/auvier
```

#### Create Systemd Service

```bash
sudo nano /etc/systemd/system/auvier.service
```

```ini
[Unit]
Description=Auvier E-Commerce Application
After=network.target postgresql.service

[Service]
Type=simple
User=auvier
Group=auvier
WorkingDirectory=/opt/auvier

# Environment variables
Environment="DATABASE_URL=jdbc:postgresql://localhost:5432/auvier_db"
Environment="DATABASE_USER=auvier_user"
Environment="DATABASE_PASSWORD=your_password"
Environment="STRIPE_SECRET_KEY=sk_live_xxx"
Environment="STRIPE_PUBLIC_KEY=pk_live_xxx"
Environment="STRIPE_WEBHOOK_SECRET=whsec_xxx"

ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar auvier-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# Restart on failure
Restart=always
RestartSec=10

# Logging
StandardOutput=append:/var/log/auvier/stdout.log
StandardError=append:/var/log/auvier/stderr.log

[Install]
WantedBy=multi-user.target
```

```bash
# Enable and start
sudo systemctl daemon-reload
sudo systemctl enable auvier
sudo systemctl start auvier

# Check status
sudo systemctl status auvier

# View logs
sudo journalctl -u auvier -f
```

#### Nginx Reverse Proxy

```bash
sudo apt install nginx -y
sudo nano /etc/nginx/sites-available/auvier
```

```nginx
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;

    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;

    # SSL certificates (use Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # SSL settings
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;

    # Proxy to Spring Boot
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket support (if needed)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # Serve static files directly
    location /assets/ {
        alias /opt/auvier/static/assets/;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    location /uploads/ {
        alias /opt/auvier/uploads/;
        expires 7d;
    }
}
```

```bash
# Enable site
sudo ln -s /etc/nginx/sites-available/auvier /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

#### SSL with Let's Encrypt

```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx -y

# Get certificate
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# Auto-renewal is set up automatically
```

---

### Option 2: Docker

#### Dockerfile

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk as build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
RUN ./mvnw clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create non-root user
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser

# Copy JAR from build stage
COPY --from=build /app/target/auvier-0.0.1-SNAPSHOT.jar app.jar

# Create upload directory
RUN mkdir uploads

EXPOSE 8080

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
```

#### docker-compose.prod.yml

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=jdbc:postgresql://db:5432/auvier_db
      - DATABASE_USER=auvier_user
      - DATABASE_PASSWORD=${DB_PASSWORD}
      - STRIPE_SECRET_KEY=${STRIPE_SECRET_KEY}
      - STRIPE_PUBLIC_KEY=${STRIPE_PUBLIC_KEY}
      - STRIPE_WEBHOOK_SECRET=${STRIPE_WEBHOOK_SECRET}
    depends_on:
      db:
        condition: service_healthy
    volumes:
      - uploads:/app/uploads
      - logs:/app/logs

  db:
    image: postgres:15
    environment:
      - POSTGRES_DB=auvier_db
      - POSTGRES_USER=auvier_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U auvier_user -d auvier_db"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
  uploads:
  logs:
```

```bash
# Deploy
docker-compose -f docker-compose.prod.yml up -d
```

---

### Option 3: Platform as a Service (Heroku, Railway, Render)

#### Railway Example

1. Push code to GitHub
2. Create new project on Railway
3. Add PostgreSQL service
4. Add environment variables
5. Deploy from GitHub

Railway automatically:
- Detects Spring Boot
- Sets DATABASE_URL
- Handles SSL

---

## Database Migration

### For First Deployment

```bash
# Generate initial schema
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.jpa.hibernate.ddl-auto=create"

# Export schema (optional)
pg_dump -U auvier_user -d auvier_db --schema-only > schema.sql
```

### For Updates

Use Flyway migrations:

```sql
-- src/main/resources/db/migration/V2__add_phone_to_users.sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
```

---

## Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection | `jdbc:postgresql://localhost:5432/auvier_db` |
| `DATABASE_USER` | DB username | `auvier_user` |
| `DATABASE_PASSWORD` | DB password | `strong_password` |
| `STRIPE_SECRET_KEY` | Stripe secret key | `sk_live_xxx` |
| `STRIPE_PUBLIC_KEY` | Stripe publishable key | `pk_live_xxx` |
| `STRIPE_WEBHOOK_SECRET` | Webhook signing secret | `whsec_xxx` |

---

## Stripe Production Setup

1. **Activate account** at https://dashboard.stripe.com/activate
2. **Get live keys** from https://dashboard.stripe.com/apikeys
3. **Create webhook endpoint**:
   - URL: `https://yourdomain.com/api/stripe/webhook`
   - Events: `payment_intent.succeeded`, `payment_intent.payment_failed`
4. **Copy webhook secret** to environment variable

---

## Monitoring & Maintenance

### Log Rotation

```bash
# /etc/logrotate.d/auvier
/var/log/auvier/*.log {
    daily
    missingok
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 auvier auvier
}
```

### Health Check

```java
// Add to application
@RestController
public class HealthController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
```

### Backups

```bash
# Database backup script
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
pg_dump -U auvier_user auvier_db > /backups/auvier_$DATE.sql
gzip /backups/auvier_$DATE.sql

# Keep last 7 days
find /backups -name "auvier_*.sql.gz" -mtime +7 -delete
```

---

## Security Checklist

- [ ] Use HTTPS everywhere
- [ ] Set secure cookie flags
- [ ] Use strong database password
- [ ] Keep dependencies updated
- [ ] Enable firewall (only 80, 443, 22)
- [ ] Use Stripe live mode
- [ ] Set up log monitoring
- [ ] Configure automated backups
- [ ] Test payment flow thoroughly

---

## Next Steps

- Read [15-FUTURE-IMPROVEMENTS.md](./15-FUTURE-IMPROVEMENTS.md) for roadmap
