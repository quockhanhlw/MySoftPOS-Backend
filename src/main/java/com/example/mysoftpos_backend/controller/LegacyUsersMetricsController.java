package com.example.mysoftpos_backend.controller;

import com.example.mysoftpos_backend.dto.LegacyUsersMetricsDashboardDto;
import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.service.LegacyUsersApiMetricService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pos-accounts/legacy-users-metrics")
@RequiredArgsConstructor
public class LegacyUsersMetricsController {

    private final LegacyUsersApiMetricService metricService;

    @GetMapping
    public ResponseEntity<LegacyUsersMetricsDashboardDto> getLegacyUsersDashboard(
            @AuthenticationPrincipal PosAccount admin,
            @RequestParam(name = "days", defaultValue = "14") int days) {
        // Authentication is enforced by security config; method keeps principal for audit symmetry.
        if (admin == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(metricService.getDashboard(days));
    }
}
