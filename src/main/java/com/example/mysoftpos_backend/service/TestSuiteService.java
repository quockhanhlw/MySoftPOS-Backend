package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.TestCaseDto;
import com.example.mysoftpos_backend.dto.TestSuiteDto;
import com.example.mysoftpos_backend.entity.TestCase;
import com.example.mysoftpos_backend.entity.TestSuite;
import com.example.mysoftpos_backend.repository.TestCaseRepository;
import com.example.mysoftpos_backend.repository.TestSuiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestSuiteService {

    private final TestSuiteRepository suiteRepo;
    private final TestCaseRepository caseRepo;

    // ==================== Suites ====================

    public List<TestSuiteDto> getSuitesByAdmin(Long adminId) {
        return suiteRepo.findByAdminIdOrderByCreatedAtDesc(adminId).stream()
                .map(this::toSuiteDto)
                .collect(Collectors.toList());
    }

    public TestSuiteDto getSuiteWithCases(Long adminId, Long suiteId) {
        TestSuite suite = suiteRepo.findById(suiteId)
                .orElseThrow(() -> new RuntimeException("Suite not found"));
        if (!suite.getAdminId().equals(adminId)) {
            throw new RuntimeException("Access denied");
        }
        TestSuiteDto dto = toSuiteDto(suite);
        dto.setTestCases(caseRepo.findBySuiteIdOrderByCreatedAtDesc(suiteId).stream()
                .map(this::toCaseDto).collect(Collectors.toList()));
        return dto;
    }

    public TestSuiteDto createSuite(Long adminId, TestSuiteDto req) {
        TestSuite suite = TestSuite.builder()
                .name(req.getName())
                .description(req.getDescription())
                .adminId(adminId)
                .build();
        suiteRepo.save(suite);
        return toSuiteDto(suite);
    }

    public TestSuiteDto updateSuite(Long adminId, Long suiteId, TestSuiteDto req) {
        TestSuite suite = suiteRepo.findById(suiteId)
                .orElseThrow(() -> new RuntimeException("Suite not found"));
        if (!suite.getAdminId().equals(adminId)) {
            throw new RuntimeException("Access denied");
        }
        if (req.getName() != null) suite.setName(req.getName());
        if (req.getDescription() != null) suite.setDescription(req.getDescription());
        suiteRepo.save(suite);
        return toSuiteDto(suite);
    }

    @Transactional
    public void deleteSuite(Long adminId, Long suiteId) {
        TestSuite suite = suiteRepo.findById(suiteId)
                .orElseThrow(() -> new RuntimeException("Suite not found"));
        if (!suite.getAdminId().equals(adminId)) {
            throw new RuntimeException("Access denied");
        }
        caseRepo.deleteBySuiteId(suiteId);
        suiteRepo.delete(suite);
    }

    // ==================== Cases ====================

    public List<TestCaseDto> getCasesBySuite(Long adminId, Long suiteId) {
        TestSuite suite = suiteRepo.findById(suiteId)
                .orElseThrow(() -> new RuntimeException("Suite not found"));
        if (!suite.getAdminId().equals(adminId)) {
            throw new RuntimeException("Access denied");
        }
        return caseRepo.findBySuiteIdOrderByCreatedAtDesc(suiteId).stream()
                .map(this::toCaseDto).collect(Collectors.toList());
    }

    public TestCaseDto createCase(Long adminId, Long suiteId, TestCaseDto req) {
        TestSuite suite = suiteRepo.findById(suiteId)
                .orElseThrow(() -> new RuntimeException("Suite not found"));
        if (!suite.getAdminId().equals(adminId)) {
            throw new RuntimeException("Access denied");
        }
        TestCase tc = TestCase.builder()
                .suite(suite)
                .name(req.getName())
                .transactionType(req.getTransactionType())
                .status(req.getStatus())
                .amount(req.getAmount())
                .de22(req.getDe22())
                .maskedPan(req.getMaskedPan())
                .expiry(req.getExpiry())
                .track2(req.getTrack2())
                .scheme(req.getScheme())
                .fieldConfigJson(req.getFieldConfigJson())
                .build();
        caseRepo.save(tc);
        return toCaseDto(tc);
    }

    public TestCaseDto updateCase(Long adminId, Long caseId, TestCaseDto req) {
        TestCase tc = caseRepo.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));
        if (!tc.getSuite().getAdminId().equals(adminId)) {
            throw new RuntimeException("Access denied");
        }
        if (req.getName() != null) tc.setName(req.getName());
        if (req.getTransactionType() != null) tc.setTransactionType(req.getTransactionType());
        if (req.getStatus() != null) tc.setStatus(req.getStatus());
        if (req.getAmount() != null) tc.setAmount(req.getAmount());
        if (req.getDe22() != null) tc.setDe22(req.getDe22());
        if (req.getMaskedPan() != null) tc.setMaskedPan(req.getMaskedPan());
        if (req.getExpiry() != null) tc.setExpiry(req.getExpiry());
        if (req.getTrack2() != null) tc.setTrack2(req.getTrack2());
        if (req.getScheme() != null) tc.setScheme(req.getScheme());
        if (req.getFieldConfigJson() != null) tc.setFieldConfigJson(req.getFieldConfigJson());
        caseRepo.save(tc);
        return toCaseDto(tc);
    }

    public void deleteCase(Long adminId, Long caseId) {
        TestCase tc = caseRepo.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));
        if (!tc.getSuite().getAdminId().equals(adminId)) {
            throw new RuntimeException("Access denied");
        }
        caseRepo.delete(tc);
    }

    // ==================== Bulk Sync (push from app) ====================

    @Transactional
    public int bulkSync(Long adminId, List<TestSuiteDto> suiteDtos) {
        int count = 0;
        for (TestSuiteDto suiteDto : suiteDtos) {
            TestSuite suite;
            if (suiteDto.getId() != null) {
                suite = suiteRepo.findById(suiteDto.getId()).orElse(null);
                if (suite != null && !suite.getAdminId().equals(adminId)) continue;
            } else {
                suite = null;
            }
            if (suite == null) {
                suite = TestSuite.builder()
                        .name(suiteDto.getName())
                        .description(suiteDto.getDescription())
                        .adminId(adminId)
                        .build();
                suiteRepo.save(suite);
            } else {
                if (suiteDto.getName() != null) suite.setName(suiteDto.getName());
                if (suiteDto.getDescription() != null) suite.setDescription(suiteDto.getDescription());
                suiteRepo.save(suite);
            }

            if (suiteDto.getTestCases() != null) {
                for (TestCaseDto caseDto : suiteDto.getTestCases()) {
                    TestCase tc = TestCase.builder()
                            .suite(suite)
                            .name(caseDto.getName())
                            .transactionType(caseDto.getTransactionType())
                            .status(caseDto.getStatus())
                            .amount(caseDto.getAmount())
                            .de22(caseDto.getDe22())
                            .maskedPan(caseDto.getMaskedPan())
                            .expiry(caseDto.getExpiry())
                            .track2(caseDto.getTrack2())
                            .scheme(caseDto.getScheme())
                            .fieldConfigJson(caseDto.getFieldConfigJson())
                            .build();
                    caseRepo.save(tc);
                    count++;
                }
            }
            count++;
        }
        return count;
    }

    // ==================== DTO Mappers ====================

    private TestSuiteDto toSuiteDto(TestSuite s) {
        return TestSuiteDto.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .adminId(s.getAdminId())
                .createdAt(s.getCreatedAt() != null ? s.getCreatedAt().toString() : null)
                .testCases(Collections.emptyList())
                .build();
    }

    private TestCaseDto toCaseDto(TestCase tc) {
        return TestCaseDto.builder()
                .id(tc.getId())
                .suiteId(tc.getSuite() != null ? tc.getSuite().getId() : null)
                .name(tc.getName())
                .transactionType(tc.getTransactionType())
                .status(tc.getStatus())
                .amount(tc.getAmount())
                .de22(tc.getDe22())
                .maskedPan(tc.getMaskedPan())
                .expiry(tc.getExpiry())
                .track2(tc.getTrack2())
                .scheme(tc.getScheme())
                .fieldConfigJson(tc.getFieldConfigJson())
                .createdAt(tc.getCreatedAt() != null ? tc.getCreatedAt().toString() : null)
                .build();
    }
}

