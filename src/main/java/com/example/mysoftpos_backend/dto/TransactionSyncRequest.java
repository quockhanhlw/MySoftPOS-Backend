package com.example.mysoftpos_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionSyncRequest {
    private List<TxnItem> transactions;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TxnItem {
        @NotBlank private String traceNumber;
        private String amount;
        private String status;
        private String maskedPan;
        private String cardScheme;
        private Long terminalId;
        private Long cardId;
        private String terminalCode;
        private String deviceId;
        private String requestHex;
        private String responseHex;
        private String processingCode;
        private String currencyCode;
        private String rrn;
        private long txnTimestamp; // epoch millis
    }
}
