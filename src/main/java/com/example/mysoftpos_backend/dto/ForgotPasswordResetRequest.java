package com.example.mysoftpos_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordResetRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 6)
    private String code;

    @NotBlank
    @Size(min = 8)
    private String newPassword;

    @NotBlank
    @Size(min = 8)
    private String confirmPassword;
}

