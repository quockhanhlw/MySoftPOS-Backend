package com.example.mysoftpos_backend;

import com.example.mysoftpos_backend.dto.CreatePosAccountRequest;
import com.example.mysoftpos_backend.dto.PosAccountDto;
import com.example.mysoftpos_backend.dto.TransactionSummaryDto;
import com.example.mysoftpos_backend.entity.Merchant;
import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.entity.Terminal;
import com.example.mysoftpos_backend.entity.TransactionSummary;
import com.example.mysoftpos_backend.repository.MerchantRepository;
import com.example.mysoftpos_backend.repository.PosAccountRepository;
import com.example.mysoftpos_backend.repository.TerminalRepository;
import com.example.mysoftpos_backend.repository.TransactionSummaryRepository;
import com.example.mysoftpos_backend.service.PosAccountServiceCore;
import com.example.mysoftpos_backend.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BackendCrudCriticalIT {

    @Autowired
    private PosAccountRepository posAccountRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private TerminalRepository terminalRepository;

    @Autowired
    private TransactionSummaryRepository transactionSummaryRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private PosAccountServiceCore posAccountServiceCore;

    @Test
    void transactions_are_scoped_by_admin() {
        PosAccount adminA = saveAdmin("admin-a");
        PosAccount adminB = saveAdmin("admin-b");

        Merchant merchantA = saveMerchant(adminA.getId(), "MADMINA000001");
        Merchant merchantB = saveMerchant(adminB.getId(), "MADMINB000001");

        PosAccount accountA = saveUser(adminA.getId(), merchantA.getId(), "user-a1");
        PosAccount accountB = saveUser(adminB.getId(), merchantB.getId(), "user-b1");

        Terminal terminalA = saveTerminal(merchantA, accountA.getId(), "TIDA0001", "10.0.0.1", 5001);
        Terminal terminalB = saveTerminal(merchantB, accountB.getId(), "TIDB0001", "10.0.0.2", 5002);

        saveTransaction(accountA, terminalA.getId(), "TRACE-A-001");
        saveTransaction(accountB, terminalB.getId(), "TRACE-B-001");

        List<TransactionSummaryDto> adminAAll = transactionService.getAllTransactions(adminA.getId());
        assertThat(adminAAll).extracting(TransactionSummaryDto::getTraceNumber)
                .containsExactly("TRACE-A-001");

        List<TransactionSummaryDto> adminAByPosAccount = transactionService.getByPosAccount(adminA.getId(), accountB.getId());
        assertThat(adminAByPosAccount).isEmpty();

        List<TransactionSummaryDto> adminAByTerminal = transactionService.getByTerminal(adminA.getId(), terminalB.getTerminalCode());
        assertThat(adminAByTerminal).isEmpty();
    }

    @Test
    void delete_pos_account_cleans_terminal_mapping() {
        PosAccount admin = saveAdmin("admin-cleanup");
        Merchant merchant = saveMerchant(admin.getId(), "MCLEANUP000001");
        PosAccount account = saveUser(admin.getId(), merchant.getId(), "user-cleanup");
        Terminal terminal = saveTerminal(merchant, account.getId(), "CLNUP001", "172.16.1.10", 6000);

        posAccountServiceCore.deletePosAccount(admin.getId(), account.getId());

        assertThat(posAccountRepository.findById(account.getId())).isEmpty();
        Terminal afterDelete = terminalRepository.findById(terminal.getId()).orElseThrow();
        assertThat(afterDelete.getPosAccountId()).isNull();
    }

    @Test
    void pos_account_contract_phone_matches_username_for_child_accounts() {
        PosAccount admin = saveAdmin("admin-contract");

        PosAccountDto primary = posAccountServiceCore.createPosAccount(admin.getId(),
                request("user-primary", "Password123", "Owner Name", null));

        PosAccountDto child = posAccountServiceCore.createPosAccount(admin.getId(),
                request("user-child-1", "Password123", "Child Name", primary.getMerchantId()));

        assertThat(child.getUsername()).isEqualTo("user-child-1");
        assertThat(child.getPhone()).isEqualTo("user-child-1");

        List<PosAccountDto> byAdmin = posAccountServiceCore.getPosAccountsByAdmin(admin.getId());
        PosAccountDto childFromList = byAdmin.stream()
                .filter(item -> item.getId().equals(child.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(childFromList.getPhone()).isEqualTo(childFromList.getUsername());

        PosAccountDto updated = posAccountServiceCore.updatePosAccount(admin.getId(), child.getId(),
                request("user-child-2", null, "Child Name 2", primary.getMerchantId()));
        assertThat(updated.getUsername()).isEqualTo("user-child-2");
        assertThat(updated.getPhone()).isEqualTo("user-child-2");
    }

    private CreatePosAccountRequest request(String phone, String password, String fullName, Long merchantId) {
        CreatePosAccountRequest req = new CreatePosAccountRequest();
        req.setPhone(phone);
        req.setPassword(password);
        req.setFullName(fullName);
        req.setEmail(phone + "@example.com");
        req.setStoreName("Store " + phone);
        req.setBankName("VCB01");
        req.setMerchantId(merchantId);
        return req;
    }

    private PosAccount saveAdmin(String username) {
        return posAccountRepository.save(PosAccount.builder()
                .username(username)
                .passwordHash("hash")
                .role("ADMIN")
                .phoneVerified(true)
                .active(true)
                .build());
    }

    private Merchant saveMerchant(Long adminId, String merchantCode) {
        String phoneSuffix = String.valueOf(Math.abs(merchantCode.hashCode()));
        if (phoneSuffix.length() > 8) {
            phoneSuffix = phoneSuffix.substring(phoneSuffix.length() - 8);
        }
        return merchantRepository.save(Merchant.builder()
                .merchantCode(merchantCode)
                .merchantName("Merchant " + merchantCode)
                .fullName("Owner " + merchantCode)
                .phone("+84" + phoneSuffix)
                .email(merchantCode.toLowerCase() + "@example.com")
                .bankName("VCB01")
                .adminId(adminId)
                .build());
    }

    private PosAccount saveUser(Long adminId, Long merchantId, String username) {
        return posAccountRepository.save(PosAccount.builder()
                .username(username)
                .passwordHash("hash")
                .role("USER")
                .phoneVerified(true)
                .active(true)
                .adminId(adminId)
                .merchantId(merchantId)
                .terminalId(null)
                .build());
    }

    private Terminal saveTerminal(Merchant merchant,
                                  Long posAccountId,
                                  String terminalCode,
                                  String serverIp,
                                  Integer serverPort) {
        return terminalRepository.save(Terminal.builder()
                .terminalCode(terminalCode)
                .merchant(merchant)
                .posAccountId(posAccountId)
                .serverIp(serverIp)
                .serverPort(serverPort)
                .build());
    }

    private void saveTransaction(PosAccount account, Long terminalId, String traceNumber) {
        transactionSummaryRepository.save(TransactionSummary.builder()
                .traceNumber(traceNumber)
                .amount("1000")
                .status("APPROVED")
                .maskedPan("412345******2345")
                .cardScheme("VISA")
                .terminalId(terminalId)
                .posAccount(account)
                .txnTimestamp(LocalDateTime.now())
                .build());
    }
}

