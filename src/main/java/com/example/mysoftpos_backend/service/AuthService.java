package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.*;
import com.example.mysoftpos_backend.entity.User;
import com.example.mysoftpos_backend.repository.UserRepository;
import com.example.mysoftpos_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final int MAX_FAILED_ATTEMPTS = 6;
    private static final int LOCKOUT_MINUTES = 30;

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;

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
}
