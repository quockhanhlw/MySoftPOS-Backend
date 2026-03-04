package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.TransactionSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionSummaryRepository extends JpaRepository<TransactionSummary, Long> {
    Optional<TransactionSummary> findByTraceNumber(String traceNumber);
    boolean existsByTraceNumber(String traceNumber);
    List<TransactionSummary> findByUserIdOrderByTxnTimestampDesc(Long userId);
    List<TransactionSummary> findByTerminalCodeOrderByTxnTimestampDesc(String terminalCode);
    List<TransactionSummary> findAllByOrderByTxnTimestampDesc();
}
