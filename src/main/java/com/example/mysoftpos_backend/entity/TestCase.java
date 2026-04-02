package com.example.mysoftpos_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suite_id", nullable = false)
    private TestSuite suite;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 20)
    private String transactionType; // PURCHASE, BALANCE

    @Column(length = 20)
    private String status; // PASS, FAIL, PENDING

    @Column(length = 20)
    private String amount;

    @Column(length = 10)
    private String de22;

    @Column(length = 25)
    private String maskedPan;

    @Column(length = 10)
    private String expiry;

    @Column(name = "req_file_path", length = 500)
    private String reqFilePath;

    @Column(name = "res_file_path", length = 500)
    private String resFilePath;

    @Column(length = 30)
    private String scheme; // Napas, Visa, etc.

    @Column(length = 40)
    private String track2;

    /** JSON map of additional ISO8583 field overrides */
    @Column(columnDefinition = "TEXT")
    private String fieldConfigJson;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = Boolean.FALSE;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
