package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.*;
import com.example.mysoftpos_backend.entity.User;
import com.example.mysoftpos_backend.repository.UserRepository;
import com.example.mysoftpos_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final int MAX_FAILED_ATTEMPTS = 6;
    private static final int LOCKOUT_MINUTES = 30;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;
    private final JavaMailSender mailSender;

    @Value("${app.auth.forgot-password.code-expiration-minutes:10}")
    private int forgotCodeExpirationMinutes;

    @Value("${app.auth.forgot-password.max-verify-attempts:5}")
    private int maxForgotVerifyAttempts;

    @Value("${app.auth.forgot-password.mail-from:no-reply@mysoftpos.local}")
    private String forgotMailFrom;

    public LoginResponse register(RegisterRequest req) {
        if (userRepo.existsByRole(ADMIN_ROLE)) {
            throw new RuntimeException("An admin account already exists. Please sign in.");
        }

        String phone = normalizePhone(req.getPhone());
        String email = normalizeEmail(req.getEmail());
        String fullName = normalizeText(req.getFullName());

        if (userRepo.existsByPhone(phone)) {
            throw new RuntimeException("Phone number already registered");
        }
        if (email != null && userRepo.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .phone(phone)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(ADMIN_ROLE)
                .fullName(fullName)
                .email(email)
                .build();
        userRepo.save(user);

        return buildLoginResponse(user);
    }

    public LoginResponse login(LoginRequest req) {
        String identifier = normalizeIdentifier(req.getUsername());

        User user = userRepo.findByPhone(identifier)
                .or(() -> userRepo.findByEmail(identifier))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // PA-DSS 3.x: Account lockout check
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Account locked until " + user.getLockedUntil());
        }

        if (!user.isActive()) {
            throw new RuntimeException("Account is disabled");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
                user.setFailedLoginAttempts(0);
            }
            userRepo.save(user);
            throw new RuntimeException("Invalid credentials");
        }

        // Reset on success
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepo.save(user);

        return buildLoginResponse(user);
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        String phone = jwtProvider.getSubjectFromToken(refreshToken);
        User user = userRepo.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return buildLoginResponse(user);
    }

    public Map<String, String> requestForgotPasswordCode(ForgotPasswordRequest req) {
        String email = normalizeEmail(req.getEmail());
        if (email == null) {
            throw new RuntimeException("Email is required");
        }

        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null || !user.isActive()) {
            // Do not leak account existence.
            return Map.of("message", "If your email exists, a verification code has been sent.");
        }

        String code = generateResetCode();
        user.setForgotPasswordCodeHash(passwordEncoder.encode(code));
        user.setForgotPasswordCodeExpiresAt(LocalDateTime.now().plusMinutes(forgotCodeExpirationMinutes));
        user.setForgotPasswordCodeVerifiedAt(null);
        user.setForgotPasswordCodeAttempts(0);
        userRepo.save(user);

        sendForgotPasswordEmail(user, code);
        return Map.of("message", "If your email exists, a verification code has been sent.");
    }

    public Map<String, String> verifyForgotPasswordCode(ForgotPasswordVerifyCodeRequest req) {
        User user = getUserForForgotPassword(req.getEmail());
        validateForgotCode(user, req.getCode());
        user.setForgotPasswordCodeVerifiedAt(LocalDateTime.now());
        userRepo.save(user);
        return Map.of("message", "Verification successful");
    }

    public Map<String, String> resetForgotPassword(ForgotPasswordResetRequest req) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new RuntimeException("Password confirmation does not match");
        }
        if (req.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new RuntimeException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        User user = getUserForForgotPassword(req.getEmail());
        validateForgotCode(user, req.getCode());

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        clearForgotPasswordState(user);
        userRepo.save(user);
        return Map.of("message", "Password reset successfully");
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = jwtProvider.generateAccessToken(user.getPhone(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getPhone());

        UserDto dto = UserDto.builder()
                .id(user.getId())
                .role(user.getRole())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .terminalId(user.getTerminalId())
                .serverIp(user.getServerIp())
                .serverPort(user.getServerPort())
                .active(user.isActive())
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(dto)
                .build();
    }

    private String normalizeIdentifier(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizePhone(String value) {
        return normalizeIdentifier(value);
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeIdentifier(value);
        return normalized.isEmpty() ? null : normalized.toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeText(String value) {
        String normalized = normalizeIdentifier(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private User getUserForForgotPassword(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (email == null) {
            throw new RuntimeException("Email is required");
        }
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification code"));
    }

    private void validateForgotCode(User user, String code) {
        if (code == null || code.isBlank()) {
            throw new RuntimeException("Verification code is required");
        }

        LocalDateTime expiresAt = user.getForgotPasswordCodeExpiresAt();
        String codeHash = user.getForgotPasswordCodeHash();
        if (codeHash == null || expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            clearForgotPasswordState(user);
            userRepo.save(user);
            throw new RuntimeException("Invalid or expired verification code");
        }

        if (user.getForgotPasswordCodeAttempts() >= maxForgotVerifyAttempts) {
            clearForgotPasswordState(user);
            userRepo.save(user);
            throw new RuntimeException("Too many invalid attempts. Please request a new code.");
        }

        if (!passwordEncoder.matches(code, codeHash)) {
            user.setForgotPasswordCodeAttempts(user.getForgotPasswordCodeAttempts() + 1);
            userRepo.save(user);
            throw new RuntimeException("Invalid or expired verification code");
        }
    }

    private void clearForgotPasswordState(User user) {
        user.setForgotPasswordCodeHash(null);
        user.setForgotPasswordCodeExpiresAt(null);
        user.setForgotPasswordCodeVerifiedAt(null);
        user.setForgotPasswordCodeAttempts(0);
    }

    private String generateResetCode() {
        int value = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format(java.util.Locale.ROOT, "%06d", value);
    }

    private void sendForgotPasswordEmail(User user, String code) {
        String email = user.getEmail();
        if (email == null || email.isBlank()) {
            throw new RuntimeException("User does not have a registered email");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom(forgotMailFrom);
        message.setSubject("MySoftPOS password reset code");
        message.setText(buildForgotMailBody(user, code));

        try {
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Unable to send verification email");
        }
    }

    private String buildForgotMailBody(User user, String code) {
        String name = user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName().trim()
                : "user";
        return "Hello " + name + ",\n\n"
                + "Your MySoftPOS verification code is: " + code + "\n"
                + "This code expires in " + forgotCodeExpirationMinutes + " minutes.\n\n"
                + "If you did not request this, please ignore this email.";
    }
}
