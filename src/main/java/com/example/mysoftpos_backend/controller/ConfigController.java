package com.example.mysoftpos_backend.controller;

import com.example.mysoftpos_backend.dto.MerchantDto;
import com.example.mysoftpos_backend.dto.TerminalDto;
import com.example.mysoftpos_backend.dto.PosAccountDto;
import com.example.mysoftpos_backend.entity.Branch;
import com.example.mysoftpos_backend.entity.Merchant;
import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.entity.Terminal;
import com.example.mysoftpos_backend.repository.BranchRepository;
import com.example.mysoftpos_backend.repository.MerchantRepository;
import com.example.mysoftpos_backend.repository.TerminalRepository;
import com.example.mysoftpos_backend.repository.PosAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConfigController {

    private static final java.util.regex.Pattern BANK_NAME_PATTERN = java.util.regex.Pattern.compile("^[A-Z0-9]{2,22}$");

    private final MerchantRepository merchantRepo;
    private final TerminalRepository terminalRepo;
    private final PosAccountRepository userRepo;
    private final BranchRepository branchRepo;

    // ==================== Merchants ====================

    @GetMapping("/merchants")
    public ResponseEntity<List<MerchantDto>> getMerchants(@AuthenticationPrincipal PosAccount admin) {
        return ResponseEntity.ok(merchantRepo.findByAdminId(admin.getId()).stream()
                .map(this::toMerchantDto).collect(Collectors.toList()));
    }

    @PostMapping("/merchants")
    public ResponseEntity<?> createMerchant(@AuthenticationPrincipal PosAccount admin,
                                            @RequestBody Map<String, String> body) {
        if (merchantRepo.existsByMerchantCode(body.get("merchantCode"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Merchant code already exists"));
        }
        final String bankName;
        try {
            bankName = normalizeBankName(body.get("bankName"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
        Merchant m = Merchant.builder()
                .merchantCode(body.get("merchantCode"))
                .merchantName(body.get("merchantName"))
                .bankName(bankName)
                .adminId(admin.getId())
                .build();
        merchantRepo.save(m);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMerchantDto(m));
    }

    @PutMapping("/merchants/{id}")
    public ResponseEntity<?> updateMerchant(@AuthenticationPrincipal PosAccount admin,
                                            @PathVariable Long id,
                                            @RequestBody Map<String, String> body) {
        Merchant m = merchantRepo.findById(id).orElse(null);
        if (m == null || !Objects.equals(m.getAdminId(), admin.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not found or access denied"));
        }
        if (body.containsKey("merchantName")) m.setMerchantName(body.get("merchantName"));
        if (body.containsKey("bankName")) {
            try {
                m.setBankName(normalizeBankName(body.get("bankName")));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
            }
        }
        if (body.containsKey("businessType")) m.setBusinessType(body.get("businessType"));
        if (body.containsKey("storeAddress")) m.setStoreAddress(body.get("storeAddress"));
        merchantRepo.save(m);
        return ResponseEntity.ok(toMerchantDto(m));
    }

    @DeleteMapping("/merchants/{id}")
    @Transactional
    public ResponseEntity<?> deleteMerchant(@AuthenticationPrincipal PosAccount admin,
                                            @PathVariable Long id) {
        Merchant merchant = merchantRepo.findById(id).orElse(null);
        if (merchant == null || !Objects.equals(merchant.getAdminId(), admin.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not found or access denied"));
        }

        // Cascade remove merchant-owned config so admin can delete merchant from UI directly.
        terminalRepo.deleteByMerchantId(id);
        branchRepo.deleteByMerchantId(id);
        if (merchant.getOwnerUserId() != null) {
            merchant.setOwnerUserId(null);
            merchantRepo.save(merchant);
        }
        userRepo.deleteByAdminIdAndMerchantId(admin.getId(), id);
        merchantRepo.delete(merchant);
        return ResponseEntity.ok(Map.of("message", "Merchant deleted"));
    }

    @GetMapping("/merchants/{id}/accounts")
    public ResponseEntity<?> getMerchantAccounts(@AuthenticationPrincipal PosAccount admin,
                                                 @PathVariable Long id,
                                                 @RequestParam(name = "branchId", required = false) Long branchId) {
        Merchant merchant = merchantRepo.findById(id).orElse(null);
        if (merchant == null || !Objects.equals(merchant.getAdminId(), admin.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not found or access denied"));
        }

        List<PosAccount> users = branchId != null
                ? userRepo.findByAdminIdAndMerchantIdAndBranchIdOrderByIdAsc(admin.getId(), id, branchId)
                : userRepo.findByAdminIdAndMerchantIdOrderByIdAsc(admin.getId(), id);

        return ResponseEntity.ok(users.stream()
                .map(this::toPosAccountDto)
                .collect(Collectors.toList()));
    }

    // ==================== Terminals ====================

    @GetMapping("/terminals")
    public ResponseEntity<List<TerminalDto>> getTerminals(@AuthenticationPrincipal PosAccount admin) {
        return ResponseEntity.ok(terminalRepo.findByMerchantAdminId(admin.getId()).stream()
                .map(this::toTerminalDto).collect(Collectors.toList()));
    }

    @PostMapping("/terminals")
    public ResponseEntity<?> createTerminal(@AuthenticationPrincipal PosAccount admin,
                                            @RequestBody Map<String, String> body) {
        if (terminalRepo.existsByTerminalCode(body.get("terminalCode"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Terminal code already exists"));
        }
        Merchant merchant = merchantRepo.findById(Long.parseLong(body.get("merchantId"))).orElse(null);
        if (merchant == null || !Objects.equals(merchant.getAdminId(), admin.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Merchant not found or access denied"));
        }
        Terminal t = Terminal.builder()
                .terminalCode(body.get("terminalCode"))
                .merchant(merchant)
                .serverIp(body.get("serverIp"))
                .serverPort(body.containsKey("serverPort") ? Integer.parseInt(body.get("serverPort")) : null)
                .branchId(parseLong(body.get("branchId")))
                .posAccountId(parseLong(body.get("posAccountId")))
                .build();
        if (t.getBranchId() != null) {
            Branch branch = branchRepo.findById(t.getBranchId()).orElse(null);
            if (branch == null || !Objects.equals(branch.getMerchantId(), merchant.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Branch not found or invalid"));
            }
        }
        if (t.getPosAccountId() != null) {
            PosAccount account = userRepo.findById(t.getPosAccountId()).orElse(null);
            if (account == null || !Objects.equals(account.getMerchantId(), merchant.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Pos account not found or invalid"));
            }
        }
        terminalRepo.save(t);
        return ResponseEntity.status(HttpStatus.CREATED).body(toTerminalDto(t));
    }

    @PutMapping("/terminals/{id}")
    public ResponseEntity<?> updateTerminal(@AuthenticationPrincipal PosAccount admin,
                                            @PathVariable Long id,
                                            @RequestBody Map<String, String> body) {
        Terminal t = terminalRepo.findById(id).orElse(null);
        if (t == null || !Objects.equals(t.getMerchant().getAdminId(), admin.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not found or access denied"));
        }
        if (body.containsKey("serverIp")) t.setServerIp(body.get("serverIp"));
        if (body.containsKey("serverPort")) t.setServerPort(Integer.parseInt(body.get("serverPort")));
        if (body.containsKey("branchId")) {
            Long branchId = parseLong(body.get("branchId"));
            if (branchId != null) {
                Branch branch = branchRepo.findById(branchId).orElse(null);
                if (branch == null || !Objects.equals(branch.getMerchantId(), t.getMerchant().getId())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Branch not found or invalid"));
                }
            }
            t.setBranchId(branchId);
        }
        if (body.containsKey("posAccountId")) {
            Long posAccountId = parseLong(body.get("posAccountId"));
            if (posAccountId != null) {
                PosAccount account = userRepo.findById(posAccountId).orElse(null);
                if (account == null || !Objects.equals(account.getMerchantId(), t.getMerchant().getId())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Pos account not found or invalid"));
                }
            }
            t.setPosAccountId(posAccountId);
        }
        terminalRepo.save(t);
        return ResponseEntity.ok(toTerminalDto(t));
    }

    @DeleteMapping("/terminals/{id}")
    public ResponseEntity<?> deleteTerminal(@AuthenticationPrincipal PosAccount admin,
                                            @PathVariable Long id) {
        Terminal terminal = terminalRepo.findById(id).orElse(null);
        if (terminal == null || terminal.getMerchant() == null
                || !Objects.equals(terminal.getMerchant().getAdminId(), admin.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not found or access denied"));
        }
        terminalRepo.delete(terminal);
        return ResponseEntity.ok(Map.of("message", "Terminal deleted"));
    }

    // ==================== DTO Mappers ====================

    private MerchantDto toMerchantDto(Merchant m) {
        return MerchantDto.builder()
                .id(m.getId())
                .merchantCode(m.getMerchantCode())
                .merchantName(m.getMerchantName())
                .fullName(m.getFullName())
                .phone(m.getPhone())
                .email(m.getEmail())
                .dob(m.getDob())
                .gender(m.getGender())
                .bankName(m.getBankName())
                .adminId(m.getAdminId())
                .ownerUserId(m.getOwnerUserId())
                .businessType(m.getBusinessType())
                .storeAddress(m.getStoreAddress())
                .build();
    }

    private PosAccountDto toPosAccountDto(PosAccount user) {
        Merchant merchant = user.getMerchantId() != null
                ? merchantRepo.findById(user.getMerchantId()).orElse(null)
                : merchantRepo.findByOwnerUserId(user.getId()).orElse(null);
        Long merchantId = user.getMerchantId() != null
                ? user.getMerchantId()
                : (merchant != null ? merchant.getId() : null);
        Branch branch = user.getBranchId() != null ? branchRepo.findById(user.getBranchId()).orElse(null) : null;
        Terminal terminal = terminalRepo.findFirstByPosAccountId(user.getId()).orElse(null);
        if (terminal == null && user.getTerminalId() != null && !user.getTerminalId().isBlank()) {
            terminal = terminalRepo.findByTerminalCode(user.getTerminalId().trim().toUpperCase(java.util.Locale.ROOT))
                    .orElse(null);
        }

        return PosAccountDto.builder()
                .id(user.getId())
                .merchantId(merchantId)
                .branchId(user.getBranchId())
                .branchCode(branch != null ? branch.getBranchCode() : null)
                .branchName(branch != null ? branch.getBranchName() : null)
                .merchantCode(merchant != null ? merchant.getMerchantCode() : null)
                .role(user.getRole())
                .fullName(merchant != null ? merchant.getFullName() : null)
                .username(user.getUsername())
                .phone(user.getUsername())
                .merchantPhone(merchant != null ? merchant.getPhone() : null)
                .email(merchant != null ? merchant.getEmail() : null)
                .dob(merchant != null ? merchant.getDob() : null)
                .gender(merchant != null ? merchant.getGender() : null)
                .storeName(merchant != null ? merchant.getMerchantName() : null)
                .bankName(merchant != null ? merchant.getBankName() : null)
                .businessType(merchant != null ? merchant.getBusinessType() : null)
                .storeAddress(merchant != null ? merchant.getStoreAddress() : null)
                .phoneVerified(user.isPhoneVerified())
                .terminalId(user.getTerminalId())
                .serverIp(terminal != null ? terminal.getServerIp() : null)
                .serverPort(terminal != null ? terminal.getServerPort() : null)
                .active(user.isActive())
                .build();
    }

    private TerminalDto toTerminalDto(Terminal t) {
        return TerminalDto.builder()
                .id(t.getId())
                .terminalCode(t.getTerminalCode())
                .merchant(t.getMerchant() != null ? toMerchantDto(t.getMerchant()) : null)
                .branchId(t.getBranchId())
                .posAccountId(t.getPosAccountId())
                .serverIp(t.getServerIp())
                .serverPort(t.getServerPort())
                .build();
    }

    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Long.parseLong(value.trim());
    }

    private String normalizeBankName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        if (!BANK_NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Bank name must be 2-22 characters using A-Z and 0-9 only");
        }
        return normalized;
    }
}
