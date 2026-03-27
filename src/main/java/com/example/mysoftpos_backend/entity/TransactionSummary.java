package com.example.mysoftpos_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String traceNumber;

    @Column(length = 20)
    private String amount;

    @Column(length = 20)
    private String status; // APPROVED, DECLINED, TIMEOUT

    /** PA-DSS: Only masked PAN (first6+last4) — never full PAN */
    @Column(length = 25)
    private String maskedPan;

    @Column(length = 20)
    private String cardScheme; // Napas, Visa, MC

    @Column(length = 8)
    private String terminalCode;

    @Lob
    @Column(name = "request_hex", columnDefinition = "TEXT")
    private String requestHex;

    @Lob
    @Column(name = "response_hex", columnDefinition = "TEXT")
    private String responseHex;

    @Column(name = "processing_code", length = 6)
    private String processingCode;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(length = 12)
    private String rrn;

    @Column(name = "owner_username", length = 64)
    private String ownerUsername;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 50)
    private String deviceId;

    @Column
    private LocalDateTime txnTimestamp;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime syncedAt = LocalDateTime.now();
}
