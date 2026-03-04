package com.example.mysoftpos_backend.controller;

import com.example.mysoftpos_backend.dto.TransactionSummaryDto;
import com.example.mysoftpos_backend.dto.TransactionSyncRequest;
import com.example.mysoftpos_backend.entity.User;
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

    /** Device pushes batch of transactions (User or Admin) */
    @PostMapping("/sync")
    public ResponseEntity<?> syncTransactions(@AuthenticationPrincipal User user,
                                              @RequestBody TransactionSyncRequest req) {
        int synced = txnService.syncTransactions(user, req);
        return ResponseEntity.ok(Map.of("syncedCount", synced));
    }

    /** Admin views all transactions */
    @GetMapping
    public ResponseEntity<List<TransactionSummaryDto>> getAll() {
        return ResponseEntity.ok(txnService.getAllTransactions());
    }

    /** Admin views by terminal */
    @GetMapping("/terminal/{code}")
    public ResponseEntity<List<TransactionSummaryDto>> getByTerminal(@PathVariable String code) {
        return ResponseEntity.ok(txnService.getByTerminal(code));
    }

    /** Admin views by user */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionSummaryDto>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(txnService.getByUser(userId));
    }
}
