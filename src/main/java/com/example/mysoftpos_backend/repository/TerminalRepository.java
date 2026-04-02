package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface TerminalRepository extends JpaRepository<Terminal, Long> {
    List<Terminal> findByMerchantId(Long merchantId);
    long countByMerchantIdAndBranchId(Long merchantId, Long branchId);
    List<Terminal> findByMerchantAdminId(Long adminId);
    long countByMerchantId(Long merchantId);
    Optional<Terminal> findFirstByPosAccountId(Long posAccountId);
    Optional<Terminal> findByTerminalCode(String terminalCode);
    void deleteByMerchantId(Long merchantId);
    boolean existsByTerminalCode(String terminalCode);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Terminal t SET t.posAccountId = null WHERE t.posAccountId = :posAccountId")
    int clearPosAccountMapping(@Param("posAccountId") Long posAccountId);
}
