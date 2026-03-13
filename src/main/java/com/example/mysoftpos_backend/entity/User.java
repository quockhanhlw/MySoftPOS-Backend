package com.example.mysoftpos_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "USER"; // ADMIN or USER

    @Column(length = 200)
    private String fullName;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    /** The admin who created this user (null if self-registered) */
    @Column
    private Long adminId;

    /** Terminal ID (TID / DE 41) — each user maps to one TID */
    @Column(length = 8)
    private String terminalId;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(length = 50)
    private String serverIp;

    @Column
    private Integer serverPort;

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
