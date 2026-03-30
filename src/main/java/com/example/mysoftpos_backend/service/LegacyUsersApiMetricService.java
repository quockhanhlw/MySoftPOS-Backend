package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.LegacyUsersDailyHitDto;
import com.example.mysoftpos_backend.dto.LegacyUsersMetricsDashboardDto;
import com.example.mysoftpos_backend.dto.LegacyUsersVersionHitDto;
import com.example.mysoftpos_backend.entity.LegacyUsersApiMetric;
import com.example.mysoftpos_backend.repository.LegacyUsersApiMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LegacyUsersApiMetricService {

    private final LegacyUsersApiMetricRepository metricRepository;

    @Transactional
    public void recordHit(String endpointPath, String httpMethod, String appVersion) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        String normalizedVersion = normalizeVersion(appVersion);

        LegacyUsersApiMetric metric = metricRepository
                .findByHitDateAndEndpointPathAndHttpMethodAndAppVersion(today, endpointPath, httpMethod, normalizedVersion)
                .orElseGet(() -> LegacyUsersApiMetric.builder()
                        .hitDate(today)
                        .endpointPath(endpointPath)
                        .httpMethod(httpMethod)
                        .appVersion(normalizedVersion)
                        .hitCount(0L)
                        .firstHitAt(now)
                        .lastHitAt(now)
                        .build());

        metric.setHitCount(metric.getHitCount() + 1);
        if (metric.getFirstHitAt() == null) {
            metric.setFirstHitAt(now);
        }
        metric.setLastHitAt(now);
        metricRepository.save(metric);
    }

    @Transactional(readOnly = true)
    public LegacyUsersMetricsDashboardDto getDashboard(int windowDays) {
        int normalizedWindowDays = Math.max(1, Math.min(windowDays, 90));
        LocalDate fromDate = LocalDate.now().minusDays(normalizedWindowDays - 1L);

        List<LegacyUsersVersionHitDto> byVersion = metricRepository.aggregateByVersion(fromDate);
        List<LegacyUsersDailyHitDto> byDay = metricRepository.aggregateByDay(fromDate);
        long totalHits = byVersion.stream().mapToLong(LegacyUsersVersionHitDto::getHitCount).sum();

        return LegacyUsersMetricsDashboardDto.builder()
                .windowDays(normalizedWindowDays)
                .totalHits(totalHits)
                .hitsByVersion(byVersion)
                .hitsByDay(byDay)
                .build();
    }

    private String normalizeVersion(String appVersion) {
        if (appVersion == null) {
            return "unknown";
        }
        String normalized = appVersion.trim();
        return normalized.isEmpty() ? "unknown" : normalized;
    }
}
