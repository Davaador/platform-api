# Platform API

REST API for an English learning platform.
Built with Spring Boot, PostgreSQL and JWT.

## Features
- Register and log in with email and password
- Two roles: USER and ADMIN
- Short-lived JWT access tokens (stateless)
- Database migrations with Flyway
- Consistent JSON errors (RFC 9457 problem details)

## Project structure
The code is organized by feature, not by layer.

    src/main/java/mn/english/platform/
    +-- PlatformApiApplication.java
    +-- config/   security, JWT and application properties
    +-- common/   code shared by all features (error handling)
    +-- auth/     register, login, tokens
    +-- user/     users, roles, the /api/me endpoint
    +-- admin/    admin-only endpoints
    src/main/resources/
    +-- application.yaml
    +-- application-local.yaml   (local development only)
    +-- db/migration/            Flyway SQL files (V1__..., V2__...)

## Run locally
1. Start PostgreSQL: `docker compose up -d`
2. Start the API:

       ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

The API runs on http://localhost:8080

## Environment variables (production)
| Name | Description |
|---|---|
| `DB_URL` | JDBC URL of the database |
| `DB_USER` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Secret for signing tokens (at least 32 bytes) |
| `ADMIN_EMAIL` | Email of the first admin user |
| `ADMIN_PASSWORD` | Password of the first admin user (at least 12 characters) |

## Endpoints
| Method | Path | Access |
|---|---|---|
| POST | `/api/auth/register` | public |
| POST | `/api/auth/login` | public |
| GET | `/api/me` | any logged-in user |
| GET | `/api/admin/ping` | ADMIN only |
| GET | `/actuator/health` | public |

## Security notes
- Passwords are hashed. The hash is never returned by the API.
- A new account is always a USER. The first ADMIN comes from environment variables.
- Secrets are never stored in the code.

![Java CI](https://github.com/Davaador/platform-api/actions/workflows/ci.yml/badge.svg)