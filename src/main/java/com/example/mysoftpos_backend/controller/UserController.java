package com.example.mysoftpos_backend.controller;

import com.example.mysoftpos_backend.dto.CreateUserRequest;
import com.example.mysoftpos_backend.dto.CreatePosAccountRequest;
import com.example.mysoftpos_backend.dto.PosAccountDto;
import com.example.mysoftpos_backend.dto.UserDto;
import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.service.PosAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Deprecated
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private static final String LEGACY_USERS_SUNSET_TARGET = "Release+2 (planned removal of /api/users compatibility endpoint)";
    private static final String LEGACY_USERS_SUNSET_RFC_1123 = "Wed, 30 Sep 2026 23:59:59 GMT";

    private final PosAccountService posAccountService;

    @GetMapping
    public ResponseEntity<List<UserDto>> getUsers(@AuthenticationPrincipal PosAccount admin) {
        logLegacyDeprecation("GET /api/users", admin.getId());
        return withDeprecationHeaders(HttpStatus.OK,
                posAccountService.getPosAccountsByAdmin(admin.getId())
                        .stream()
                        .map(this::toUserDto)
                        .toList());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@AuthenticationPrincipal PosAccount admin,
                                        @Valid @RequestBody CreateUserRequest req) {
        try {
            logLegacyDeprecation("POST /api/users", admin.getId());
            return withDeprecationHeaders(HttpStatus.CREATED,
                    toUserDto(posAccountService.createPosAccount(admin.getId(), toCreatePosAccountRequest(req))));
        } catch (RuntimeException e) {
            return withDeprecationHeaders(HttpStatus.BAD_REQUEST, Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@AuthenticationPrincipal PosAccount admin,
                                        @PathVariable Long id,
                                        @Valid @RequestBody CreateUserRequest req) {
        try {
            logLegacyDeprecation("PUT /api/users/{id}", admin.getId());
            return withDeprecationHeaders(HttpStatus.OK,
                    toUserDto(posAccountService.updatePosAccount(admin.getId(), id,
                            toCreatePosAccountRequest(req))));
        } catch (RuntimeException e) {
            return withDeprecationHeaders(HttpStatus.BAD_REQUEST, Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal PosAccount admin,
                                        @PathVariable Long id) {
        try {
            logLegacyDeprecation("DELETE /api/users/{id}", admin.getId());
            posAccountService.deletePosAccount(admin.getId(), id);
            return withDeprecationHeaders(HttpStatus.OK, Map.of("message", "User deleted"));
        } catch (RuntimeException e) {
            return withDeprecationHeaders(HttpStatus.BAD_REQUEST, Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@AuthenticationPrincipal PosAccount admin,
                                           @PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        try {
            logLegacyDeprecation("PUT /api/users/{id}/reset-password", admin.getId());
            posAccountService.resetPosAccountPassword(admin.getId(), id, body.get("newPassword"));
            return withDeprecationHeaders(HttpStatus.OK, Map.of("message", "Password reset successfully"));
        } catch (RuntimeException e) {
            return withDeprecationHeaders(HttpStatus.BAD_REQUEST, Map.of("error", e.getMessage()));
        }
    }

    private void logLegacyDeprecation(String route, Long adminId) {
        log.warn("[DEPRECATED] {} called by adminId={}. Use /api/pos-accounts instead. Sunset target: {}",
                route, adminId, LEGACY_USERS_SUNSET_TARGET);
    }

    private HttpHeaders buildDeprecationHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Deprecation", "true");
        headers.add("Sunset", LEGACY_USERS_SUNSET_RFC_1123);
        headers.add("Link", "</api/pos-accounts>; rel=\"successor-version\"");
        return headers;
    }

    private <T> ResponseEntity<T> withDeprecationHeaders(HttpStatus status, T body) {
        return ResponseEntity.status(status)
                .headers(buildDeprecationHeaders())
                .body(body);
    }

    private CreatePosAccountRequest toCreatePosAccountRequest(CreateUserRequest req) {
        if (req == null) {
            return null;
        }
        return new CreatePosAccountRequest(
                req.getPhone(),
                req.getPassword(),
                req.getFullName(),
                req.getEmail(),
                req.getDob(),
                req.getGender(),
                req.getStoreName(),
                req.getBankName(),
                req.getBusinessType(),
                req.getStoreAddress(),
                req.getMerchantId(),
                req.getBranchId(),
                req.getTerminalId(),
                req.getServerIp(),
                req.getServerPort());
    }

    private UserDto toUserDto(PosAccountDto dto) {
        if (dto == null) {
            return null;
        }
        return UserDto.builder()
                .id(dto.getId())
                .merchantId(dto.getMerchantId())
                .branchId(dto.getBranchId())
                .branchCode(dto.getBranchCode())
                .branchName(dto.getBranchName())
                .merchantCode(dto.getMerchantCode())
                .role(dto.getRole())
                .fullName(dto.getFullName())
                .username(dto.getUsername())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .dob(dto.getDob())
                .gender(dto.getGender())
                .storeName(dto.getStoreName())
                .bankName(dto.getBankName())
                .businessType(dto.getBusinessType())
                .storeAddress(dto.getStoreAddress())
                .phoneVerified(dto.getPhoneVerified())
                .terminalId(dto.getTerminalId())
                .active(dto.isActive())
                .online(dto.isOnline())
                .build();
    }
}
