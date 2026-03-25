package com.example.mysoftpos_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    @NotBlank private String phone;
    @NotBlank @Size(min = 8) private String password;
    private String fullName;
    @Email private String email;
    private String dob;
    private String gender;
    private String storeName;
    private String businessType;
    private String storeAddress;
    private Integer branchCount;
    private String branchAddresses;
    private Integer accountCount;
}
