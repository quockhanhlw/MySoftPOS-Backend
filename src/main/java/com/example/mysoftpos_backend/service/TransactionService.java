package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.TransactionSummaryDto;
import com.example.mysoftpos_backend.dto.TransactionSyncRequest;
import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.entity.TransactionSummary;
import com.example.mysoftpos_backend.entity.Terminal;
import com.example.mysoftpos_backend.repository.TransactionSummaryRepository;
import com.example.mysoftpos_backend.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionSummaryRepository txnRepo;
    private final TerminalRepository terminalRepo;
    private final SensitiveDataMaskingService maskingService;
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public int syncTransactions(PosAccount posAccount, TransactionSyncRequest req) {
        int synced = 0;
        if (req.getTransactions() == null) return 0;

        for (TransactionSyncRequest.TxnItem item : req.getTransactions()) {
            if (txnRepo.existsByTraceNumber(item.getTraceNumber())) continue;

            Long terminalId = item.getTerminalId();
            if (terminalId == null && item.getTerminalCode() != null && !item.getTerminalCode().isBlank()) {
                terminalId = terminalRepo.findByTerminalCode(item.getTerminalCode())
                        .map(Terminal::getId)
                        .orElse(null);
            }

            TransactionSummary txn = TransactionSummary.builder()
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
    public List<TransactionSummaryDto> getAllTransactions() {
        return txnRepo.findAllByOrderByTxnTimestampDesc().stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionSummaryDto> getByTerminal(String terminalCode) {
        Long terminalId = terminalRepo.findByTerminalCode(terminalCode).map(Terminal::getId).orElse(null);
        if (terminalId == null) {
            return java.util.Collections.emptyList();
        }
        return txnRepo.findByTerminalIdOrderByTxnTimestampDesc(terminalId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionSummaryDto> getByPosAccount(Long posAccountId) {
        return txnRepo.findByPosAccountIdOrderByTxnTimestampDesc(posAccountId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    /**
     * @deprecated Use {@link #getByPosAccount(Long)}.
     */
    @Deprecated
    public List<TransactionSummaryDto> getByUser(Long userId) {
        return getByPosAccount(userId);
    }

    private TransactionSummaryDto toDto(TransactionSummary t) {
        return TransactionSummaryDto.builder()
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
                .userId(t.getPosAccount() != null ? t.getPosAccount().getId() : null)
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
