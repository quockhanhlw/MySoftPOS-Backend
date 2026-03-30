package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.PosAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Canonical repository name for pos_accounts table.
 * PosAccount is the canonical domain model.
 */
public interface PosAccountRepository extends JpaRepository<PosAccount, Long> {
    Optional<PosAccount> findByUsername(String username);

    boolean existsByUsername(String username);


    boolean existsByRole(String role);

    List<PosAccount> findByAdminId(Long adminId);

    List<PosAccount> findByAdminIdAndRole(Long adminId, String role);

    List<PosAccount> findByAdminIdAndMerchantIdOrderByIdAsc(Long adminId, Long merchantId);

    List<PosAccount> findByAdminIdAndMerchantIdAndBranchIdOrderByIdAsc(Long adminId, Long merchantId, Long branchId);

    List<PosAccount> findByMerchantIdAndBranchId(Long merchantId, Long branchId);

    long countByAdminIdAndMerchantId(Long adminId, Long merchantId);

    Optional<PosAccount> findFirstByRoleOrderByIdAsc(String role);

    @Transactional
    @Modifying
    @Query("UPDATE PosAccount u SET u.lastActiveAt = :lastActiveAt WHERE u.id = :id")
    void updateLastActiveAt(@Param("id") Long id, @Param("lastActiveAt") LocalDateTime lastActiveAt);
}

