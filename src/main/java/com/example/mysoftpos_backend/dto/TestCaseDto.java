package com.example.mysoftpos_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestCaseDto {
    private Long id;
    private Long suiteId;
    private String name;
    private String transactionType;
    private String status;
    private String amount;
    private String de22;
    private String maskedPan;
    private String expiry;
    private String reqFilePath;
    private String resFilePath;
    private String scheme;
    private String track2;
    private String fieldConfigJson;
    private Boolean isDefault;
    private String createdAt;
}
