package com.example.mysoftpos_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private String track2;
    private String scheme;
    private String fieldConfigJson;
    private String createdAt;
}
