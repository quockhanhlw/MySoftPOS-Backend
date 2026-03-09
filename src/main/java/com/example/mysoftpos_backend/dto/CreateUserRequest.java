package com.example.mysoftpos_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateUserRequest {
    @NotBlank private String phone;
    @Size(min = 7) private String password; // nullable for update (keep existing password)
    private String fullName;
    @Email private String email;
    private String terminalId;
    private String serverIp;
    private Integer serverPort;
}
