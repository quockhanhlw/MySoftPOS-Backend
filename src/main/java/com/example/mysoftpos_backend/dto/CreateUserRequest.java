package com.example.mysoftpos_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateUserRequest {
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
}
