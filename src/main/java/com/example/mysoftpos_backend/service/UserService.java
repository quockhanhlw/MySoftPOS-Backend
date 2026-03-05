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
        if (userRepo.existsByPhone(req.getPhone())) {
            throw new RuntimeException("Phone number already registered");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new RuntimeException("Password is required for new user");
        }

        User user = User.builder()
                .phone(req.getPhone())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role("USER")
                .fullName(req.getFullName())
                .email(req.getEmail())
                .terminalId(req.getTerminalId())
                .serverIp(req.getServerIp())
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

        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getPhone() != null) {
            // Check phone uniqueness if changed
            if (!req.getPhone().equals(user.getPhone()) && userRepo.existsByPhone(req.getPhone())) {
                throw new RuntimeException("Phone number already registered");
            }
            user.setPhone(req.getPhone());
        }
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getTerminalId() != null) user.setTerminalId(req.getTerminalId());
        if (req.getServerIp() != null) user.setServerIp(req.getServerIp());
        if (req.getServerPort() != null) user.setServerPort(req.getServerPort());
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
                .build();
    }
}
