package com.example.mysoftpos_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardDto {
    private Long id;
    private String panMasked;
    private String bin;
    private String last4;
    private String scheme;
    private Long adminId;
    private Long posAccountId;
}

