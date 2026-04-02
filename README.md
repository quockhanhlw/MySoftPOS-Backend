# MySoftPOS Backend - Huong dan su dung

Tai lieu nay huong dan chay va su dung `mysoftpos-backend` cho local/dev va deploy.

## 1) Tong quan

- Backend su dung Spring Boot + MySQL + Flyway.
- Xac thuc bang JWT.
- Tai khoan he thong dung domain `PosAccount` (khong dung `User` cu).
- Swagger UI: `/swagger-ui.html`
- OpenAPI docs: `/api-docs`

## 2) Yeu cau moi truong

- Java 21
- Maven Wrapper (`mvnw.cmd`) da co san trong repo
- MySQL (local hoac cloud, vi du Aiven)

## 3) Cau hinh can thiet

Khuyen nghi dat qua environment variables (khong hard-code secret).

Bien quan trong:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET`
- `SPRING_FLYWAY_ENABLED` (mac dinh `true`)
- `SPRING_FLYWAY_BASELINE_ON_MIGRATE` (lan dau voi DB cu co the can `true`)

Bien email forgot-password (neu dung OTP qua email):

- `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`
- `SPRING_MAIL_SMTP_AUTH`, `SPRING_MAIL_SMTP_STARTTLS_ENABLE`
- `APP_FORGOT_PASSWORD_MAIL_FROM`

Ban co the tham khao mau o `src/main/resources/application.example.yml`.

## 4) Chay backend local

Tu thu muc `mysoftpos-backend`:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/mysoftpos?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
$env:APP_JWT_SECRET="your_long_random_jwt_secret_at_least_32_chars"
.\mvnw.cmd spring-boot:run
```

Kiem tra nhanh:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/health"
```

## 5) Build va test

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
```

Chay test tich hop CRUD quan trong:

```powershell
.\mvnw.cmd "-Dtest=BackendCrudCriticalIT,BackendCrudControllerIT" test
```

## 6) Luong su dung co ban

### 6.1 Dang nhap admin

`POST /api/auth/login`

Body:

```json
{
  "username": "admin_username",
  "password": "admin_password"
}
```

Lay `accessToken` de goi API co bao mat:

`Authorization: Bearer <accessToken>`

### 6.2 Quan ly merchant / branch / account / terminal (admin)

- Merchant: `/api/merchants`
- Branch: `/api/merchants/{merchantId}/branches`
- Pos account: `/api/pos-accounts`
- Terminal: `/api/terminals`

Luu y moi:

- Co `DELETE /api/terminals/{id}`
- Co `DELETE /api/merchants/{merchantId}/branches/{branchId}` voi guardrails:
  - khong xoa branch `MAIN`
  - khong xoa branch dang co account/terminal lien ket

### 6.3 Dong bo giao dich

- Device push batch: `POST /api/transactions/sync` (authenticated)
- Admin xem lich su: `GET /api/transactions` (admin-only)

Backend da scope transaction theo admin, tranh lo du lieu cheo admin.

## 7) Phan quyen endpoint (tom tat)

Public:

- `/api/auth/register`
- `/api/auth/login`
- `/api/auth/refresh`
- `/api/auth/forgot-password/**`
- `/health`, `/swagger-ui/**`, `/api-docs/**`

Admin-only:

- `/api/pos-accounts/**`
- `/api/merchants/**`
- `/api/terminals/**`
- `/api/test-suites/**`
- `GET /api/transactions/**`

Authenticated (khac):

- `POST /api/transactions/sync`
- `PUT /api/auth/change-password`

## 8) Smoke test script

Da co script: `scripts/smoke/render-branch-terminal-smoke.ps1`

Vi du chay:

```powershell
.\scripts\smoke\render-branch-terminal-smoke.ps1 `
  -BaseUrl "https://your-backend-url" `
  -AdminUsername "admin_username" `
  -AdminPassword "admin_password" `
  -MerchantId 1
```

Dry-run:

```powershell
.\scripts\smoke\render-branch-terminal-smoke.ps1 `
  -BaseUrl "https://your-backend-url" `
  -AdminUsername "admin_username" `
  -AdminPassword "admin_password" `
  -MerchantId 1 `
  -DryRun
```

## 9) Loi thuong gap

- `Access denied for user ... (using password: NO)`
  - Chua set `SPRING_DATASOURCE_PASSWORD` hoac set sai.
- Loi JWT secret
  - Kiem tra `APP_JWT_SECRET` da set va du do dai.
- Flyway baseline
  - DB cu chua co `flyway_schema_history`: tam set `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true`, sau do doi lai `false`.

## 10) Tai lieu lien quan

- `src/main/resources/application.example.yml`
- `src/main/resources/application.yml`
- `src/main/resources/application-local.yml`
- `scripts/smoke/render-branch-terminal-smoke.ps1`

