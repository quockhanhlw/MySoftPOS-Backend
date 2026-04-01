package com.example.mysoftpos_backend.controller;

import com.example.mysoftpos_backend.dto.CreatePosAccountRequest;
import com.example.mysoftpos_backend.dto.PosAccountConnectionRequest;
import com.example.mysoftpos_backend.dto.PosAccountDto;
import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.service.PosAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pos-accounts")
@RequiredArgsConstructor
public class PosAccountController {

    private final PosAccountService posAccountService;

    @GetMapping
    public ResponseEntity<List<PosAccountDto>> getPosAccounts(@AuthenticationPrincipal PosAccount admin) {
        return ResponseEntity.ok(posAccountService.getPosAccountsByAdmin(admin.getId()));
    }

    @PostMapping
    public ResponseEntity<?> createPosAccount(@AuthenticationPrincipal PosAccount admin,
                                               @Valid @RequestBody CreatePosAccountRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(posAccountService.createPosAccount(admin.getId(), req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePosAccount(@AuthenticationPrincipal PosAccount admin,
                                              @PathVariable Long id,
                                               @Valid @RequestBody CreatePosAccountRequest req) {
        try {
            return ResponseEntity.ok(posAccountService.updatePosAccount(admin.getId(), id, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/connection")
    public ResponseEntity<?> updatePosAccountConnection(@AuthenticationPrincipal PosAccount admin,
                                                        @PathVariable Long id,
                                                        @RequestBody PosAccountConnectionRequest req) {
        try {
            return ResponseEntity.ok(posAccountService.updatePosAccountConnection(admin.getId(), id, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePosAccount(@AuthenticationPrincipal PosAccount admin,
                                              @PathVariable Long id) {
        try {
            posAccountService.deletePosAccount(admin.getId(), id);
            return ResponseEntity.ok(Map.of("message", "Pos account deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPosAccountPassword(@AuthenticationPrincipal PosAccount admin,
                                                     @PathVariable Long id,
                                                     @RequestBody Map<String, String> body) {
        try {
            posAccountService.resetPosAccountPassword(admin.getId(), id, body.get("newPassword"));
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

