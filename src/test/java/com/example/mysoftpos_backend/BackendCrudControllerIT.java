package com.example.mysoftpos_backend;

import com.example.mysoftpos_backend.controller.BranchController;
import com.example.mysoftpos_backend.controller.ConfigController;
import com.example.mysoftpos_backend.controller.PosAccountController;
import com.example.mysoftpos_backend.controller.TransactionController;
import com.example.mysoftpos_backend.dto.TransactionSummaryDto;
import com.example.mysoftpos_backend.entity.Branch;
import com.example.mysoftpos_backend.entity.Merchant;
import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.entity.Terminal;
import com.example.mysoftpos_backend.entity.TransactionSummary;
import com.example.mysoftpos_backend.repository.BranchRepository;
import com.example.mysoftpos_backend.repository.MerchantRepository;
import com.example.mysoftpos_backend.repository.PosAccountRepository;
import com.example.mysoftpos_backend.repository.TerminalRepository;
import com.example.mysoftpos_backend.repository.TransactionSummaryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BackendCrudControllerIT {

    @Autowired
    private TransactionController transactionController;

    @Autowired
    private PosAccountController posAccountController;

    @Autowired
    private ConfigController configController;

    @Autowired
    private BranchController branchController;

    @Autowired
    private PosAccountRepository posAccountRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private TerminalRepository terminalRepository;

    @Autowired
    private TransactionSummaryRepository transactionSummaryRepository;

    @Test
    void get_transactions_is_scoped_by_authenticated_admin() throws Exception {
        PosAccount adminA = saveAdmin("http-admin-a");
        PosAccount adminB = saveAdmin("http-admin-b");

        Merchant merchantA = saveMerchant(adminA.getId(), "MHTTPA000001");
        Merchant merchantB = saveMerchant(adminB.getId(), "MHTTPB000001");

        PosAccount accountA = saveUser(adminA.getId(), merchantA.getId(), "http-user-a");
        PosAccount accountB = saveUser(adminB.getId(), merchantB.getId(), "http-user-b");

        Terminal terminalA = saveTerminal(merchantA, accountA.getId(), "HTPA0001");
        Terminal terminalB = saveTerminal(merchantB, accountB.getId(), "HTPB0001");

        saveTransaction(accountA, terminalA.getId(), "HTTP-TRACE-A");
        saveTransaction(accountB, terminalB.getId(), "HTTP-TRACE-B");

        ResponseEntity<List<TransactionSummaryDto>> response = transactionController.getAll(adminA, null, null);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).extracting(TransactionSummaryDto::getTraceNumber)
                .contains("HTTP-TRACE-A")
                .doesNotContain("HTTP-TRACE-B");
    }

    @Test
    void delete_pos_account_clears_terminal_mapping_over_http() throws Exception {
        PosAccount admin = saveAdmin("http-admin-clean");
        Merchant merchant = saveMerchant(admin.getId(), "MHTTPC000001");
        PosAccount account = saveUser(admin.getId(), merchant.getId(), "http-user-clean");
        Terminal terminal = saveTerminal(merchant, account.getId(), "HTPC0001");

        ResponseEntity<?> response = posAccountController.deletePosAccount(admin, account.getId());
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Terminal afterDelete = terminalRepository.findById(terminal.getId()).orElseThrow();
        assertThat(afterDelete.getPosAccountId()).isNull();
    }

    @Test
    void delete_terminal_is_admin_scoped() throws Exception {
        PosAccount adminA = saveAdmin("http-admin-term-a");
        PosAccount adminB = saveAdmin("http-admin-term-b");

        Merchant merchantA = saveMerchant(adminA.getId(), "MHTPTA000001");
        Merchant merchantB = saveMerchant(adminB.getId(), "MHTPTB000001");

        Terminal ownTerminal = saveTerminal(merchantA, null, "HTPTA001");
        Terminal foreignTerminal = saveTerminal(merchantB, null, "HTPTB001");

        ResponseEntity<?> foreignDelete = configController.deleteTerminal(adminA, foreignTerminal.getId());
        assertThat(foreignDelete.getStatusCode().is4xxClientError()).isTrue();

        ResponseEntity<?> ownDelete = configController.deleteTerminal(adminA, ownTerminal.getId());
        assertThat(ownDelete.getStatusCode().is2xxSuccessful()).isTrue();

        assertThat(terminalRepository.findById(ownTerminal.getId())).isEmpty();
    }

    @Test
    void delete_branch_guardrails_are_enforced() throws Exception {
        PosAccount admin = saveAdmin("http-admin-branch");
        Merchant merchant = saveMerchant(admin.getId(), "MHTPB000001");

        Branch mainBranch = branchRepository.save(Branch.builder()
                .merchantId(merchant.getId())
                .branchCode("MAIN")
                .branchName("Main")
                .build());
        Branch removableBranch = branchRepository.save(Branch.builder()
                .merchantId(merchant.getId())
                .branchCode("B001")
                .branchName("Branch 1")
                .build());

        ResponseEntity<?> mainDelete = branchController.deleteBranch(admin, merchant.getId(), mainBranch.getId());
        assertThat(mainDelete.getStatusCode().is4xxClientError()).isTrue();

        ResponseEntity<?> removableDelete = branchController.deleteBranch(admin, merchant.getId(), removableBranch.getId());
        assertThat(removableDelete.getStatusCode().is2xxSuccessful()).isTrue();

        assertThat(branchRepository.findById(removableBranch.getId())).isEmpty();
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
        return merchantRepository.save(Merchant.builder()
                .merchantCode(merchantCode)
                .merchantName("Merchant " + merchantCode)
                .fullName("Owner " + merchantCode)
                .phone(uniquePhoneFor(merchantCode))
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
                .build());
    }

    private Terminal saveTerminal(Merchant merchant, Long posAccountId, String terminalCode) {
        return terminalRepository.save(Terminal.builder()
                .terminalCode(terminalCode)
                .merchant(merchant)
                .posAccountId(posAccountId)
                .serverIp("10.0.0.1")
                .serverPort(5000)
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

    private String uniquePhoneFor(String merchantCode) {
        String suffix = String.valueOf(Math.abs(merchantCode.hashCode()));
        if (suffix.length() > 8) {
            suffix = suffix.substring(suffix.length() - 8);
        }
        return "+84" + suffix;
    }
}

