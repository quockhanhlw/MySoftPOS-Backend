package com.example.mysoftpos_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateUserRequest {
    @NotBlank private String username;
    @Size(min = 7) private String password; // nullable for update (keep existing password)
    private String fullName;
    private String phone;
    private String email;
    private String terminalId;
}
