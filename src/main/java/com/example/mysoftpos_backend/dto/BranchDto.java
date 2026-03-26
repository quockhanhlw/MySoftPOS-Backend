package com.example.mysoftpos_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDto {
    private Long id;
    private Long merchantId;
    private String branchCode;
    private String branchName;
    private String branchAddress;
    private Integer accountCount;
}

