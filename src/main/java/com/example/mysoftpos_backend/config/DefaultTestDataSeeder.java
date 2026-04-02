package com.example.mysoftpos_backend.config;

import com.example.mysoftpos_backend.entity.Card;
import com.example.mysoftpos_backend.entity.TestCase;
import com.example.mysoftpos_backend.entity.TestSuite;
import com.example.mysoftpos_backend.repository.CardRepository;
import com.example.mysoftpos_backend.repository.TestCaseRepository;
import com.example.mysoftpos_backend.repository.TestSuiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Seeds global default test data (admin_id=0) so a fresh app install can pull
 * baseline suites/cases/cards immediately after login.
 */
@Component
@RequiredArgsConstructor
public class DefaultTestDataSeeder implements ApplicationRunner {

    private static final Long GLOBAL_ADMIN_ID = 0L;

    private final TestSuiteRepository testSuiteRepository;
    private final TestCaseRepository testCaseRepository;
    private final CardRepository cardRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedDefaultSuitesAndCases();
        seedDefaultCards();
    }

    private void seedDefaultSuitesAndCases() {
        List<String> schemes = Arrays.asList("Napas", "Visa", "Mastercard");
        List<String> txnTypes = Arrays.asList("PURCHASE", "BALANCE");

        for (String scheme : schemes) {
            for (String txnType : txnTypes) {
                String suiteName = "Default " + scheme + " " + txnType;
                TestSuite suite = testSuiteRepository.findByAdminIdAndName(GLOBAL_ADMIN_ID, suiteName)
                        .orElseGet(() -> testSuiteRepository.save(TestSuite.builder()
                                .adminId(GLOBAL_ADMIN_ID)
                                .name(suiteName)
                                .description("System default suite for " + scheme + " " + txnType)
                                .build()));

                upsertDefaultCase(suite, txnType, scheme, "011", "Manual key-in with PIN",
                        "970416******9923", "3101", null);
                upsertDefaultCase(suite, txnType, scheme, "012", "Manual key-in without PIN",
                        "970430******5257", "3101", null);
                upsertDefaultCase(suite, txnType, scheme, "021", "Magstripe with PIN",
                        "970416******9923", "3101", "9704166606226219923=31016010000000123");
                upsertDefaultCase(suite, txnType, scheme, "022", "Magstripe without PIN",
                        "970430******5257", "3101", "9704306669144645257=31016010000000123");
                upsertDefaultCase(suite, txnType, scheme, "051", "Chip contact with PIN",
                        "970418******7647", "3101", null);
                upsertDefaultCase(suite, txnType, scheme, "052", "Chip contact without PIN",
                        "970418******7647", "3101", null);
                upsertDefaultCase(suite, txnType, scheme, "071", "Contactless with PIN",
                        "970418******7647", "2808", null);
                upsertDefaultCase(suite, txnType, scheme, "072", "Contactless without PIN",
                        "970418******7647", "2808", null);
            }
        }
    }

    private void upsertDefaultCase(TestSuite suite,
                                   String txnType,
                                   String scheme,
                                   String de22,
                                   String label,
                                   String maskedPan,
                                   String expiry,
                                   String track2) {
        String caseName = "Default DE22 " + de22 + " - " + label;
        TestCase tc = testCaseRepository.findBySuiteIdAndNameAndDe22(suite.getId(), caseName, de22)
                .orElseGet(() -> TestCase.builder()
                        .suite(suite)
                        .name(caseName)
                        .de22(de22)
                        .build());

        tc.setSuite(suite);
        tc.setName(caseName);
        tc.setTransactionType(txnType);
        tc.setStatus("READY");
        tc.setAmount("PURCHASE".equals(txnType) ? "000000010000" : "000000000000");
        tc.setMaskedPan(maskedPan);
        tc.setExpiry(expiry);
        tc.setScheme(scheme);
        tc.setTrack2(track2);
        tc.setFieldConfigJson("{}");
        tc.setIsDefault(Boolean.TRUE);

        testCaseRepository.save(tc);
    }

    private void seedDefaultCards() {
        upsertDefaultCard("970416******9923", "970416", "9923", "Napas");
        upsertDefaultCard("970430******5257", "970430", "5257", "Napas");
        upsertDefaultCard("970418******7647", "970418", "7647", "Napas");
    }

    private void upsertDefaultCard(String panMasked, String bin, String last4, String scheme) {
        Card card = cardRepository.findByAdminIdAndPanMasked(GLOBAL_ADMIN_ID, panMasked)
                .orElseGet(Card::new);
        card.setAdminId(GLOBAL_ADMIN_ID);
        card.setPosAccountId(0L);
        card.setPanMasked(panMasked);
        card.setBin(bin);
        card.setLast4(last4);
        card.setScheme(scheme);
        cardRepository.save(card);
    }
}

