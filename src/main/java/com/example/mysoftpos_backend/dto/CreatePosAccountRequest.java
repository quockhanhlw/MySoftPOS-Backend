package com.example.mysoftpos_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreatePosAccountRequest {
    @NotBlank private String phone;
    @Size(min = 8) private String password; // nullable for update (keep existing password)
    private String fullName;
    @Email private String email;
    private String dob;
    private String gender;
    private String storeName;
    @Size(min = 2, max = 22)
    @Pattern(regexp = "^[A-Za-z0-9]{2,22}$")
    private String bankName;
    private String businessType;
    private String storeAddress;
    private Long merchantId;
    private Long branchId;
    private String terminalId;
    private String serverIp;
    private Integer serverPort;

    public CreatePosAccountRequest(String phone,
                                   String password,
                                   String fullName,
                                   String email,
                                   String dob,
                                   String gender,
                                   String storeName,
                                   String bankName,
                                   String businessType,
                                   String storeAddress,
                                   Long merchantId,
                                   Long branchId,
                                   String terminalId,
                                   String serverIp,
                                   Integer serverPort) {
        this.phone = phone;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.dob = dob;
        this.gender = gender;
        this.storeName = storeName;
        this.bankName = bankName;
        this.businessType = businessType;
        this.storeAddress = storeAddress;
        this.merchantId = merchantId;
        this.branchId = branchId;
        this.terminalId = terminalId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }
}

