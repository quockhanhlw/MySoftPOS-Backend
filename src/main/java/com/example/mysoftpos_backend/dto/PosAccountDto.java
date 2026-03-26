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
    private String serverIp;
    private Integer serverPort;
    private boolean active;
    private boolean online;

    public static PosAccountDto fromUserDto(UserDto userDto) {
        if (userDto == null) {
            return null;
        }
        return PosAccountDto.builder()
                .id(userDto.getId())
                .merchantId(userDto.getMerchantId())
                .branchId(userDto.getBranchId())
                .branchCode(userDto.getBranchCode())
                .branchName(userDto.getBranchName())
                .merchantCode(userDto.getMerchantCode())
                .role(userDto.getRole())
                .fullName(userDto.getFullName())
                .phone(userDto.getPhone())
                .email(userDto.getEmail())
                .dob(userDto.getDob())
                .gender(userDto.getGender())
                .storeName(userDto.getStoreName())
                .bankName(userDto.getBankName())
                .businessType(userDto.getBusinessType())
                .storeAddress(userDto.getStoreAddress())
                .phoneVerified(userDto.getPhoneVerified())
                .terminalId(userDto.getTerminalId())
                .serverIp(userDto.getServerIp())
                .serverPort(userDto.getServerPort())
                .active(userDto.isActive())
                .online(userDto.isOnline())
                .build();
    }
}

