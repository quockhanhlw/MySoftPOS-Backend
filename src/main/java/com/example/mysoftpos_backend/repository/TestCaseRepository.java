package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findBySuiteIdOrderByCreatedAtDesc(Long suiteId);
    java.util.Optional<TestCase> findBySuiteIdAndNameAndDe22(Long suiteId, String name, String de22);
    void deleteBySuiteId(Long suiteId);
}

