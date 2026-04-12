package com.example.mysoftpos_backend.controller;

import com.example.mysoftpos_backend.dto.TransactionRecordDto;
import com.example.mysoftpos_backend.dto.TransactionSyncRequest;
import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Records", description = "APIs for syncing and querying transaction records")
public class TransactionController {

    private final TransactionService txnService;

    /** Device pushes batch of transaction records (POS account or admin) */
    @PostMapping("/sync")
    @Operation(summary = "Sync transaction records", description = "Accepts a batch of transaction records from device and stores new records")
    public ResponseEntity<?> syncTransactions(@AuthenticationPrincipal PosAccount posAccount,
                                              @RequestBody TransactionSyncRequest req) {
        int synced = txnService.syncTransactions(posAccount, req);
        return ResponseEntity.ok(Map.of("syncedCount", synced));
    }

    /** Admin views transaction records */
    @GetMapping
    @Operation(summary = "Get transaction records", description = "Returns transaction records scoped to current admin, with optional merchant/terminal filters")
    public ResponseEntity<List<TransactionRecordDto>> getAll(
            @AuthenticationPrincipal PosAccount admin,
            @RequestParam(name = "merchantId", required = false) Long merchantId,
            @RequestParam(name = "terminalId", required = false) Long terminalId) {
        return ResponseEntity.ok(txnService.getAllTransactions(admin.getId(), merchantId, terminalId));
    }

    /** Admin backfill: repair missing transaction record-account links (legacy data). */
    @PostMapping("/admin/backfill")
    @Operation(summary = "Backfill transaction records", description = "Repairs legacy transaction record links for current admin scope")
    public ResponseEntity<Map<String, Integer>> backfillAdminTransactions(
            @AuthenticationPrincipal PosAccount admin,
            @RequestParam(name = "merchantId", required = false) Long merchantId) {
        return ResponseEntity.ok(txnService.backfillAdminTransactions(admin.getId(), merchantId));
    }

    /** Admin views transaction records by terminal */
    @GetMapping("/terminal/{code}")
    @Operation(summary = "Get transaction records by terminal", description = "Returns transaction records for a terminal code in current admin scope")
    public ResponseEntity<List<TransactionRecordDto>> getByTerminal(@AuthenticationPrincipal PosAccount admin,
                                                                     @PathVariable String code) {
        return ResponseEntity.ok(txnService.getByTerminal(admin.getId(), code));
    }

    /** Admin views transaction records by POS account (canonical route). */
    @GetMapping("/pos-accounts/{posAccountId}")
    @Operation(summary = "Get transaction records by POS account", description = "Returns transaction records for a POS account in current admin scope")
    public ResponseEntity<List<TransactionRecordDto>> getByPosAccount(@AuthenticationPrincipal PosAccount admin,
                                                                       @PathVariable Long posAccountId) {
        return ResponseEntity.ok(txnService.getByPosAccount(admin.getId(), posAccountId));
    }

}
