package com.example.mysoftpos_backend.controller;

import com.example.mysoftpos_backend.dto.CardDto;
import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping
    public ResponseEntity<List<CardDto>> getCards(@AuthenticationPrincipal PosAccount actor) {
        long adminScopeId = resolveAdminScopeId(actor);
        return ResponseEntity.ok(cardService.getCardsByAdmin(adminScopeId));
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Integer>> syncCards(@AuthenticationPrincipal PosAccount actor,
                                                          @RequestBody List<CardDto> cards) {
        long adminScopeId = resolveAdminScopeId(actor);
        int synced = cardService.syncCards(adminScopeId, actor != null ? actor.getId() : null, cards);
        return ResponseEntity.ok(Map.of("syncedCount", synced));
    }

    private long resolveAdminScopeId(PosAccount actor) {
        if (actor == null) {
            return 0L;
        }
        if ("ADMIN".equalsIgnoreCase(actor.getRole())) {
            return actor.getId() != null ? actor.getId() : 0L;
        }
        return actor.getAdminId() != null ? actor.getAdminId() : 0L;
    }
}

