package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.dto.LegacyUsersDailyHitDto;
import com.example.mysoftpos_backend.dto.LegacyUsersVersionHitDto;
import com.example.mysoftpos_backend.entity.LegacyUsersApiMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LegacyUsersApiMetricRepository extends JpaRepository<LegacyUsersApiMetric, Long> {

    Optional<LegacyUsersApiMetric> findByHitDateAndEndpointPathAndHttpMethodAndAppVersion(
            LocalDate hitDate,
            String endpointPath,
            String httpMethod,
            String appVersion);

    @Query("""
            select new com.example.mysoftpos_backend.dto.LegacyUsersVersionHitDto(m.appVersion, sum(m.hitCount))
            from LegacyUsersApiMetric m
            where m.hitDate >= :fromDate
            group by m.appVersion
            order by sum(m.hitCount) desc
            """)
    List<LegacyUsersVersionHitDto> aggregateByVersion(@Param("fromDate") LocalDate fromDate);

    @Query("""
            select new com.example.mysoftpos_backend.dto.LegacyUsersDailyHitDto(m.hitDate, sum(m.hitCount))
            from LegacyUsersApiMetric m
            where m.hitDate >= :fromDate
            group by m.hitDate
            order by m.hitDate asc
            """)
    List<LegacyUsersDailyHitDto> aggregateByDay(@Param("fromDate") LocalDate fromDate);
}
