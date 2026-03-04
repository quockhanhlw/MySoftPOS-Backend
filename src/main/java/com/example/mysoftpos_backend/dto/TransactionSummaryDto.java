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
    private String terminalCode;
    private String deviceId;
    private String txnTimestamp;
    private String syncedAt;
    private Long userId;
    private String username;
}

