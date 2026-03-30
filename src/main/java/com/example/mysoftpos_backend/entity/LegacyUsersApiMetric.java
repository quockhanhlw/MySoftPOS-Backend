package com.example.mysoftpos_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "legacy_users_api_metrics",
        uniqueConstraints = @UniqueConstraint(name = "uk_legacy_users_metric",
                columnNames = {"hit_date", "endpoint_path", "http_method", "app_version"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegacyUsersApiMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hit_date", nullable = false)
    private LocalDate hitDate;

    @Column(name = "endpoint_path", nullable = false, length = 120)
    private String endpointPath;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "app_version", nullable = false, length = 40)
    private String appVersion;

    @Column(name = "hit_count", nullable = false)
    @Builder.Default
    private long hitCount = 0L;

    @Column(name = "first_hit_at", nullable = false)
    private LocalDateTime firstHitAt;

    @Column(name = "last_hit_at", nullable = false)
    private LocalDateTime lastHitAt;
}
