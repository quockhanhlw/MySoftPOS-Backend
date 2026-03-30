package com.example.mysoftpos_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
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
    private String email;
    private String dob;
    private String gender;
    private String storeName;
    private String bankName;
    private String businessType;
    private String storeAddress;
    private Boolean phoneVerified;
    private String terminalId;
    private boolean active;
    private boolean online;

    public static UserDto fromPosAccountDto(PosAccountDto posAccountDto) {
        if (posAccountDto == null) {
            return null;
        }
        return UserDto.builder()
                .id(posAccountDto.getId())
                .merchantId(posAccountDto.getMerchantId())
                .branchId(posAccountDto.getBranchId())
                .branchCode(posAccountDto.getBranchCode())
                .branchName(posAccountDto.getBranchName())
                .merchantCode(posAccountDto.getMerchantCode())
                .role(posAccountDto.getRole())
                .fullName(posAccountDto.getFullName())
                .username(posAccountDto.getUsername())
                .phone(posAccountDto.getPhone())
                .email(posAccountDto.getEmail())
                .dob(posAccountDto.getDob())
                .gender(posAccountDto.getGender())
                .storeName(posAccountDto.getStoreName())
                .bankName(posAccountDto.getBankName())
                .businessType(posAccountDto.getBusinessType())
                .storeAddress(posAccountDto.getStoreAddress())
                .phoneVerified(posAccountDto.getPhoneVerified())
                .terminalId(posAccountDto.getTerminalId())
                .active(posAccountDto.isActive())
                .online(posAccountDto.isOnline())
                .build();
    }
}
