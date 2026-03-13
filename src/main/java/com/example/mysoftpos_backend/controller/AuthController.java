package com.example.mysoftpos_backend.controller;

import com.example.mysoftpos_backend.dto.*;
import com.example.mysoftpos_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            return ResponseEntity.ok(authService.login(req));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");
            return ResponseEntity.ok(authService.refreshToken(refreshToken));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password/request-code")
    public ResponseEntity<?> requestForgotPasswordCode(@Valid @RequestBody ForgotPasswordRequest req) {
        try {
            return ResponseEntity.ok(authService.requestForgotPasswordCode(req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password/verify-code")
    public ResponseEntity<?> verifyForgotPasswordCode(@Valid @RequestBody ForgotPasswordVerifyCodeRequest req) {
        try {
            return ResponseEntity.ok(authService.verifyForgotPasswordCode(req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetForgotPassword(@Valid @RequestBody ForgotPasswordResetRequest req) {
        try {
            return ResponseEntity.ok(authService.resetForgotPassword(req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
