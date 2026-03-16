package com.example.mysoftpos_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}

