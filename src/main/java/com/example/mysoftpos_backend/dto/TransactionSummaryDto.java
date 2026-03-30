package com.example.mysoftpos_backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionSummaryDto {
    private Long id;
    private String traceNumber;
    private String amount;
    private String status;
    private String maskedPan;
    private String cardScheme;
    private Long terminalId;
    private Long cardId;
    private String terminalCode;
    private String deviceId;
    private String txnTimestamp;
    private String syncedAt;
    private Long posAccountId;
    @Deprecated(since = "V18", forRemoval = false)
    private Long userId;
    private String username;
    private String requestHex;
    private String responseHex;
    private String processingCode;
    private String currencyCode;
    private String rrn;
}

