# MySoftPOS Backend

`mysoftpos-backend` is a Spring Boot REST API for MySoftPOS.
It provides authentication, admin configuration management, transaction record sync, and test data synchronization.

## Overview

- Framework: Spring Boot `4.0.3`
- Java: `21`
- Database: MySQL (Flyway migrations)
- Auth: JWT (access + refresh)
- API docs: `/swagger-ui.html`
- OpenAPI JSON: `/api-docs`
- Health checks: `/` and `/health`

## Main Capabilities

- Auth and password operations (`/api/auth/**`)
- POS account management (`/api/pos-accounts/**`)
- Merchant and terminal management (`/api/merchants/**`, `/api/terminals/**`)
- Branch management per merchant (`/api/merchants/{merchantId}/branches/**`)
- Transaction record sync and admin query (`/api/transactions/**`)
- Test suites/test cases sync (`/api/test-suites/**`)
- Card data sync (`/api/cards/**`)

## Security Model

Configured in `src/main/java/com/example/mysoftpos_backend/config/SecurityConfig.java`:

- Public:
  - `/`
  - `/health`
  - `/api/auth/register`
  - `/api/auth/login`
  - `/api/auth/refresh`
  - `/api/auth/forgot-password/**`
  - `/swagger-ui/**`, `/api-docs/**`, `/v3/api-docs/**`
- Admin-only:
  - `/api/pos-accounts/**`
  - `/api/merchants/**`
  - `/api/terminals/**`
  - `/api/test-suites/**`
  - `GET /api/transactions/**`
- Authenticated:
  - Remaining endpoints (including transaction/card sync)

## Quick Endpoint Table (for Testing)

| Method | Path | Role |
| --- | --- | --- |
| GET | `/`, `/health` | Public |
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/refresh` | Public |
| POST | `/api/auth/forgot-password/request-code` | Public |
| POST | `/api/auth/forgot-password/verify-code` | Public |
| POST | `/api/auth/forgot-password/reset` | Public |
| PUT | `/api/auth/change-password` | Authenticated (`ADMIN`/`MERCHANT`) |
| ANY | `/api/pos-accounts/**`, `/api/merchants/**`, `/api/terminals/**`, `/api/test-suites/**` | `ADMIN` |
| GET | `/api/transactions/**` | `ADMIN` |
| POST | `/api/transactions/sync` | Authenticated (`ADMIN`/`MERCHANT`) |
| GET | `/api/cards` | Authenticated (`ADMIN`/`MERCHANT`) |
| POST | `/api/cards/sync` | Authenticated (`ADMIN`/`MERCHANT`) |

## Prerequisites

- JDK 21
- MySQL 8+
- Maven Wrapper (`mvnw`/`mvnw.cmd`)

## Environment Variables

Use environment variables instead of hardcoding secrets.
See `src/main/resources/application.example.yml` for full examples.

Required for normal startup:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET`

Common Flyway options:

- `SPRING_FLYWAY_ENABLED` (default `true`)
- `SPRING_FLYWAY_BASELINE_ON_MIGRATE` (useful for first migration on existing DB)

Optional mail settings for forgot-password flow:

- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `APP_FORGOT_PASSWORD_MAIL_FROM`

## Run Locally (Windows PowerShell)

From `mysoftpos-backend`:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/mysoftpos?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
$env:APP_JWT_SECRET="your_long_random_jwt_secret_at_least_32_chars"
.\mvnw.cmd spring-boot:run
```

Quick health check:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/health"
```

## Build and Test

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
```

Run critical integration tests only:

```powershell
.\mvnw.cmd "-Dtest=BackendCrudCriticalIT,BackendCrudControllerIT" test
```

## API Workflow (Typical)

1. Login via `POST /api/auth/login`
2. Use `Authorization: Bearer <accessToken>` for secured APIs
3. Admin configures merchants/branches/POS accounts/terminals
4. Devices push transaction records to `POST /api/transactions/sync`
5. Admin queries records with optional merchant/terminal filters via `GET /api/transactions`

## Notes on Naming and Data Scope

- Domain naming is standardized on `PosAccount` (legacy `User` naming is removed from active API contracts).
- Transaction queries are admin-scoped to avoid cross-admin data leakage.
- The project uses `TransactionRecord` naming in DTO/controller layers.

## Known Limitations

- App UI no longer exposes a forgot-password screen/flow.
- Backend still keeps `/api/auth/forgot-password/**` public for compatibility and manual QA/testing.
- Result: forgot-password APIs are callable from Swagger/Postman, but not reachable from current Android screens.

## Troubleshooting

- `Access denied for user ... (using password: NO)`
  - Check `SPRING_DATASOURCE_PASSWORD` and datasource credentials.
- Startup fails on Flyway baseline
  - Temporarily set `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` for first migration on existing DB.
- `401 Unauthorized`
  - Verify JWT token validity and role for the endpoint.

## Useful Paths

- `src/main/resources/application.example.yml`
- `src/main/resources/application.yml`
- `src/main/resources/db/migration`
- `scripts/smoke/render-branch-terminal-smoke.ps1`

