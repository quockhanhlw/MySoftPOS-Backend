package com.example.mysoftpos_backend.controller;

import com.example.mysoftpos_backend.dto.MerchantDto;
import com.example.mysoftpos_backend.dto.TerminalDto;
import com.example.mysoftpos_backend.entity.Merchant;
import com.example.mysoftpos_backend.entity.Terminal;
import com.example.mysoftpos_backend.entity.User;
import com.example.mysoftpos_backend.repository.MerchantRepository;
import com.example.mysoftpos_backend.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConfigController {

    private final MerchantRepository merchantRepo;
    private final TerminalRepository terminalRepo;

    // ==================== Merchants ====================

    @GetMapping("/merchants")
    public ResponseEntity<List<MerchantDto>> getMerchants(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(merchantRepo.findByAdminId(admin.getId()).stream()
                .map(this::toMerchantDto).collect(Collectors.toList()));
    }

    @PostMapping("/merchants")
    public ResponseEntity<?> createMerchant(@AuthenticationPrincipal User admin,
                                            @RequestBody Map<String, String> body) {
        if (merchantRepo.existsByMerchantCode(body.get("merchantCode"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Merchant code already exists"));
        }
        Merchant m = Merchant.builder()
                .merchantCode(body.get("merchantCode"))
                .merchantName(body.get("merchantName"))
                .adminId(admin.getId())
                .build();
        merchantRepo.save(m);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMerchantDto(m));
    }

    @PutMapping("/merchants/{id}")
    public ResponseEntity<?> updateMerchant(@AuthenticationPrincipal User admin,
                                            @PathVariable Long id,
                                            @RequestBody Map<String, String> body) {
        Merchant m = merchantRepo.findById(id).orElse(null);
        if (m == null || !Objects.equals(m.getAdminId(), admin.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not found or access denied"));
        }
        if (body.containsKey("merchantName")) m.setMerchantName(body.get("merchantName"));
        if (body.containsKey("businessType")) m.setBusinessType(body.get("businessType"));
        if (body.containsKey("storeAddress")) m.setStoreAddress(body.get("storeAddress"));
        merchantRepo.save(m);
        return ResponseEntity.ok(toMerchantDto(m));
    }

    // ==================== Terminals ====================

    @GetMapping("/terminals")
    public ResponseEntity<List<TerminalDto>> getTerminals(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(terminalRepo.findByMerchantAdminId(admin.getId()).stream()
                .map(this::toTerminalDto).collect(Collectors.toList()));
    }

    @PostMapping("/terminals")
    public ResponseEntity<?> createTerminal(@AuthenticationPrincipal User admin,
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
                .build();
        terminalRepo.save(t);
        return ResponseEntity.status(HttpStatus.CREATED).body(toTerminalDto(t));
    }

    @PutMapping("/terminals/{id}")
    public ResponseEntity<?> updateTerminal(@AuthenticationPrincipal User admin,
                                            @PathVariable Long id,
                                            @RequestBody Map<String, String> body) {
        Terminal t = terminalRepo.findById(id).orElse(null);
        if (t == null || !Objects.equals(t.getMerchant().getAdminId(), admin.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not found or access denied"));
        }
        if (body.containsKey("serverIp")) t.setServerIp(body.get("serverIp"));
        if (body.containsKey("serverPort")) t.setServerPort(Integer.parseInt(body.get("serverPort")));
        terminalRepo.save(t);
        return ResponseEntity.ok(toTerminalDto(t));
    }

    // ==================== DTO Mappers ====================

    private MerchantDto toMerchantDto(Merchant m) {
        return MerchantDto.builder()
                .id(m.getId())
                .merchantCode(m.getMerchantCode())
                .merchantName(m.getMerchantName())
                .adminId(m.getAdminId())
                .ownerUserId(m.getOwnerUserId())
                .businessType(m.getBusinessType())
                .storeAddress(m.getStoreAddress())
                .build();
    }

    private TerminalDto toTerminalDto(Terminal t) {
        return TerminalDto.builder()
                .id(t.getId())
                .terminalCode(t.getTerminalCode())
                .merchant(t.getMerchant() != null ? toMerchantDto(t.getMerchant()) : null)
                .serverIp(t.getServerIp())
                .serverPort(t.getServerPort())
                .build();
    }
}
