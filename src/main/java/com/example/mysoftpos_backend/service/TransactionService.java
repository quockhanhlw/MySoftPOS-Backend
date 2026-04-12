package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.TransactionRecordDto;
import com.example.mysoftpos_backend.dto.TransactionSyncRequest;
import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.entity.TransactionRecord;
import com.example.mysoftpos_backend.entity.Terminal;
import com.example.mysoftpos_backend.repository.PosAccountRepository;
import com.example.mysoftpos_backend.repository.TransactionRecordRepository;
import com.example.mysoftpos_backend.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRecordRepository txnRepo;
    private final TerminalRepository terminalRepo;
    private final PosAccountRepository posAccountRepo;
    private final SensitiveDataMaskingService maskingService;
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public int syncTransactions(PosAccount posAccount, TransactionSyncRequest req) {
        int synced = 0;
        if (req.getTransactions() == null) return 0;
        if (posAccount == null || posAccount.getId() == null) return 0;

        for (TransactionSyncRequest.TxnItem item : req.getTransactions()) {
            if (item.getTraceNumber() == null || item.getTraceNumber().isBlank()) continue;
            if (txnRepo.existsByTraceNumberAndPosAccountId(item.getTraceNumber(), posAccount.getId())) continue;

            Long terminalId = item.getTerminalId();
            if (terminalId == null && item.getTerminalCode() != null && !item.getTerminalCode().isBlank()) {
                terminalId = terminalRepo.findByTerminalCode(item.getTerminalCode())
                        .map(Terminal::getId)
                        .orElse(null);
            }

            TransactionRecord txn = TransactionRecord.builder()
                    .traceNumber(item.getTraceNumber())
                    .amount(item.getAmount())
                    .status(item.getStatus())
                    .maskedPan(item.getMaskedPan())
                    .cardScheme(item.getCardScheme())
                    .terminalId(terminalId)
                    .cardId(item.getCardId())
                    .requestHex(maskingService.maskIsoHex(item.getRequestHex()))
                    .responseHex(maskingService.maskIsoHex(item.getResponseHex()))
                    .processingCode(item.getProcessingCode())
                    .currencyCode(item.getCurrencyCode())
                    .rrn(item.getRrn())
                    .posAccount(posAccount)
                    .deviceId(item.getDeviceId())
                    .txnTimestamp(LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(item.getTxnTimestamp()), ZoneId.systemDefault()))
                    .build();
            txnRepo.save(txn);
            synced++;
        }
        return synced;
    }

    @Transactional(readOnly = true)
    public List<TransactionRecordDto> getAllTransactions(Long adminId, Long merchantId, Long terminalId) {
        List<TransactionRecord> rows;
        if (merchantId != null && terminalId != null) {
            rows = txnRepo.findByPosAccountAdminIdAndPosAccountMerchantIdAndTerminalIdOrderByTxnTimestampDesc(
                    adminId,
                    merchantId,
                    terminalId);
        } else if (merchantId != null) {
            rows = txnRepo.findByPosAccountAdminIdAndPosAccountMerchantIdOrderByTxnTimestampDesc(adminId, merchantId);
        } else if (terminalId != null) {
            rows = txnRepo.findByPosAccountAdminIdAndTerminalIdOrderByTxnTimestampDesc(adminId, terminalId);
        } else {
            rows = txnRepo.findByPosAccountAdminIdOrderByTxnTimestampDesc(adminId);
        }

        // Legacy compatibility: some historical pos_accounts may miss admin_id but still belong
        // to merchants owned by this admin. Merge these rows to avoid dropping transactions.
        List<TransactionRecord> merchantOwnedRows;
        if (merchantId != null && terminalId != null) {
            merchantOwnedRows = txnRepo.findByMerchantOwnerAdminIdAndMerchantIdAndTerminalIdOrderByTxnTimestampDesc(
                    adminId,
                    merchantId,
                    terminalId);
        } else if (merchantId != null) {
            merchantOwnedRows = txnRepo.findByMerchantOwnerAdminIdAndMerchantIdOrderByTxnTimestampDesc(adminId, merchantId);
        } else if (terminalId != null) {
            merchantOwnedRows = txnRepo.findByMerchantOwnerAdminIdAndTerminalIdOrderByTxnTimestampDesc(adminId, terminalId);
        } else {
            merchantOwnedRows = txnRepo.findByMerchantOwnerAdminIdOrderByTxnTimestampDesc(adminId);
        }

        rows = mergeDedupById(rows, merchantOwnedRows);
        rows = includeScopedOrphans(rows, adminId, merchantId, terminalId);
        return rows.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionRecordDto> getByTerminal(Long adminId, String terminalCode) {
        Long terminalId = terminalRepo.findByTerminalCode(terminalCode).map(Terminal::getId).orElse(null);
        if (terminalId == null) {
            return java.util.Collections.emptyList();
        }
        return txnRepo.findByPosAccountAdminIdAndTerminalIdOrderByTxnTimestampDesc(adminId, terminalId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionRecordDto> getByPosAccount(Long adminId, Long posAccountId) {
        return txnRepo.findByPosAccountIdAndPosAccountAdminIdOrderByTxnTimestampDesc(posAccountId, adminId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Integer> backfillAdminTransactions(Long adminId, Long merchantId) {
        List<PosAccount> accounts = (merchantId != null)
                ? posAccountRepo.findByAdminIdAndMerchantIdOrderByIdAsc(adminId, merchantId)
                : posAccountRepo.findByAdminId(adminId);
        if (accounts.isEmpty()) {
            return Map.of("relinked", 0, "scanned", 0, "totalScoped", 0);
        }

        Map<Long, PosAccount> accountById = new HashMap<>();
        for (PosAccount account : accounts) {
            accountById.put(account.getId(), account);
        }

        List<Terminal> adminTerminals = terminalRepo.findByMerchantAdminId(adminId);
        Map<Long, PosAccount> accountByTerminalId = new HashMap<>();
        for (Terminal terminal : adminTerminals) {
            if (terminal == null || terminal.getId() == null || terminal.getPosAccountId() == null) {
                continue;
            }
            PosAccount account = accountById.get(terminal.getPosAccountId());
            if (account == null) {
                continue;
            }
            if (merchantId != null && !merchantId.equals(account.getMerchantId())) {
                continue;
            }
            accountByTerminalId.put(terminal.getId(), account);
        }

        if (accountByTerminalId.isEmpty()) {
            int total = merchantId != null
                    ? (int) txnRepo.countByAdminIdAndMerchantId(adminId, merchantId)
                    : (int) txnRepo.countByAdminId(adminId);
            return Map.of("relinked", 0, "scanned", 0, "totalScoped", total);
        }

        List<Long> terminalIds = accountByTerminalId.keySet().stream().toList();
        List<TransactionRecord> orphans = txnRepo.findByPosAccountIsNullAndTerminalIdInOrderByTxnTimestampDesc(terminalIds);

        int relinked = 0;
        for (TransactionRecord txn : orphans) {
            PosAccount mapped = accountByTerminalId.get(txn.getTerminalId());
            if (mapped == null) {
                continue;
            }
            txn.setPosAccount(mapped);
            relinked++;
        }

        if (relinked > 0) {
            txnRepo.saveAll(orphans);
        }

        int total = merchantId != null
                ? (int) txnRepo.countByAdminIdAndMerchantId(adminId, merchantId)
                : (int) txnRepo.countByAdminId(adminId);
        return Map.of("relinked", relinked, "scanned", orphans.size(), "totalScoped", total);
    }

    private List<TransactionRecord> includeScopedOrphans(List<TransactionRecord> scoped,
                                                          Long adminId,
                                                          Long merchantId,
                                                          Long terminalId) {
        List<Terminal> adminTerminals = terminalRepo.findByMerchantAdminId(adminId);
        if (adminTerminals == null || adminTerminals.isEmpty()) {
            return scoped;
        }

        List<Long> scopedTerminalIds = adminTerminals.stream()
                .filter(t -> t != null && t.getId() != null)
                .filter(t -> merchantId == null || (t.getMerchant() != null && merchantId.equals(t.getMerchant().getId())))
                .filter(t -> terminalId == null || terminalId.equals(t.getId()))
                .map(Terminal::getId)
                .toList();

        if (scopedTerminalIds.isEmpty()) {
            return scoped;
        }

        List<TransactionRecord> orphans = txnRepo.findByPosAccountIsNullAndTerminalIdInOrderByTxnTimestampDesc(scopedTerminalIds);
        if (orphans == null || orphans.isEmpty()) {
            return scoped;
        }

        List<TransactionRecord> merged = mergeDedupById(scoped, orphans);
        merged.sort(Comparator.comparing(TransactionRecord::getTxnTimestamp,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return merged;
    }

    private List<TransactionRecord> mergeDedupById(List<TransactionRecord> first,
                                                   List<TransactionRecord> second) {
        List<TransactionRecord> merged = new ArrayList<>((first != null ? first.size() : 0)
                + (second != null ? second.size() : 0));
        Set<Long> seenIds = new HashSet<>();

        if (first != null) {
            for (TransactionRecord row : first) {
                if (row != null && row.getId() != null && seenIds.add(row.getId())) {
                    merged.add(row);
                }
            }
        }

        if (second != null) {
            for (TransactionRecord row : second) {
                if (row != null && row.getId() != null && seenIds.add(row.getId())) {
                    merged.add(row);
                }
            }
        }

        return merged;
    }


    private TransactionRecordDto toDto(TransactionRecord t) {
        Long posAccountId = t.getPosAccount() != null ? t.getPosAccount().getId() : null;
        return TransactionRecordDto.builder()
                .id(t.getId())
                .traceNumber(t.getTraceNumber())
                .amount(t.getAmount())
                .status(t.getStatus())
                .maskedPan(t.getMaskedPan())
                .cardScheme(t.getCardScheme())
                .terminalId(t.getTerminalId())
                .cardId(t.getCardId())
                .terminalCode(resolveTerminalCode(t.getTerminalId()))
                .deviceId(t.getDeviceId())
                .txnTimestamp(t.getTxnTimestamp() != null ? t.getTxnTimestamp().format(ISO_FMT) : null)
                .syncedAt(t.getSyncedAt() != null ? t.getSyncedAt().format(ISO_FMT) : null)
                .posAccountId(posAccountId)
                .userId(posAccountId)
                .username(t.getPosAccount() != null ? t.getPosAccount().getUsername() : null)
                .requestHex(t.getRequestHex())
                .responseHex(t.getResponseHex())
                .processingCode(t.getProcessingCode())
                .currencyCode(t.getCurrencyCode())
                .rrn(t.getRrn())
                .build();
    }

    private String resolveTerminalCode(Long terminalId) {
        if (terminalId == null) {
            return null;
        }
        return terminalRepo.findById(terminalId).map(Terminal::getTerminalCode).orElse(null);
    }
}
