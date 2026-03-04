package com.example.mysoftpos_backend.controller;

import com.example.mysoftpos_backend.dto.CreateUserRequest;
import com.example.mysoftpos_backend.dto.UserDto;
import com.example.mysoftpos_backend.entity.User;
import com.example.mysoftpos_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDto>> getUsers(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(userService.getUsersByAdmin(admin.getId()));
    }

    @PostMapping
    public ResponseEntity<?> createUser(@AuthenticationPrincipal User admin,
                                        @Valid @RequestBody CreateUserRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(userService.createUser(admin.getId(), req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@AuthenticationPrincipal User admin,
                                        @PathVariable Long id,
                                        @Valid @RequestBody CreateUserRequest req) {
        try {
            return ResponseEntity.ok(userService.updateUser(admin.getId(), id, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal User admin,
                                        @PathVariable Long id) {
        try {
            userService.deleteUser(admin.getId(), id);
            return ResponseEntity.ok(Map.of("message", "User deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@AuthenticationPrincipal User admin,
                                           @PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        try {
            userService.resetPassword(admin.getId(), id, body.get("newPassword"));
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
