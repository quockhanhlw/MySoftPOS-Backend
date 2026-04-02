package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.TransactionSummary;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TransactionSummaryRepository extends JpaRepository<TransactionSummary, Long> {
    Optional<TransactionSummary> findByTraceNumber(String traceNumber);
    boolean existsByTraceNumber(String traceNumber);
    boolean existsByTraceNumberAndPosAccountId(String traceNumber, Long posAccountId);

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionSummary> findByPosAccountIdOrderByTxnTimestampDesc(Long posAccountId);


    @EntityGraph(attributePaths = "posAccount")
    List<TransactionSummary> findByTerminalIdOrderByTxnTimestampDesc(Long terminalId);

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionSummary> findAllByOrderByTxnTimestampDesc();

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionSummary> findByPosAccountAdminIdOrderByTxnTimestampDesc(Long adminId);

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionSummary> findByPosAccountAdminIdAndPosAccountMerchantIdOrderByTxnTimestampDesc(Long adminId,
                                                                                                     Long merchantId);

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionSummary> findByPosAccountAdminIdAndTerminalIdOrderByTxnTimestampDesc(Long adminId, Long terminalId);

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionSummary> findByPosAccountAdminIdAndPosAccountMerchantIdAndTerminalIdOrderByTxnTimestampDesc(
            Long adminId,
            Long merchantId,
            Long terminalId);

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionSummary> findByPosAccountIdAndPosAccountAdminIdOrderByTxnTimestampDesc(Long posAccountId, Long adminId);

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionSummary> findByPosAccountIsNullAndTerminalIdInOrderByTxnTimestampDesc(List<Long> terminalIds);

    @Query("SELECT COUNT(t) FROM TransactionSummary t WHERE t.posAccount.adminId = :adminId")
    long countByAdminId(@Param("adminId") Long adminId);

    @Query("SELECT COUNT(t) FROM TransactionSummary t WHERE t.posAccount.adminId = :adminId AND t.posAccount.merchantId = :merchantId")
    long countByAdminIdAndMerchantId(@Param("adminId") Long adminId, @Param("merchantId") Long merchantId);
}
