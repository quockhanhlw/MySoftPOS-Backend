package com.example.mysoftpos_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LegacyUsersVersionHitDto {
    private String appVersion;
    private long hitCount;
}
