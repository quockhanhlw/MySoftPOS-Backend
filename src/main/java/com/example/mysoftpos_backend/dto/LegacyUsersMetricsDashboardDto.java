package com.example.mysoftpos_backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LegacyUsersMetricsDashboardDto {
    private int windowDays;
    private long totalHits;
    private List<LegacyUsersVersionHitDto> hitsByVersion;
    private List<LegacyUsersDailyHitDto> hitsByDay;
}
