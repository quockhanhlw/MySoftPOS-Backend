package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.CreatePosAccountRequest;
import com.example.mysoftpos_backend.dto.PosAccountDto;
import com.example.mysoftpos_backend.entity.Branch;
import com.example.mysoftpos_backend.entity.Merchant;
import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.repository.BranchRepository;
import com.example.mysoftpos_backend.repository.MerchantRepository;
import com.example.mysoftpos_backend.repository.PosAccountRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PosAccountServiceCore {

    private static final java.util.regex.Pattern TID_PATTERN = java.util.regex.Pattern.compile("^[A-Z0-9]{8}$");
    private static final java.util.regex.Pattern BANK_NAME_PATTERN = java.util.regex.Pattern.compile("^[A-Z0-9]{2,22}$");
    private static final String DEFAULT_BRANCH_CODE = "MAIN";

    private final PosAccountRepository posAccountRepo;
    private final MerchantRepository merchantRepo;
    private final BranchRepository branchRepo;
    private final PasswordEncoder passwordEncoder;

    public PosAccountServiceCore(@Qualifier("posAccountRepository") PosAccountRepository posAccountRepo,
                                 MerchantRepository merchantRepo,
                                 BranchRepository branchRepo,
                                 PasswordEncoder passwordEncoder) {
        this.posAccountRepo = posAccountRepo;
        this.merchantRepo = merchantRepo;
        this.branchRepo = branchRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public List<PosAccountDto> getPosAccountsByAdmin(Long adminId) {
        return posAccountRepo.findByAdminId(adminId).stream()
                .map(this::toPosAccountDto)
                .collect(Collectors.toList());
    }

    public PosAccountDto createPosAccount(Long adminId, CreatePosAccountRequest req) {
        String username = normalizeUsername(req.getPhone());
        String merchantPhone = normalizePhone(req.getPhone());
        Merchant targetMerchant = null;
        Long branchId = null;

        if (username == null || username.isBlank()) {
            throw new RuntimeException("Username is required");
        }

        if (req.getMerchantId() != null) {
            targetMerchant = merchantRepo.findById(req.getMerchantId())
                    .orElseThrow(() -> new RuntimeException("Merchant not found"));
            if (!adminId.equals(targetMerchant.getAdminId())) {
                throw new RuntimeException("Access denied");
            }
            branchId = resolveBranchIdForRequest(targetMerchant, req.getBranchId());
        }

        if (posAccountRepo.existsByUsername(username)) {
            throw new RuntimeException("Username already registered");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new RuntimeException("Password is required for new user");
        }
        validateTerminalId(req.getTerminalId());

        PosAccount posAccount = PosAccount.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role("USER")
                .phoneVerified(true)
                .terminalId(normalizeTerminalId(req.getTerminalId()))
                .adminId(adminId)
                .merchantId(targetMerchant != null ? targetMerchant.getId() : null)
                .branchId(branchId)
                .build();
        posAccount = posAccountRepo.save(posAccount);

        if (targetMerchant == null) {
            Merchant merchant = Merchant.builder()
                    .merchantCode(generateMerchantCode(posAccount.getId(), merchantPhone))
                    .merchantName(normalizeText(req.getStoreName()) != null
                            ? normalizeText(req.getStoreName())
                            : normalizeText(req.getFullName()))
                    .fullName(normalizeText(req.getFullName()))
                    .phone(merchantPhone)
                    .email(normalizeEmail(req.getEmail()))
                    .dob(normalizeText(req.getDob()))
                    .gender(normalizeText(req.getGender()))
                    .bankName(normalizeBankName(req.getBankName()))
                    .adminId(adminId)
                    .ownerUserId(posAccount.getId())
                    .businessType(normalizeBusinessType(req.getBusinessType()))
                    .storeAddress(normalizeText(req.getStoreAddress()))
                    .build();
            merchant = merchantRepo.save(merchant);
            Branch mainBranch = ensureMainBranch(merchant);
            posAccount.setMerchantId(merchant.getId());
            posAccount.setBranchId(mainBranch.getId());
            posAccount = posAccountRepo.save(posAccount);
        }

        return toPosAccountDto(posAccount);
    }

    public PosAccountDto updatePosAccount(Long adminId, Long accountId, CreatePosAccountRequest req) {
        PosAccount posAccount = posAccountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Pos account not found"));
        if (!adminId.equals(posAccount.getAdminId())) {
            throw new RuntimeException("Access denied");
        }
        validateTerminalId(req.getTerminalId());

        if (req.getPhone() != null) {
            String username = normalizeUsername(req.getPhone());
            if (!username.equals(posAccount.getUsername()) && posAccountRepo.existsByUsername(username)) {
                throw new RuntimeException("Username already registered");
            }
            posAccount.setUsername(username);
        }
        if (req.getTerminalId() != null) {
            posAccount.setTerminalId(normalizeTerminalId(req.getTerminalId()));
        }
        if (req.getMerchantId() != null) {
            Merchant targetMerchant = merchantRepo.findById(req.getMerchantId())
                    .orElseThrow(() -> new RuntimeException("Merchant not found"));
            if (!adminId.equals(targetMerchant.getAdminId())) {
                throw new RuntimeException("Access denied");
            }
            posAccount.setMerchantId(targetMerchant.getId());
            if (req.getBranchId() == null) {
                posAccount.setBranchId(resolveBranchIdForRequest(targetMerchant, null));
            }
        }
        if (req.getBranchId() != null) {
            Merchant scopeMerchant = posAccount.getMerchantId() != null
                    ? merchantRepo.findById(posAccount.getMerchantId()).orElse(null)
                    : null;
            if (scopeMerchant == null || !adminId.equals(scopeMerchant.getAdminId())) {
                throw new RuntimeException("Merchant not found or access denied");
            }
            posAccount.setBranchId(resolveBranchIdForRequest(scopeMerchant, req.getBranchId()));
        }
        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            posAccount.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }
        posAccount = posAccountRepo.save(posAccount);

        Merchant merchant = posAccount.getMerchantId() != null
                ? merchantRepo.findById(posAccount.getMerchantId()).orElse(null)
                : merchantRepo.findByOwnerUserId(posAccount.getId()).orElse(null);
        if (merchant != null) {
            boolean updateMerchantProfile = merchant.getOwnerUserId() != null
                    && merchant.getOwnerUserId().equals(posAccount.getId());

            if (updateMerchantProfile && req.getFullName() != null) {
                merchant.setFullName(normalizeText(req.getFullName()));
            }
            if (updateMerchantProfile && req.getPhone() != null) {
                String normalizedPhone = normalizePhone(req.getPhone());
                if (normalizedPhone != null
                        && !normalizedPhone.equals(merchant.getPhone())
                        && merchantRepo.existsByPhone(normalizedPhone)) {
                    throw new RuntimeException("Phone number already registered");
                }
                merchant.setPhone(normalizedPhone);
            }
            if (updateMerchantProfile && req.getEmail() != null) {
                String normalizedEmail = normalizeEmail(req.getEmail());
                if (normalizedEmail != null
                        && !normalizedEmail.equals(normalizeEmail(merchant.getEmail()))
                        && merchantRepo.existsByEmail(normalizedEmail)) {
                    throw new RuntimeException("Email already registered");
                }
                merchant.setEmail(normalizedEmail);
            }
            if (updateMerchantProfile && req.getDob() != null) {
                merchant.setDob(normalizeText(req.getDob()));
            }
            if (updateMerchantProfile && req.getGender() != null) {
                merchant.setGender(normalizeText(req.getGender()));
            }
            if (req.getStoreName() != null) {
                merchant.setMerchantName(normalizeText(req.getStoreName()));
            }
            if (req.getBankName() != null) {
                merchant.setBankName(normalizeBankName(req.getBankName()));
            }
            if (req.getBusinessType() != null) {
                merchant.setBusinessType(normalizeBusinessType(req.getBusinessType()));
            }
            if (req.getStoreAddress() != null) {
                merchant.setStoreAddress(normalizeText(req.getStoreAddress()));
            }
            merchantRepo.save(merchant);
        }
        return toPosAccountDto(posAccount);
    }

    public void deletePosAccount(Long adminId, Long accountId) {
        PosAccount posAccount = posAccountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Pos account not found"));
        if (!adminId.equals(posAccount.getAdminId())) {
            throw new RuntimeException("Access denied");
        }
        merchantRepo.deleteByOwnerUserId(posAccount.getId());
        posAccountRepo.delete(posAccount);
    }

    public void resetPosAccountPassword(Long adminId, Long accountId, String newPassword) {
        PosAccount posAccount = posAccountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Pos account not found"));
        if (!adminId.equals(posAccount.getAdminId())) {
            throw new RuntimeException("Access denied");
        }
        posAccount.setPasswordHash(passwordEncoder.encode(newPassword));
        posAccount.setFailedLoginAttempts(0);
        posAccount.setLockedUntil(null);
        posAccountRepo.save(posAccount);
    }

    public PosAccountDto toPosAccountDto(PosAccount posAccount) {
        Merchant merchant = posAccount.getMerchantId() != null
                ? merchantRepo.findById(posAccount.getMerchantId()).orElse(null)
                : merchantRepo.findByOwnerUserId(posAccount.getId()).orElse(null);
        Branch branch = posAccount.getBranchId() != null ? branchRepo.findById(posAccount.getBranchId()).orElse(null)
                : null;
        Long merchantId = posAccount.getMerchantId() != null
                ? posAccount.getMerchantId()
                : (merchant != null ? merchant.getId() : null);
        boolean isOnline = posAccount.getLastActiveAt() != null
                && posAccount.getLastActiveAt().isAfter(LocalDateTime.now().minusMinutes(5));

        return PosAccountDto.builder()
                .id(posAccount.getId())
                .merchantId(merchantId)
                .branchId(posAccount.getBranchId())
                .branchCode(branch != null ? branch.getBranchCode() : null)
                .branchName(branch != null ? branch.getBranchName() : null)
                .merchantCode(merchant != null ? merchant.getMerchantCode() : null)
                .role(posAccount.getRole())
                .fullName(merchant != null ? merchant.getFullName() : null)
                .username(posAccount.getUsername())
                .phone(merchant != null ? merchant.getPhone() : null)
                .email(merchant != null ? merchant.getEmail() : null)
                .dob(merchant != null ? merchant.getDob() : null)
                .gender(merchant != null ? merchant.getGender() : null)
                .storeName(merchant != null ? merchant.getMerchantName() : null)
                .bankName(merchant != null ? merchant.getBankName() : null)
                .businessType(merchant != null ? merchant.getBusinessType() : null)
                .storeAddress(merchant != null ? merchant.getStoreAddress() : null)
                .phoneVerified(posAccount.isPhoneVerified())
                .terminalId(posAccount.getTerminalId())
                .active(posAccount.isActive())
                .online(isOnline)
                .build();
    }

    private Long resolveBranchIdForRequest(Merchant merchant, Long requestedBranchId) {
        if (merchant == null) {
            return null;
        }
        if (requestedBranchId != null) {
            Branch branch = branchRepo.findById(requestedBranchId)
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
            if (!merchant.getId().equals(branch.getMerchantId())) {
                throw new RuntimeException("Branch does not belong to merchant");
            }
            return branch.getId();
        }
        return ensureMainBranch(merchant).getId();
    }

    private Branch ensureMainBranch(Merchant merchant) {
        return branchRepo.findByMerchantIdAndBranchCode(merchant.getId(), DEFAULT_BRANCH_CODE)
                .orElseGet(() -> branchRepo.save(Branch.builder()
                        .merchantId(merchant.getId())
                        .branchCode(DEFAULT_BRANCH_CODE)
                        .branchName(normalizeText(merchant.getMerchantName()))
                        .branchAddress(normalizeText(merchant.getStoreAddress()))
                        .build()));
    }

    private String normalizeBusinessType(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.matches("^\\d{4}$")) {
            return normalized;
        }
        String fromPrefix = normalized.replace('\u2013', '-').replace(':', '-')
                .replaceAll("\\s+", " ");
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d{4})\\s*-\\s*.+$")
                .matcher(fromPrefix);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private String normalizeBankName(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(java.util.Locale.ROOT);
        if (!BANK_NAME_PATTERN.matcher(normalized).matches()) {
            throw new RuntimeException("Bank name must be 2-22 characters using A-Z and 0-9 only");
        }
        return normalized;
    }

    private String generateMerchantCode(Long userId, String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        String suffix = digits.length() > 4 ? digits.substring(digits.length() - 4) : digits;
        while (suffix.length() < 4) {
            suffix = "0" + suffix;
        }
        long uid = userId != null ? userId : System.currentTimeMillis() % 10_000_000_000L;
        String candidate = String.format(java.util.Locale.ROOT, "M%010d%s", uid % 10_000_000_000L, suffix);
        if (!merchantRepo.existsByMerchantCode(candidate)) {
            return candidate;
        }
        return String.format(java.util.Locale.ROOT, "M%014d", Math.abs(System.currentTimeMillis() % 100_000_000_000_000L));
    }

    private void validateTerminalId(String terminalId) {
        String normalized = normalizeTerminalId(terminalId);
        if (normalized == null || normalized.isEmpty()) {
            return;
        }
        if (!TID_PATTERN.matcher(normalized).matches()) {
            throw new RuntimeException("Terminal ID (TID) must be exactly 8 uppercase letters/digits");
        }
    }

    private String normalizeTerminalId(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toUpperCase(java.util.Locale.ROOT);
    }

    private String normalizeUsername(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
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

