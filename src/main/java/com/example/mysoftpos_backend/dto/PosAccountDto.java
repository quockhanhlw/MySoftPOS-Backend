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
public class PosAccountDto {
    private Long id;
    private Long merchantId;
    private Long branchId;
    private String branchCode;
    private String branchName;
    private String merchantCode;
    private String role;
    private String fullName;
    private String username;
    private String phone;
    private String merchantPhone;
    private String email;
    private String dob;
    private String gender;
    private String storeName;
    private String bankName;
    private String businessType;
    private String storeAddress;
    private Boolean phoneVerified;
    private String terminalId;
    private String serverIp;
    private Integer serverPort;
    private boolean active;
    private boolean online;

}

