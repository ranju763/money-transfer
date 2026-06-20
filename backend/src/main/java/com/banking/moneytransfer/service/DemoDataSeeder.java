package com.banking.moneytransfer.service;

import com.banking.moneytransfer.dto.TransferRequest;
import com.banking.moneytransfer.model.entity.Promotion;
import com.banking.moneytransfer.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Seeds realistic demo activity for the primary demo account (1000-1000-1001)
 * so that, on a fresh start, a logged-in user can immediately explore every
 * feature: a transaction history (sent, received, one failed), earned SwiftCoins,
 * and redeemed coupons.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DemoDataSeeder {

    private static final String DEMO = "1000-1000-1001";

    private final TransferService transferService;
    private final PromotionService promotionService;
    private final PromotionRepository promotionRepository;

    public void seed() {
        // Money received by the demo account (builds balance + "received" history)
        transfer("1000-1000-1003", DEMO, 15000);
        transfer("1000-1000-1006", DEMO, 20000);
        transfer("1000-1000-1004", DEMO, 8000);
        transfer("1000-1000-1008", DEMO, 7500);
        transfer("1000-1000-1002", DEMO, 5000);

        // Money sent by the demo account (earns SwiftCoins: 1 per ₹100)
        transfer(DEMO, "1000-1000-1006", 30000);
        transfer(DEMO, "1000-1000-1004", 20000);

        // One failed transfer (to a LOCKED account) so the history shows a failure too
        transfer(DEMO, "1000-1000-1005", 2000);

        // Redeem the two cheapest promotions for the demo account
        List<Promotion> promos = promotionRepository.findByActiveTrueOrderByCoinCostAsc();
        for (int i = 0; i < Math.min(2, promos.size()); i++) {
            try {
                promotionService.redeem(DEMO, promos.get(i).getId());
            } catch (Exception e) {
                log.warn("Demo redeem skipped: {}", e.getMessage());
            }
        }

        log.info("Seeded demo activity for {}", DEMO);
    }

    private void transfer(String from, String to, double amount) {
        try {
            transferService.transfer(TransferRequest.builder()
                    .fromAccountId(from)
                    .toAccountId(to)
                    .amount(BigDecimal.valueOf(amount))
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build());
        } catch (Exception e) {
            // Expected for the intentional failed transfer to a locked account.
            log.debug("Demo transfer {} -> {} not completed: {}", from, to, e.getMessage());
        }
    }
}
