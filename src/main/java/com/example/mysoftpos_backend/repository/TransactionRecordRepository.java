package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.TransactionRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {
    Optional<TransactionRecord> findByTraceNumber(String traceNumber);
    boolean existsByTraceNumber(String traceNumber);
    boolean existsByTraceNumberAndPosAccountId(String traceNumber, Long posAccountId);

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionRecord> findByPosAccountIdOrderByTxnTimestampDesc(Long posAccountId);


    @EntityGraph(attributePaths = "posAccount")
    List<TransactionRecord> findByTerminalIdOrderByTxnTimestampDesc(Long terminalId);

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionRecord> findAllByOrderByTxnTimestampDesc();

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionRecord> findByPosAccountAdminIdOrderByTxnTimestampDesc(Long adminId);

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionRecord> findByPosAccountAdminIdAndPosAccountMerchantIdOrderByTxnTimestampDesc(Long adminId,
                                                                                                     Long merchantId);

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionRecord> findByPosAccountAdminIdAndTerminalIdOrderByTxnTimestampDesc(Long adminId, Long terminalId);

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionRecord> findByPosAccountAdminIdAndPosAccountMerchantIdAndTerminalIdOrderByTxnTimestampDesc(
            Long adminId,
            Long merchantId,
            Long terminalId);

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionRecord> findByPosAccountIdAndPosAccountAdminIdOrderByTxnTimestampDesc(Long posAccountId, Long adminId);

    @EntityGraph(attributePaths = "posAccount")
    @Query("""
            SELECT t
            FROM TransactionRecord t
            JOIN Merchant m ON m.id = t.posAccount.merchantId
            WHERE m.adminId = :adminId
            ORDER BY t.txnTimestamp DESC
            """)
    List<TransactionRecord> findByMerchantOwnerAdminIdOrderByTxnTimestampDesc(@Param("adminId") Long adminId);

    @EntityGraph(attributePaths = "posAccount")
    @Query("""
            SELECT t
            FROM TransactionRecord t
            JOIN Merchant m ON m.id = t.posAccount.merchantId
            WHERE m.adminId = :adminId
              AND m.id = :merchantId
            ORDER BY t.txnTimestamp DESC
            """)
    List<TransactionRecord> findByMerchantOwnerAdminIdAndMerchantIdOrderByTxnTimestampDesc(@Param("adminId") Long adminId,
                                                                                              @Param("merchantId") Long merchantId);

    @EntityGraph(attributePaths = "posAccount")
    @Query("""
            SELECT t
            FROM TransactionRecord t
            JOIN Merchant m ON m.id = t.posAccount.merchantId
            WHERE m.adminId = :adminId
              AND t.terminalId = :terminalId
            ORDER BY t.txnTimestamp DESC
            """)
    List<TransactionRecord> findByMerchantOwnerAdminIdAndTerminalIdOrderByTxnTimestampDesc(@Param("adminId") Long adminId,
                                                                                              @Param("terminalId") Long terminalId);

    @EntityGraph(attributePaths = "posAccount")
    @Query("""
            SELECT t
            FROM TransactionRecord t
            JOIN Merchant m ON m.id = t.posAccount.merchantId
            WHERE m.adminId = :adminId
              AND m.id = :merchantId
              AND t.terminalId = :terminalId
            ORDER BY t.txnTimestamp DESC
            """)
    List<TransactionRecord> findByMerchantOwnerAdminIdAndMerchantIdAndTerminalIdOrderByTxnTimestampDesc(@Param("adminId") Long adminId,
                                                                                                            @Param("merchantId") Long merchantId,
                                                                                                            @Param("terminalId") Long terminalId);

    @EntityGraph(attributePaths = "posAccount")
    List<TransactionRecord> findByPosAccountIsNullAndTerminalIdInOrderByTxnTimestampDesc(List<Long> terminalIds);

    @Query("SELECT COUNT(t) FROM TransactionRecord t WHERE t.posAccount.adminId = :adminId")
    long countByAdminId(@Param("adminId") Long adminId);

    @Query("SELECT COUNT(t) FROM TransactionRecord t WHERE t.posAccount.adminId = :adminId AND t.posAccount.merchantId = :merchantId")
    long countByAdminIdAndMerchantId(@Param("adminId") Long adminId, @Param("merchantId") Long merchantId);
}

