package com.example.mysoftpos_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class LegacyUsersDailyHitDto {
    private LocalDate hitDate;
    private long hitCount;
}
