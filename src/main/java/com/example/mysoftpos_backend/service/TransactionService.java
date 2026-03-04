package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.TransactionSummaryDto;
import com.example.mysoftpos_backend.dto.TransactionSyncRequest;
import com.example.mysoftpos_backend.entity.TransactionSummary;
import com.example.mysoftpos_backend.entity.User;
import com.example.mysoftpos_backend.repository.TransactionSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public int syncTransactions(User user, TransactionSyncRequest req) {
        int synced = 0;
        if (req.getTransactions() == null) return 0;

        for (TransactionSyncRequest.TxnItem item : req.getTransactions()) {
            if (txnRepo.existsByTraceNumber(item.getTraceNumber())) continue;

            TransactionSummary txn = TransactionSummary.builder()
                    .traceNumber(item.getTraceNumber())
                    .amount(item.getAmount())
                    .status(item.getStatus())
                    .maskedPan(item.getMaskedPan())
                    .cardScheme(item.getCardScheme())
                    .terminalCode(item.getTerminalCode())
                    .user(user)
                    .deviceId(item.getDeviceId())
                    .txnTimestamp(LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(item.getTxnTimestamp()), ZoneId.systemDefault()))
                    .build();
            txnRepo.save(txn);
            synced++;
        }
        return synced;
    }

    public List<TransactionSummaryDto> getAllTransactions() {
        return txnRepo.findAllByOrderByTxnTimestampDesc().stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<TransactionSummaryDto> getByTerminal(String terminalCode) {
        return txnRepo.findByTerminalCodeOrderByTxnTimestampDesc(terminalCode).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<TransactionSummaryDto> getByUser(Long userId) {
        return txnRepo.findByUserIdOrderByTxnTimestampDesc(userId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    private TransactionSummaryDto toDto(TransactionSummary t) {
        return TransactionSummaryDto.builder()
                .id(t.getId())
                .traceNumber(t.getTraceNumber())
                .amount(t.getAmount())
                .status(t.getStatus())
                .maskedPan(t.getMaskedPan())
                .cardScheme(t.getCardScheme())
                .terminalCode(t.getTerminalCode())
                .deviceId(t.getDeviceId())
                .txnTimestamp(t.getTxnTimestamp() != null ? t.getTxnTimestamp().format(ISO_FMT) : null)
                .syncedAt(t.getSyncedAt() != null ? t.getSyncedAt().format(ISO_FMT) : null)
                .userId(t.getUser() != null ? t.getUser().getId() : null)
                .username(t.getUser() != null ? t.getUser().getUsername() : null)
                .build();
    }
}
