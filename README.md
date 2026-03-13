# MySoftPOS Backend

Spring Boot backend cho MySoftPOS, thiết kế để deploy trên Render và kết nối MySQL của Aiven bằng biến môi trường.

## Runtime configuration

Backend **không** nên chứa secret thật trong file được commit. Các biến môi trường cần có:

- `PORT` - Render tự cấp, local có thể bỏ qua
- `SPRING_DATASOURCE_URL` - JDBC URL của Aiven MySQL
- `SPRING_DATASOURCE_DRIVER_CLASS_NAME` - mặc định `com.mysql.cj.jdbc.Driver`
- `SPRING_DATASOURCE_USERNAME` - user của Aiven
- `SPRING_DATASOURCE_PASSWORD` - password của Aiven
- `SPRING_JPA_HIBERNATE_DDL_AUTO` - ví dụ `update`
- `SPRING_JPA_SHOW_SQL` - `true` hoặc `false`
- `SPRING_JPA_DATABASE_PLATFORM` - mặc định `org.hibernate.dialect.MySQLDialect`
- `APP_JWT_SECRET` - secret JWT đủ dài, chỉ cấu hình qua env
- `SPRING_MAIL_HOST` - SMTP host để gửi OTP qua email
- `SPRING_MAIL_PORT` - SMTP port (thường 587)
- `SPRING_MAIL_USERNAME` - SMTP username/email gửi
- `SPRING_MAIL_PASSWORD` - SMTP password/app-password
- `SPRING_MAIL_SMTP_AUTH` - bật/tắt SMTP auth (`true`/`false`)
- `SPRING_MAIL_SMTP_STARTTLS_ENABLE` - bật/tắt STARTTLS (`true`/`false`)
- `APP_FORGOT_PASSWORD_MAIL_FROM` - địa chỉ From cho email OTP
- `APP_FORGOT_PASSWORD_CODE_EXPIRATION_MINUTES` - thời gian hết hạn OTP
- `APP_FORGOT_PASSWORD_MAX_VERIFY_ATTEMPTS` - số lần nhập sai OTP tối đa

## Render + Aiven

Trên Render, cấu hình các environment variables ở trên trong service settings. Không commit password hoặc JWT secret vào:

- `src/main/resources/application.yml`
- `src/main/resources/application.example.yml`
- bất kỳ file `application-local*.yml` nào

## Local development

`src/main/resources/application-local.yml` đã được ignore trong git. Nếu cần chạy local với profile riêng, hãy tự tạo file này trên máy của bạn và **không commit**.

Ví dụ tối thiểu:

```yml
spring:
  datasource:
    url: jdbc:mysql://YOUR_AIVEN_HOST:YOUR_AIVEN_PORT/YOUR_DATABASE?sslMode=REQUIRED&serverTimezone=UTC&allowPublicKeyRetrieval=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: YOUR_AIVEN_USERNAME
    password: YOUR_AIVEN_PASSWORD

app:
  jwt:
    secret: YOUR_LONG_RANDOM_JWT_SECRET
```

## Build

```powershell
.\mvnw test
.\mvnw clean package
```

## Docker

`Dockerfile` build jar bằng Maven và chạy app bằng `JAVA_OPTS` nếu được cung cấp ở môi trường runtime.

