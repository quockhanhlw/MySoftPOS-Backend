package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.TransactionSummary;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionSummaryRepository extends JpaRepository<TransactionSummary, Long> {
    Optional<TransactionSummary> findByTraceNumber(String traceNumber);
    boolean existsByTraceNumber(String traceNumber);

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
}
