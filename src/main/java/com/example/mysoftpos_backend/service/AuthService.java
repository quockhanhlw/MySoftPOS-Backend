package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.*;
import com.example.mysoftpos_backend.entity.Branch;
import com.example.mysoftpos_backend.entity.Merchant;
import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.repository.BranchRepository;
import com.example.mysoftpos_backend.repository.MerchantRepository;
import com.example.mysoftpos_backend.repository.PosAccountRepository;
import com.example.mysoftpos_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_ROLE = "USER";
    private static final int MAX_FAILED_ATTEMPTS = 6;
    private static final int LOCKOUT_MINUTES = 30;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int FORGOT_EMAIL_MAX_SEND_ATTEMPTS = 2;
    private static final long FORGOT_EMAIL_RETRY_DELAY_MS = 1200L;
    private static final int DEFAULT_ACCOUNT_COUNT = 1;
    private static final int MAX_ACCOUNT_COUNT = 500;
    private static final int MAX_BRANCH_COUNT = 50;
    private static final String DEFAULT_BRANCH_CODE = "MAIN";
    private static final java.util.regex.Pattern BANK_NAME_PATTERN = java.util.regex.Pattern.compile("^[A-Z0-9]{2,22}$");

    private final PosAccountRepository userRepo;
    private final MerchantRepository merchantRepo;
    private final BranchRepository branchRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;
    private final JavaMailSender mailSender;

    @Value("${app.auth.forgot-password.code-expiration-minutes:10}")
    private int forgotCodeExpirationMinutes;

    @Value("${app.auth.forgot-password.max-verify-attempts:5}")
    private int maxForgotVerifyAttempts;

    @Value("${app.auth.forgot-password.mail-from:no-reply@mysoftpos.local}")
    private String forgotMailFrom;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public LoginResponse register(RegisterRequest req) {
        String basePhone = normalizePhone(req.getPhone());
        String email = normalizeEmail(req.getEmail());
        String fullName = normalizeText(req.getFullName());
        String dob = normalizeText(req.getDob());
        String gender = normalizeText(req.getGender());
        String storeName = normalizeText(req.getStoreName());
        String bankName = normalizeBankName(req.getBankName());
        validateBankNameOrThrow(bankName);
        String businessType = normalizeBusinessType(req.getBusinessType());
        String storeAddress = normalizeText(req.getStoreAddress());
        int accountCount = normalizeAccountCount(req.getAccountCount());
        Long assignedAdminId = resolveAssignedAdminId();
        List<String> accountUsernames = buildMerchantAccountUsernames(basePhone, accountCount);

        validateAccountUsernamesAvailable(accountUsernames);
        if (basePhone == null || basePhone.isBlank()) {
            throw new RuntimeException("Phone number is required");
        }
        if (merchantRepo.existsByPhone(basePhone)) {
            throw new RuntimeException("Phone number already registered");
        }
        if (email != null && merchantRepo.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }
        if (req.getPassword() == null || req.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new RuntimeException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        String primaryUsername = accountUsernames.get(0);
        PosAccount user = PosAccount.builder()
                .username(primaryUsername)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(USER_ROLE)
                .phoneVerified(true)
                .adminId(assignedAdminId)
                .build();
        user = userRepo.save(user);

        Merchant merchant = Merchant.builder()
                .merchantCode(generateMerchantCode(basePhone, user.getId()))
                .merchantName(storeName != null ? storeName : fullName)
                .fullName(fullName)
                .phone(basePhone)
                .email(email)
                .dob(dob)
                .gender(gender)
                .bankName(bankName != null ? bankName : "MYSOFTPOS BANK")
                .adminId(assignedAdminId)
                .ownerUserId(user.getId())
                .businessType(businessType)
                .storeAddress(storeAddress)
                .build();
        merchant = merchantRepo.save(merchant);

        Branch mainBranch = ensureMainBranch(merchant);

        user.setMerchantId(merchant.getId());
        user.setBranchId(mainBranch.getId());
        userRepo.save(user);

        if (accountCount > 1) {
            createAdditionalMerchantAccounts(req.getPassword(), user, merchant, mainBranch, assignedAdminId,
                    accountUsernames);
        }

        return buildLoginResponse(user);
    }

    public LoginResponse login(LoginRequest req) {
        String identifier = normalizeIdentifier(req.getUsername());
        PosAccount user = resolveLoginUserByIdentifier(identifier);

        // PA-DSS 3.x: Account lockout check
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Account locked until " + user.getLockedUntil());
        }

        if (!user.isActive()) {
            throw new RuntimeException("Account is disabled");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
                user.setFailedLoginAttempts(0);
            }
            userRepo.save(user);
            throw new RuntimeException("Invalid credentials");
        }

        // Reset on success
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepo.save(user);

        return buildLoginResponse(user);
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        String username = jwtProvider.getSubjectFromToken(refreshToken);
        PosAccount user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Pos account not found"));
        return buildLoginResponse(user);
    }

    public Map<String, String> requestForgotPasswordCode(ForgotPasswordRequest req) {
        String email = normalizeEmail(req.getEmail());
        if (email == null) {
            throw new RuntimeException("Email is required");
        }

        Merchant merchant = merchantRepo.findByEmail(email).orElse(null);
        if (merchant == null || merchant.getOwnerUserId() == null) {
            // Do not leak account existence.
            return Map.of("message", "If your email exists, a verification code has been sent.");
        }

        PosAccount user = userRepo.findById(merchant.getOwnerUserId()).orElse(null);
        if (user == null || !user.isActive()) {
            return Map.of("message", "If your email exists, a verification code has been sent.");
        }

        String code = generateResetCode();
        user.setForgotPasswordCodeHash(passwordEncoder.encode(code));
        user.setForgotPasswordCodeExpiresAt(LocalDateTime.now().plusMinutes(forgotCodeExpirationMinutes));
        user.setForgotPasswordCodeVerifiedAt(null);
        user.setForgotPasswordCodeAttempts(0);
        userRepo.save(user);

        try {
            sendForgotPasswordEmailWithRetry(user, code);
        } catch (RuntimeException ex) {
            // Avoid keeping a fresh OTP when delivery fails.
            clearForgotPasswordState(user);
            userRepo.save(user);
            throw ex;
        }
        return Map.of("message", "If your email exists, a verification code has been sent.");
    }

    public Map<String, String> verifyForgotPasswordCode(ForgotPasswordVerifyCodeRequest req) {
        PosAccount user = getUserForForgotPassword(req.getEmail());
        validateForgotCode(user, req.getCode());
        user.setForgotPasswordCodeVerifiedAt(LocalDateTime.now());
        userRepo.save(user);
        return Map.of("message", "Verification successful");
    }

    public Map<String, String> resetForgotPassword(ForgotPasswordResetRequest req) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new RuntimeException("Password confirmation does not match");
        }
        if (req.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new RuntimeException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        PosAccount user = getUserForForgotPassword(req.getEmail());
        validateForgotCode(user, req.getCode());

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        clearForgotPasswordState(user);
        userRepo.save(user);
        return Map.of("message", "Password reset successfully");
    }

    public Map<String, String> changePassword(PosAccount currentUser, ChangePasswordRequest req) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new RuntimeException("Unauthorized");
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new RuntimeException("Password confirmation does not match");
        }
        if (req.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new RuntimeException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        PosAccount user = userRepo.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Pos account not found"));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepo.save(user);
        return Map.of("message", "Password changed successfully");
    }

    private LoginResponse buildLoginResponse(PosAccount user) {
        String accessToken = jwtProvider.generateAccessToken(user.getUsername(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getUsername());
        Merchant merchant = user.getMerchantId() != null
                ? merchantRepo.findById(user.getMerchantId()).orElse(null)
                : merchantRepo.findByOwnerUserId(user.getId()).orElse(null);
        Branch branch = user.getBranchId() != null ? branchRepo.findById(user.getBranchId()).orElse(null) : null;

        PosAccountDto dto = PosAccountDto.builder()
                .id(user.getId())
                .merchantId(user.getMerchantId())
                .branchId(user.getBranchId())
                .branchCode(branch != null ? branch.getBranchCode() : null)
                .branchName(branch != null ? branch.getBranchName() : null)
                .merchantCode(merchant != null ? merchant.getMerchantCode() : null)
                .role(user.getRole())
                .fullName(merchant != null ? merchant.getFullName() : null)
                .username(user.getUsername())
                .phone(merchant != null ? merchant.getPhone() : null)
                .email(merchant != null ? merchant.getEmail() : null)
                .dob(merchant != null ? merchant.getDob() : null)
                .gender(merchant != null ? merchant.getGender() : null)
                .storeName(merchant != null ? merchant.getMerchantName() : null)
                .bankName(merchant != null ? merchant.getBankName() : null)
                .businessType(merchant != null ? merchant.getBusinessType() : null)
                .storeAddress(merchant != null ? merchant.getStoreAddress() : null)
                .phoneVerified(user.isPhoneVerified())
                .terminalId(user.getTerminalId())
                .active(user.isActive())
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .posAccount(dto)
                .user(UserDto.fromPosAccountDto(dto))
                .build();
    }

    private Long resolveAssignedAdminId() {
        PosAccount admin = userRepo.findFirstByRoleOrderByIdAsc(ADMIN_ROLE).orElse(null);
        return admin != null ? admin.getId() : null;
    }

    private Integer normalizeBranchCount(Integer value) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            return 0;
        }
        return Math.min(value, MAX_BRANCH_COUNT);
    }

    private int normalizeAccountCount(Integer value) {
        if (value == null || value < 1) {
            return DEFAULT_ACCOUNT_COUNT;
        }
        return Math.min(value, MAX_ACCOUNT_COUNT);
    }

    private void createAdditionalMerchantAccounts(String rawPassword,
                                                  PosAccount primaryUser,
                                                  Merchant merchant,
                                                  Branch mainBranch,
                                                  Long assignedAdminId,
                                                  List<String> accountUsernames) {
        List<PosAccount> createdAccounts = new ArrayList<>();
        for (int accountIndex = 2; accountIndex <= accountUsernames.size(); accountIndex++) {
            String accountUsername = accountUsernames.get(accountIndex - 1);

            PosAccount account = PosAccount.builder()
                    .username(accountUsername)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .role(USER_ROLE)
                    .phoneVerified(true)
                    .adminId(assignedAdminId)
                    .merchantId(merchant.getId())
                    .branchId(mainBranch != null ? mainBranch.getId() : null)
                    .active(true)
                    .build();
            createdAccounts.add(account);
        }
        userRepo.saveAll(createdAccounts);
    }

    private Branch ensureMainBranch(Merchant merchant) {
        if (merchant == null || merchant.getId() == null) {
            return null;
        }
        return branchRepo.findByMerchantIdAndBranchCode(merchant.getId(), DEFAULT_BRANCH_CODE)
                .orElseGet(() -> branchRepo.save(Branch.builder()
                        .merchantId(merchant.getId())
                        .branchCode(DEFAULT_BRANCH_CODE)
                        .branchName(normalizeText(merchant.getMerchantName()))
                        .branchAddress(normalizeText(merchant.getStoreAddress()))
                        .build()));
    }

    private List<String> buildMerchantAccountUsernames(String basePhone, int accountCount) {
        if (basePhone == null || basePhone.isBlank()) {
            throw new RuntimeException("Phone number is required");
        }

        List<String> usernames = new ArrayList<>();
        for (int index = 1; index <= accountCount; index++) {
            String candidate = basePhone + index;
            if (candidate.length() > 40) {
                throw new RuntimeException("Generated account username exceeds max length");
            }
            usernames.add(candidate);
        }
        return usernames;
    }

    private void validateAccountUsernamesAvailable(List<String> accountUsernames) {
        java.util.Set<String> unique = new java.util.LinkedHashSet<>(accountUsernames);
        if (unique.size() != accountUsernames.size()) {
            throw new RuntimeException("Generated account usernames are duplicated");
        }
        for (String username : unique) {
            if (userRepo.existsByUsername(username)) {
                throw new RuntimeException("Username already registered: " + username);
            }
        }
    }

    private PosAccount resolveLoginUserByIdentifier(String identifier) {
        PosAccount byUsername = userRepo.findByUsername(identifier).orElse(null);
        if (byUsername != null) {
            return byUsername;
        }
        Merchant merchant = merchantRepo.findByEmail(identifier).orElse(null);
        if (merchant != null && merchant.getOwnerUserId() != null) {
            return userRepo.findById(merchant.getOwnerUserId())
                    .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        }
        throw new RuntimeException("Invalid credentials");
    }

    private String normalizeIdentifier(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.contains("@")) {
            return normalized.toLowerCase(Locale.ROOT);
        }
        normalized = normalized.replaceAll("[\\s()-]", "");
        if (normalized.startsWith("00")) {
            normalized = "+" + normalized.substring(2);
        }
        if (normalized.indexOf('+') > 0) {
            normalized = normalized.replace("+", "");
        }
        return normalized;
    }

    private String normalizePhone(String value) {
        String normalized = normalizeIdentifier(value).replaceAll("[\\s()-]", "");
        if (normalized.startsWith("00")) {
            return "+" + normalized.substring(2);
        }
        return normalized;
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeIdentifier(value);
        return normalized.isEmpty() ? null : normalized.toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeText(String value) {
        String normalized = normalizeIdentifier(value);
        return normalized.isEmpty() ? null : normalized;
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
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    private String normalizeBankName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }

    private void validateBankNameOrThrow(String bankName) {
        if (bankName == null || !BANK_NAME_PATTERN.matcher(bankName).matches()) {
            throw new RuntimeException("Bank name must be 2-22 characters using A-Z and 0-9 only");
        }
    }

    private String generateMerchantCode(String phone, Long userId) {
        String suffix = phone == null ? "0000" : phone.replaceAll("\\D", "");
        if (suffix.length() > 4) {
            suffix = suffix.substring(suffix.length() - 4);
        }
        while (suffix.length() < 4) {
            suffix = "0" + suffix;
        }
        long uid = userId != null ? userId : System.currentTimeMillis() % 10_000_000_000L;
        String candidate = String.format(Locale.ROOT, "M%010d%s", uid % 10_000_000_000L, suffix);
        if (!merchantRepo.existsByMerchantCode(candidate)) {
            return candidate;
        }
        return String.format(Locale.ROOT, "M%014d", Math.abs(System.currentTimeMillis() % 100_000_000_000_000L));
    }

    private PosAccount getUserForForgotPassword(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (email == null) {
            throw new RuntimeException("Email is required");
        }
        Merchant merchant = merchantRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification code"));
        if (merchant.getOwnerUserId() == null) {
            throw new RuntimeException("Invalid or expired verification code");
        }
        return userRepo.findById(merchant.getOwnerUserId())
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification code"));
    }

    private void validateForgotCode(PosAccount user, String code) {
        if (code == null || code.isBlank()) {
            throw new RuntimeException("Verification code is required");
        }

        LocalDateTime expiresAt = user.getForgotPasswordCodeExpiresAt();
        String codeHash = user.getForgotPasswordCodeHash();
        if (codeHash == null || expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            clearForgotPasswordState(user);
            userRepo.save(user);
            throw new RuntimeException("Invalid or expired verification code");
        }

        if (user.getForgotPasswordCodeAttempts() >= maxForgotVerifyAttempts) {
            clearForgotPasswordState(user);
            userRepo.save(user);
            throw new RuntimeException("Too many invalid attempts. Please request a new code.");
        }

        if (!passwordEncoder.matches(code, codeHash)) {
            user.setForgotPasswordCodeAttempts(user.getForgotPasswordCodeAttempts() + 1);
            userRepo.save(user);
            throw new RuntimeException("Invalid or expired verification code");
        }
    }

    private void clearForgotPasswordState(PosAccount user) {
        user.setForgotPasswordCodeHash(null);
        user.setForgotPasswordCodeExpiresAt(null);
        user.setForgotPasswordCodeVerifiedAt(null);
        user.setForgotPasswordCodeAttempts(0);
    }

    private String generateResetCode() {
        int value = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format(java.util.Locale.ROOT, "%06d", value);
    }

    private void sendForgotPasswordEmailWithRetry(PosAccount user, String code) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= FORGOT_EMAIL_MAX_SEND_ATTEMPTS; attempt++) {
            try {
                sendForgotPasswordEmail(user, code);
                return;
            } catch (RuntimeException ex) {
                lastException = ex;
                if (!isTimeoutMessage(ex.getMessage()) || attempt >= FORGOT_EMAIL_MAX_SEND_ATTEMPTS) {
                    throw ex;
                }
                log.warn("Retry forgot-password email send attempt {}/{} for {} after timeout",
                        attempt + 1, FORGOT_EMAIL_MAX_SEND_ATTEMPTS, resolveMerchantEmail(user));
                sleepQuietly(FORGOT_EMAIL_RETRY_DELAY_MS);
            }
        }

        if (lastException != null) {
            throw lastException;
        }
    }

    private void sendForgotPasswordEmail(PosAccount user, String code) {
        String email = resolveMerchantEmail(user);
        if (email == null || email.isBlank()) {
            throw new RuntimeException("User does not have a registered email");
        }
        if (mailHost == null || mailHost.isBlank()) {
            throw new RuntimeException("Email service is not configured on server");
        }
        if (mailUsername == null || mailUsername.isBlank()) {
            throw new RuntimeException("Email sender account is not configured on server");
        }

        String from = (forgotMailFrom == null || forgotMailFrom.isBlank())
                ? mailUsername
                : forgotMailFrom;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom(from);
        message.setSubject("MySoftPOS password reset code");
        message.setText(buildForgotMailBody(user, code));

        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Keep full cause in logs for SMTP diagnosis (auth, TLS, network timeout, etc.).
            log.warn("Failed to send forgot-password email to {}", email, e);
            if (isTimeoutError(e)) {
                throw new RuntimeException("Email service timed out. Please try again in a moment.");
            }
            throw new RuntimeException("Unable to send verification email. Please contact support.");
        }
    }

    private String resolveMerchantEmail(PosAccount user) {
        if (user == null) {
            return null;
        }
        Merchant merchant = user.getMerchantId() != null
                ? merchantRepo.findById(user.getMerchantId()).orElse(null)
                : merchantRepo.findByOwnerUserId(user.getId()).orElse(null);
        return merchant != null ? merchant.getEmail() : null;
    }

    private boolean isTimeoutError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isTimeoutMessage(String message) {
        return message != null && message.toLowerCase(java.util.Locale.ROOT).contains("timed out");
    }

    private void sleepQuietly(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildForgotMailBody(PosAccount user, String code) {
        Merchant merchant = user.getMerchantId() != null
                ? merchantRepo.findById(user.getMerchantId()).orElse(null)
                : merchantRepo.findByOwnerUserId(user.getId()).orElse(null);
        String name = merchant != null && merchant.getFullName() != null && !merchant.getFullName().isBlank()
                ? merchant.getFullName().trim()
                : "user";
        return "Hello " + name + ",\n\n"
                + "Your MySoftPOS verification code is: " + code + "\n"
                + "This code expires in " + forgotCodeExpirationMinutes + " minutes.\n\n"
                + "If you did not request this, please ignore this email.";
    }
}
