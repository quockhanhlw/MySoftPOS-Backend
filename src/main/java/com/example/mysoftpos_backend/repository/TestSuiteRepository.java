package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.TestSuite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestSuiteRepository extends JpaRepository<TestSuite, Long> {
    List<TestSuite> findByAdminIdOrderByCreatedAtDesc(Long adminId);
}

