package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.CreateUserRequest;
import com.example.mysoftpos_backend.dto.UserDto;
import com.example.mysoftpos_backend.entity.User;
import com.example.mysoftpos_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public List<UserDto> getUsersByAdmin(Long adminId) {
        return userRepo.findByAdminId(adminId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public UserDto createUser(Long adminId, CreateUserRequest req) {
        String phone = normalizePhone(req.getPhone());
        String email = normalizeEmail(req.getEmail());
        String fullName = normalizeText(req.getFullName());

        if (userRepo.existsByPhone(phone)) {
            throw new RuntimeException("Phone number already registered");
        }
        if (email != null && userRepo.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new RuntimeException("Password is required for new user");
        }

        User user = User.builder()
                .phone(phone)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role("USER")
                .fullName(fullName)
                .email(email)
                .terminalId(normalizeText(req.getTerminalId()))
                .serverIp(normalizeText(req.getServerIp()))
                .serverPort(req.getServerPort())
                .adminId(adminId)
                .build();
        userRepo.save(user);
        return toDto(user);
    }

    public UserDto updateUser(Long adminId, Long userId, CreateUserRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!adminId.equals(user.getAdminId())) {
            throw new RuntimeException("Access denied");
        }

        if (req.getFullName() != null)
            user.setFullName(normalizeText(req.getFullName()));
        if (req.getPhone() != null) {
            String phone = normalizePhone(req.getPhone());
            if (!phone.equals(user.getPhone()) && userRepo.existsByPhone(phone)) {
                throw new RuntimeException("Phone number already registered");
            }
            user.setPhone(phone);
        }
        if (req.getEmail() != null) {
            String email = normalizeEmail(req.getEmail());
            String currentEmail = normalizeEmail(user.getEmail());
            if (email != null && !email.equals(currentEmail) && userRepo.existsByEmail(email)) {
                throw new RuntimeException("Email already registered");
            }
            user.setEmail(email);
        }
        if (req.getTerminalId() != null)
            user.setTerminalId(normalizeText(req.getTerminalId()));
        if (req.getServerIp() != null)
            user.setServerIp(normalizeText(req.getServerIp()));
        if (req.getServerPort() != null)
            user.setServerPort(req.getServerPort());
        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }
        userRepo.save(user);
        return toDto(user);
    }

    public void deleteUser(Long adminId, Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!adminId.equals(user.getAdminId())) {
            throw new RuntimeException("Access denied");
        }
        userRepo.delete(user);
    }

    public void resetPassword(Long adminId, Long userId, String newPassword) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!adminId.equals(user.getAdminId())) {
            throw new RuntimeException("Access denied");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepo.save(user);
    }

    private UserDto toDto(User u) {
        boolean isOnline = u.getLastActiveAt() != null &&
                u.getLastActiveAt().isAfter(java.time.LocalDateTime.now().minusMinutes(5));

        return UserDto.builder()
                .id(u.getId())
                .role(u.getRole())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .email(u.getEmail())
                .terminalId(u.getTerminalId())
                .serverIp(u.getServerIp())
                .serverPort(u.getServerPort())
                .active(u.isActive())
                .online(isOnline)
                .build();
    }

    private String normalizePhone(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
