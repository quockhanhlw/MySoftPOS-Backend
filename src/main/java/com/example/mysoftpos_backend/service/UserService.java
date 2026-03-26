package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.CreateUserRequest;
import com.example.mysoftpos_backend.dto.UserDto;
import com.example.mysoftpos_backend.entity.Branch;
import com.example.mysoftpos_backend.entity.Merchant;
import com.example.mysoftpos_backend.entity.User;
import com.example.mysoftpos_backend.repository.BranchRepository;
import com.example.mysoftpos_backend.repository.MerchantRepository;
import com.example.mysoftpos_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final java.util.regex.Pattern TID_PATTERN = java.util.regex.Pattern.compile("^[A-Z0-9]{8}$");
    private static final java.util.regex.Pattern BANK_NAME_PATTERN = java.util.regex.Pattern.compile("^[A-Z0-9]{2,22}$");
    private static final String DEFAULT_BRANCH_CODE = "MAIN";

    private final UserRepository userRepo;
    private final MerchantRepository merchantRepo;
    private final BranchRepository branchRepo;
    private final PasswordEncoder passwordEncoder;

    public List<UserDto> getUsersByAdmin(Long adminId) {
        return userRepo.findByAdminId(adminId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    // Domain alias: users table is currently the POS account table.
    public List<UserDto> getPosAccountsByAdmin(Long adminId) {
        return getUsersByAdmin(adminId);
    }

    public UserDto createUser(Long adminId, CreateUserRequest req) {
        String phone = normalizePhone(req.getPhone());
        String username = normalizeUsername(req.getPhone());
        String email = normalizeEmail(req.getEmail());
        String fullName = normalizeText(req.getFullName());
        Merchant targetMerchant = null;
        Long branchId = null;

        if (req.getMerchantId() != null) {
            targetMerchant = merchantRepo.findById(req.getMerchantId())
                    .orElseThrow(() -> new RuntimeException("Merchant not found"));
            if (!adminId.equals(targetMerchant.getAdminId())) {
                throw new RuntimeException("Access denied");
            }
            branchId = resolveBranchIdForRequest(targetMerchant, req.getBranchId());
        }

        if (userRepo.existsByUsername(username)) {
            throw new RuntimeException("Username already registered");
        }
        if (userRepo.existsByPhone(phone)) {
            throw new RuntimeException("Phone number already registered");
        }
        if (email != null && userRepo.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new RuntimeException("Password is required for new user");
        }
        validateTerminalId(req.getTerminalId());

        User user = User.builder()
                .phone(phone)
                .username(username)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role("USER")
                .fullName(fullName)
                .email(email)
                .dob(normalizeText(req.getDob()))
                .gender(normalizeText(req.getGender()))
                .phoneVerified(true)
                .terminalId(normalizeTerminalId(req.getTerminalId()))
                .serverIp(normalizeText(req.getServerIp()))
                .serverPort(req.getServerPort())
                .adminId(adminId)
                .merchantId(targetMerchant != null ? targetMerchant.getId() : null)
                .branchId(branchId)
                .build();
        user = userRepo.save(user);

        if (targetMerchant == null) {
            Merchant merchant = Merchant.builder()
                    .merchantCode(generateMerchantCode(user.getId(), phone))
                    .merchantName(normalizeText(req.getStoreName()) != null
                            ? normalizeText(req.getStoreName())
                            : fullName)
                    .bankName(normalizeBankName(req.getBankName()))
                    .adminId(adminId)
                    .ownerUserId(user.getId())
                    .businessType(normalizeBusinessType(req.getBusinessType()))
                    .storeAddress(normalizeText(req.getStoreAddress()))
                    .accountCount(1)
                    .build();
            merchant = merchantRepo.save(merchant);
            Branch mainBranch = ensureMainBranch(merchant);
            user.setMerchantId(merchant.getId());
            user.setBranchId(mainBranch.getId());
            userRepo.save(user);
        }

        return toDto(user);
    }

    public UserDto createPosAccount(Long adminId, CreateUserRequest req) {
        return createUser(adminId, req);
    }

    public UserDto updateUser(Long adminId, Long userId, CreateUserRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!adminId.equals(user.getAdminId())) {
            throw new RuntimeException("Access denied");
        }
        validateTerminalId(req.getTerminalId());

        if (req.getFullName() != null)
            user.setFullName(normalizeText(req.getFullName()));
        if (req.getPhone() != null) {
            String phone = normalizePhone(req.getPhone());
            String username = normalizeUsername(req.getPhone());
            if (!phone.equals(user.getPhone()) && userRepo.existsByPhone(phone)) {
                throw new RuntimeException("Phone number already registered");
            }
            if (!username.equals(user.getUsername()) && userRepo.existsByUsername(username)) {
                throw new RuntimeException("Username already registered");
            }
            user.setPhone(phone);
            user.setUsername(username);
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
            user.setTerminalId(normalizeTerminalId(req.getTerminalId()));
        if (req.getServerIp() != null)
            user.setServerIp(normalizeText(req.getServerIp()));
        if (req.getServerPort() != null)
            user.setServerPort(req.getServerPort());
        if (req.getMerchantId() != null) {
            Merchant targetMerchant = merchantRepo.findById(req.getMerchantId())
                    .orElseThrow(() -> new RuntimeException("Merchant not found"));
            if (!adminId.equals(targetMerchant.getAdminId())) {
                throw new RuntimeException("Access denied");
            }
            user.setMerchantId(targetMerchant.getId());
            if (req.getBranchId() == null) {
                user.setBranchId(resolveBranchIdForRequest(targetMerchant, null));
            }
        }
        if (req.getBranchId() != null) {
            Merchant scopeMerchant = user.getMerchantId() != null
                    ? merchantRepo.findById(user.getMerchantId()).orElse(null)
                    : null;
            if (scopeMerchant == null || !adminId.equals(scopeMerchant.getAdminId())) {
                throw new RuntimeException("Merchant not found or access denied");
            }
            user.setBranchId(resolveBranchIdForRequest(scopeMerchant, req.getBranchId()));
        }
        if (req.getDob() != null)
            user.setDob(normalizeText(req.getDob()));
        if (req.getGender() != null)
            user.setGender(normalizeText(req.getGender()));
        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }
        userRepo.save(user);

        Merchant merchant = user.getMerchantId() != null
                ? merchantRepo.findById(user.getMerchantId()).orElse(null)
                : merchantRepo.findByOwnerUserId(user.getId()).orElse(null);
        if (merchant != null) {
            if (req.getStoreName() != null)
                merchant.setMerchantName(normalizeText(req.getStoreName()));
            if (req.getBankName() != null)
                merchant.setBankName(normalizeBankName(req.getBankName()));
            if (req.getBusinessType() != null)
                merchant.setBusinessType(normalizeBusinessType(req.getBusinessType()));
            if (req.getStoreAddress() != null)
                merchant.setStoreAddress(normalizeText(req.getStoreAddress()));
            merchantRepo.save(merchant);
        }
        return toDto(user);
    }

    public UserDto updatePosAccount(Long adminId, Long accountId, CreateUserRequest req) {
        return updateUser(adminId, accountId, req);
    }

    public void deleteUser(Long adminId, Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!adminId.equals(user.getAdminId())) {
            throw new RuntimeException("Access denied");
        }
        merchantRepo.deleteByOwnerUserId(user.getId());
        userRepo.delete(user);
    }

    public void deletePosAccount(Long adminId, Long accountId) {
        deleteUser(adminId, accountId);
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

    public void resetPosAccountPassword(Long adminId, Long accountId, String newPassword) {
        resetPassword(adminId, accountId, newPassword);
    }

    public UserDto toDto(User u) {
        Merchant merchant = u.getMerchantId() != null
                ? merchantRepo.findById(u.getMerchantId()).orElse(null)
                : merchantRepo.findByOwnerUserId(u.getId()).orElse(null);
        Branch branch = u.getBranchId() != null ? branchRepo.findById(u.getBranchId()).orElse(null) : null;
        Long merchantId = u.getMerchantId() != null
                ? u.getMerchantId()
                : (merchant != null ? merchant.getId() : null);
        boolean isOnline = u.getLastActiveAt() != null &&
                u.getLastActiveAt().isAfter(java.time.LocalDateTime.now().minusMinutes(5));

        return UserDto.builder()
                .id(u.getId())
                .merchantId(merchantId)
                .branchId(u.getBranchId())
                .branchCode(branch != null ? branch.getBranchCode() : null)
                .branchName(branch != null ? branch.getBranchName() : null)
                .merchantCode(merchant != null ? merchant.getMerchantCode() : null)
                .role(u.getRole())
                .fullName(u.getFullName())
                .username(u.getUsername())
                .phone(u.getPhone())
                .email(u.getEmail())
                .dob(u.getDob())
                .gender(u.getGender())
                .storeName(merchant != null ? merchant.getMerchantName() : null)
                .bankName(merchant != null ? merchant.getBankName() : null)
                .businessType(merchant != null ? merchant.getBusinessType() : null)
                .storeAddress(merchant != null ? merchant.getStoreAddress() : null)
                .phoneVerified(u.isPhoneVerified())
                .terminalId(u.getTerminalId())
                .serverIp(u.getServerIp())
                .serverPort(u.getServerPort())
                .active(u.isActive())
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

    private String normalizePhone(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeUsername(String value) {
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
