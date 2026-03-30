package com.example.mysoftpos_backend.config;

import com.example.mysoftpos_backend.service.LegacyUsersApiMetricService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class LegacyUsersMetricsInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LegacyUsersMetricsInterceptor.class);

    private final LegacyUsersApiMetricService metricService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String appVersion = resolveAppVersion(request);
            metricService.recordHit(request.getRequestURI(), request.getMethod(), appVersion);
        } catch (Exception ex) {
            // Metrics must never block business flow.
            log.warn("Failed to record legacy /api/users metric: {}", ex.getMessage());
        }
        return true;
    }

    private String resolveAppVersion(HttpServletRequest request) {
        String directVersion = firstNonBlank(
                request.getHeader("X-App-Version"),
                request.getHeader("App-Version"),
                request.getHeader("X-Client-Version"));
        if (directVersion != null) {
            return directVersion;
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)mysoftpos[/\\s]([0-9]+(?:\\.[0-9]+){0,3})")
                .matcher(userAgent);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.trim().isEmpty()) {
                return candidate.trim();
            }
        }
        return null;
    }
}
