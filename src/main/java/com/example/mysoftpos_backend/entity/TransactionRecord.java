package com.example.mysoftpos_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transactions_trace_pos_account", columnNames = {"trace_number", "pos_account_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String traceNumber;

    @Column(length = 20)
    private String amount;

    @Column(length = 40)
    private String status; // APPROVED, DECLINED, TIMEOUT

    /** PA-DSS: Only masked PAN (first6+last4) — never full PAN */
    @Column(length = 25)
    private String maskedPan;

    @Column(length = 20)
    private String cardScheme; // Napas, Visa, MC

    @Column(name = "terminal_id")
    private Long terminalId;

    @Column(name = "card_id")
    private Long cardId;

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


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pos_account_id")
    private PosAccount posAccount;

    @Column(length = 50)
    private String deviceId;

    @Column(name = "txn_timestamp")
    private LocalDateTime txnTimestamp;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime syncedAt = LocalDateTime.now();
}

