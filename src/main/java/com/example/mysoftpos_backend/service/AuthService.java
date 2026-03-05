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

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;

    private static final int MAX_FAILED_ATTEMPTS = 6;
    private static final int LOCKOUT_MINUTES = 30;

    public LoginResponse register(RegisterRequest req) {
        if (userRepo.existsByPhone(req.getPhone())) {
            throw new RuntimeException("Phone number already registered");
        }

        User user = User.builder()
                .phone(req.getPhone())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role("ADMIN")
                .fullName(req.getFullName())
                .email(req.getEmail())
                .build();
        userRepo.save(user);

        return buildLoginResponse(user);
    }

    public LoginResponse login(LoginRequest req) {
        // req.getUsername() contains phone or email from the client
        String identifier = req.getUsername();

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
}
