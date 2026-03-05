package com.example.mysoftpos_backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDto {
    private Long id;
    private String role;
    private String fullName;
    private String phone;
    private String email;
    private String terminalId;
    private boolean active;
}
