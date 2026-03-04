package com.example.mysoftpos_backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MerchantDto {
    private Long id;
    private String merchantCode;
    private String merchantName;
    private Long adminId;
}

