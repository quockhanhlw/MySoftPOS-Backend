package com.example.mysoftpos_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pos_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "USER"; // ADMIN or USER

    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @Column(nullable = false)
    @Builder.Default
    private boolean phoneVerified = true;

    /** The admin who created this account (null if self-registered) */
    @Column
    private Long adminId;

    /** Merchant profile this account belongs to */
    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "branch_id")
    private Long branchId;

    /** Terminal ID (TID / DE 41) — each account maps to one TID */
    @Column(length = 8)
    private String terminalId;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // PA-DSS 3.x: Account lockout
    @Column(nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column
    private LocalDateTime lockedUntil;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime lastActiveAt;

    @Column(length = 255)
    private String forgotPasswordCodeHash;

    @Column
    private LocalDateTime forgotPasswordCodeExpiresAt;

    @Column
    private LocalDateTime forgotPasswordCodeVerifiedAt;

    @Column(nullable = false)
    @Builder.Default
    private int forgotPasswordCodeAttempts = 0;
}
