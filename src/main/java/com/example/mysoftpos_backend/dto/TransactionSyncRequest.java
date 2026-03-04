package com.example.mysoftpos_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TransactionSyncRequest {
    private List<TxnItem> transactions;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class TxnItem {
        @NotBlank private String traceNumber;
        private String amount;
        private String status;
        private String maskedPan;
        private String cardScheme;
        private String terminalCode;
        private String deviceId;
        private long txnTimestamp; // epoch millis
    }
}
