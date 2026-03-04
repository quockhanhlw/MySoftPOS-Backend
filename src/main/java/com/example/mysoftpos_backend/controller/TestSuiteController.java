package com.example.mysoftpos_backend.controller;

import com.example.mysoftpos_backend.dto.TestCaseDto;
import com.example.mysoftpos_backend.dto.TestSuiteDto;
import com.example.mysoftpos_backend.entity.User;
import com.example.mysoftpos_backend.service.TestSuiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test-suites")
@RequiredArgsConstructor
public class TestSuiteController {

    private final TestSuiteService service;

    // ==================== Suites ====================

    @GetMapping
    public ResponseEntity<List<TestSuiteDto>> getSuites(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(service.getSuitesByAdmin(admin.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSuiteWithCases(@AuthenticationPrincipal User admin,
                                                @PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getSuiteWithCases(admin.getId(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createSuite(@AuthenticationPrincipal User admin,
                                          @RequestBody TestSuiteDto req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(service.createSuite(admin.getId(), req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSuite(@AuthenticationPrincipal User admin,
                                          @PathVariable Long id,
                                          @RequestBody TestSuiteDto req) {
        try {
            return ResponseEntity.ok(service.updateSuite(admin.getId(), id, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSuite(@AuthenticationPrincipal User admin,
                                          @PathVariable Long id) {
        try {
            service.deleteSuite(admin.getId(), id);
            return ResponseEntity.ok(Map.of("message", "Suite deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Cases ====================

    @GetMapping("/{suiteId}/cases")
    public ResponseEntity<?> getCases(@AuthenticationPrincipal User admin,
                                       @PathVariable Long suiteId) {
        try {
            return ResponseEntity.ok(service.getCasesBySuite(admin.getId(), suiteId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{suiteId}/cases")
    public ResponseEntity<?> createCase(@AuthenticationPrincipal User admin,
                                         @PathVariable Long suiteId,
                                         @RequestBody TestCaseDto req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(service.createCase(admin.getId(), suiteId, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/cases/{caseId}")
    public ResponseEntity<?> updateCase(@AuthenticationPrincipal User admin,
                                         @PathVariable Long caseId,
                                         @RequestBody TestCaseDto req) {
        try {
            return ResponseEntity.ok(service.updateCase(admin.getId(), caseId, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/cases/{caseId}")
    public ResponseEntity<?> deleteCase(@AuthenticationPrincipal User admin,
                                         @PathVariable Long caseId) {
        try {
            service.deleteCase(admin.getId(), caseId);
            return ResponseEntity.ok(Map.of("message", "Case deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Bulk Sync (push from app) ====================

    @PostMapping("/sync")
    public ResponseEntity<?> bulkSync(@AuthenticationPrincipal User admin,
                                       @RequestBody List<TestSuiteDto> suites) {
        try {
            int count = service.bulkSync(admin.getId(), suites);
            return ResponseEntity.ok(Map.of("syncedCount", count));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

