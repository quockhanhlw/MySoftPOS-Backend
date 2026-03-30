package com.example.mysoftpos_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private PosAccountDto posAccount;

    /**
     * Temporary alias for older app builds expecting `user` in login payload.
     */
    @JsonProperty("user")
    private PosAccountDto user;
}
