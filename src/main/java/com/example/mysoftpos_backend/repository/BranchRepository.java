package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByMerchantIdOrderByIdAsc(Long merchantId);

    Optional<Branch> findByMerchantIdAndBranchCode(Long merchantId, String branchCode);

    long countByMerchantId(Long merchantId);

    boolean existsByMerchantIdAndBranchCode(Long merchantId, String branchCode);
}

