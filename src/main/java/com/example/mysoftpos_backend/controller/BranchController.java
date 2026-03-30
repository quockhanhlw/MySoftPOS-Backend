package com.example.mysoftpos_backend.controller;

import com.example.mysoftpos_backend.dto.BranchDto;
import com.example.mysoftpos_backend.dto.CreateBranchRequest;
import com.example.mysoftpos_backend.dto.PosAccountDto;
import com.example.mysoftpos_backend.dto.UserDto;
import com.example.mysoftpos_backend.entity.Branch;
import com.example.mysoftpos_backend.entity.Merchant;
import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.repository.BranchRepository;
import com.example.mysoftpos_backend.repository.MerchantRepository;
import com.example.mysoftpos_backend.repository.PosAccountRepository;
import com.example.mysoftpos_backend.service.PosAccountServiceCore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/merchants/{merchantId}/branches")
@RequiredArgsConstructor
public class BranchController {

    private static final String DEFAULT_BRANCH_CODE = "MAIN";

    private final MerchantRepository merchantRepo;
    private final BranchRepository branchRepo;
    private final PosAccountRepository userRepo;
    private final PosAccountServiceCore posAccountServiceCore;

    @GetMapping
    public ResponseEntity<?> getBranches(@AuthenticationPrincipal PosAccount admin,
                                         @PathVariable Long merchantId) {
        Merchant merchant = merchantRepo.findById(merchantId).orElse(null);
        if (merchant == null || !Objects.equals(merchant.getAdminId(), admin.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not found or access denied"));
        }

        ensureMainBranch(merchant);
        List<BranchDto> branches = branchRepo.findByMerchantIdOrderByIdAsc(merchantId).stream()
                .map(this::toBranchDto)
                .toList();
        return ResponseEntity.ok(branches);
    }

    @PostMapping
    public ResponseEntity<?> createBranch(@AuthenticationPrincipal PosAccount admin,
                                          @PathVariable Long merchantId,
                                          @Valid @RequestBody CreateBranchRequest req) {
        Merchant merchant = merchantRepo.findById(merchantId).orElse(null);
        if (merchant == null || !Objects.equals(merchant.getAdminId(), admin.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not found or access denied"));
        }

        String code = normalizeCode(req.getBranchCode());
        if (code.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Branch code is required"));
        }
        if (branchRepo.existsByMerchantIdAndBranchCode(merchantId, code)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Branch code already exists"));
        }

        Branch branch = Branch.builder()
                .merchantId(merchantId)
                .branchCode(code)
                .branchName(normalizeText(req.getBranchName()))
                .branchAddress(normalizeText(req.getBranchAddress()))
                .build();
        branch = branchRepo.save(branch);
        recalculateMerchantBranchCount(merchant);
        return ResponseEntity.status(HttpStatus.CREATED).body(toBranchDto(branch));
    }

    @PutMapping("/{branchId}")
    public ResponseEntity<?> updateBranch(@AuthenticationPrincipal PosAccount admin,
                                          @PathVariable Long merchantId,
                                          @PathVariable Long branchId,
                                          @Valid @RequestBody CreateBranchRequest req) {
        Merchant merchant = merchantRepo.findById(merchantId).orElse(null);
        if (merchant == null || !Objects.equals(merchant.getAdminId(), admin.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not found or access denied"));
        }

        Branch branch = branchRepo.findById(branchId).orElse(null);
        if (branch == null || !Objects.equals(branch.getMerchantId(), merchantId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Branch not found"));
        }

        String code = normalizeCode(req.getBranchCode());
        if (!code.isEmpty() && !code.equals(branch.getBranchCode())
                && branchRepo.existsByMerchantIdAndBranchCode(merchantId, code)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Branch code already exists"));
        }

        if (!code.isEmpty()) {
            branch.setBranchCode(code);
        }
        branch.setBranchName(normalizeText(req.getBranchName()));
        branch.setBranchAddress(normalizeText(req.getBranchAddress()));
        branch = branchRepo.save(branch);
        return ResponseEntity.ok(toBranchDto(branch));
    }

    @GetMapping("/{branchId}/accounts")
    public ResponseEntity<?> getBranchAccounts(@AuthenticationPrincipal PosAccount admin,
                                               @PathVariable Long merchantId,
                                               @PathVariable Long branchId) {
        Merchant merchant = merchantRepo.findById(merchantId).orElse(null);
        if (merchant == null || !Objects.equals(merchant.getAdminId(), admin.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not found or access denied"));
        }

        Branch branch = branchRepo.findById(branchId).orElse(null);
        if (branch == null || !Objects.equals(branch.getMerchantId(), merchantId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Branch not found"));
        }

        List<UserDto> accounts = userRepo
                .findByAdminIdAndMerchantIdAndBranchIdOrderByIdAsc(admin.getId(), merchantId, branchId)
                .stream()
                .map(posAccountServiceCore::toPosAccountDto)
                .map(this::toUserDto)
                .toList();
        return ResponseEntity.ok(accounts);
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

    private Branch ensureMainBranch(Merchant merchant) {
        return branchRepo.findByMerchantIdAndBranchCode(merchant.getId(), DEFAULT_BRANCH_CODE)
                .orElseGet(() -> {
                    Branch main = Branch.builder()
                            .merchantId(merchant.getId())
                            .branchCode(DEFAULT_BRANCH_CODE)
                            .branchName(normalizeText(merchant.getMerchantName()))
                            .branchAddress(normalizeText(merchant.getStoreAddress()))
                            .build();
                    main = branchRepo.save(main);
                    recalculateMerchantBranchCount(merchant);
                    return main;
                });
    }

    private void recalculateMerchantBranchCount(Merchant merchant) {
        // branch count is derived dynamically; no denormalized counter persisted.
    }

    private BranchDto toBranchDto(Branch branch) {
        int accountCount = userRepo.findByMerchantIdAndBranchId(branch.getMerchantId(), branch.getId()).size();
        return BranchDto.builder()
                .id(branch.getId())
                .merchantId(branch.getMerchantId())
                .branchCode(branch.getBranchCode())
                .branchName(branch.getBranchName())
                .branchAddress(branch.getBranchAddress())
                .accountCount(accountCount)
                .build();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeCode(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? "" : normalized.toUpperCase(java.util.Locale.ROOT);
    }
}

