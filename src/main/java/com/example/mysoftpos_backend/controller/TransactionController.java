package com.example.mysoftpos_backend.controller;

import com.example.mysoftpos_backend.dto.TransactionSummaryDto;
import com.example.mysoftpos_backend.dto.TransactionSyncRequest;
import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService txnService;

    /** Device pushes batch of transactions (POS account or admin) */
    @PostMapping("/sync")
    public ResponseEntity<?> syncTransactions(@AuthenticationPrincipal PosAccount posAccount,
                                              @RequestBody TransactionSyncRequest req) {
        int synced = txnService.syncTransactions(posAccount, req);
        return ResponseEntity.ok(Map.of("syncedCount", synced));
    }

    /** Admin views all transactions */
    @GetMapping
    public ResponseEntity<List<TransactionSummaryDto>> getAll(
            @AuthenticationPrincipal PosAccount admin,
            @RequestParam(name = "merchantId", required = false) Long merchantId,
            @RequestParam(name = "terminalId", required = false) Long terminalId) {
        return ResponseEntity.ok(txnService.getAllTransactions(admin.getId(), merchantId, terminalId));
    }

    /** Admin views by terminal */
    @GetMapping("/terminal/{code}")
    public ResponseEntity<List<TransactionSummaryDto>> getByTerminal(@AuthenticationPrincipal PosAccount admin,
                                                                     @PathVariable String code) {
        return ResponseEntity.ok(txnService.getByTerminal(admin.getId(), code));
    }

    /** Admin views by POS account (canonical route). */
    @GetMapping("/pos-accounts/{posAccountId}")
    public ResponseEntity<List<TransactionSummaryDto>> getByPosAccount(@AuthenticationPrincipal PosAccount admin,
                                                                       @PathVariable Long posAccountId) {
        return ResponseEntity.ok(txnService.getByPosAccount(admin.getId(), posAccountId));
    }

}
