package com.example.mysoftpos_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    @NotBlank private String phone;
    @NotBlank @Size(min = 7) private String password;
    private String fullName;
    private String email;
}
