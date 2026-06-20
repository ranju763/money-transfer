package com.banking.moneytransfer.service;

import com.banking.moneytransfer.dto.PromotionResponse;
import com.banking.moneytransfer.dto.RedemptionResponse;
import com.banking.moneytransfer.exception.AccountNotFoundException;
import com.banking.moneytransfer.exception.InsufficientCoinsException;
import com.banking.moneytransfer.exception.PromotionNotFoundException;
import com.banking.moneytransfer.model.entity.Account;
import com.banking.moneytransfer.model.entity.Promotion;
import com.banking.moneytransfer.model.entity.Redemption;
import com.banking.moneytransfer.repository.AccountRepository;
import com.banking.moneytransfer.repository.PromotionRepository;
import com.banking.moneytransfer.repository.RedemptionRepository;
import com.banking.moneytransfer.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SwiftCoins store: lists promotions, redeems them against a user's coin balance,
 * and tracks redemption history.
 *
 * <p>A user's spendable balance is their earned reward points minus the SwiftCoins
 * they have already spent on redemptions.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PromotionService {

    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PromotionRepository promotionRepository;
    private final RedemptionRepository redemptionRepository;
    private final RewardRepository rewardRepository;
    private final AccountRepository accountRepository;

    /** Available promotions catalog, cheapest first. */
    @Transactional(readOnly = true)
    public List<PromotionResponse> getPromotions() {
        return promotionRepository.findByActiveTrueOrderByCoinCostAsc()
                .stream().map(this::mapPromotion).toList();
    }

    /** Spendable SwiftCoins = earned reward points − SwiftCoins already spent. */
    @Transactional(readOnly = true)
    public int getAvailableCoins(String accountId) {
        return rewardRepository.sumPointsByAccount(accountId) - redemptionRepository.sumCoinsByAccount(accountId);
    }

    /** A user's redemption history, newest purchase first. */
    @Transactional(readOnly = true)
    public List<RedemptionResponse> getRedemptions(String accountId) {
        ensureAccountExists(accountId);
        return redemptionRepository.findByAccount_IdOrderByCreatedOnDesc(accountId)
                .stream().map(this::mapRedemption).toList();
    }

    /**
     * Redeem a promotion: validates balance, records the redemption with a coupon
     * code and expiry, and returns it. Atomic — spend and record commit together.
     */
    @Transactional
    public RedemptionResponse redeem(String accountId, Long promotionId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Promotion promotion = promotionRepository.findById(promotionId)
                .filter(Promotion::isActive)
                .orElseThrow(() -> new PromotionNotFoundException(promotionId));

        int available = getAvailableCoins(accountId);
        if (available < promotion.getCoinCost()) {
            throw new InsufficientCoinsException(
                    "Not enough SwiftCoins: you have " + available + " but need " + promotion.getCoinCost() + ".");
        }

        Redemption redemption = Redemption.builder()
                .account(account)
                .promotion(promotion)
                .coinsSpent(promotion.getCoinCost())
                .code(generateCode(promotion))
                .expiresOn(LocalDateTime.now().plusDays(promotion.getValidityDays()))
                .build();

        redemption = redemptionRepository.save(redemption);

        log.info("AUDIT | PROMO_REDEEMED | account={} | promotion={} | coins={} | code={}",
                accountId, promotion.getId(), promotion.getCoinCost(), redemption.getCode());

        return mapRedemption(redemption);
    }

    /**
     * Seed the catalog on startup. Re-seeds every time so catalog changes take
     * effect in dev (redemptions are cleared beforehand by the account seeder).
     */
    @Transactional
    public void seedPromotions() {
        promotionRepository.deleteAll();
        promotionRepository.saveAll(List.of(
                // Shopping
                promo("Amazon", "10% off", "On orders above ₹2,000 · up to ₹500 off", "shopping", 10, 2000, 500, 200, 45),
                promo("Flipkart", "12% off", "On orders above ₹2,500 · up to ₹600 off", "shopping", 12, 2500, 600, 250, 45),
                promo("Croma", "8% off", "On electronics above ₹5,000 · up to ₹1,000 off", "shopping", 8, 5000, 1000, 350, 45),
                promo("Tata CLiQ", "15% off", "On orders above ₹3,000 · up to ₹900 off", "shopping", 15, 3000, 900, 320, 45),
                // Dressing
                promo("Myntra", "15% off", "On fashion above ₹1,999 · up to ₹750 off", "dressing", 15, 1999, 750, 300, 30),
                promo("Ajio", "20% off", "On styles above ₹2,500 · up to ₹1,000 off", "dressing", 20, 2500, 1000, 400, 30),
                promo("H&M", "12% off", "On apparel above ₹1,799 · up to ₹600 off", "dressing", 12, 1799, 600, 240, 30),
                promo("Levi's", "25% off", "On denim above ₹2,999 · up to ₹1,200 off", "dressing", 25, 2999, 1200, 480, 30),
                // Gifting
                promo("Ferns N Petals", "10% off", "On gifts above ₹999 · up to ₹300 off", "gifting", 10, 999, 300, 150, 30),
                promo("IGP", "15% off", "On gifting above ₹1,499 · up to ₹500 off", "gifting", 15, 1499, 500, 250, 30),
                promo("Archies", "12% off", "On cards & gifts above ₹799 · up to ₹250 off", "gifting", 12, 799, 250, 160, 30),
                promo("Winni", "18% off", "On cakes & gifts above ₹1,299 · up to ₹450 off", "gifting", 18, 1299, 450, 230, 30),
                // Travel
                promo("MakeMyTrip", "10% off flights", "On domestic flights above ₹5,000 · up to ₹1,500 off", "travel", 10, 5000, 1500, 500, 60),
                promo("OYO", "20% off hotels", "On hotels above ₹2,000 · up to ₹1,000 off", "travel", 20, 2000, 1000, 380, 60),
                promo("Goibibo", "12% off", "On bookings above ₹4,000 · up to ₹1,200 off", "travel", 12, 4000, 1200, 420, 60),
                promo("Ola", "15% off rides", "On rides above ₹300 · up to ₹150 off", "travel", 15, 300, 150, 180, 30),
                // Food & Dining
                promo("Swiggy", "20% off", "On orders above ₹499 · up to ₹150 off", "food", 20, 499, 150, 180, 30),
                promo("Zomato", "15% off", "On orders above ₹599 · up to ₹200 off", "food", 15, 599, 200, 200, 30),
                promo("Domino's", "25% off", "On pizzas above ₹699 · up to ₹250 off", "food", 25, 699, 250, 220, 30),
                promo("Starbucks", "10% off", "On beverages above ₹500 · up to ₹120 off", "food", 10, 500, 120, 150, 30),
                // Entertainment
                promo("BookMyShow", "25% off", "On movie tickets above ₹500 · up to ₹300 off", "entertainment", 25, 500, 300, 220, 30),
                promo("Netflix", "20% off", "On a Premium plan above ₹499 · up to ₹130 off", "entertainment", 20, 499, 130, 200, 30),
                promo("Spotify", "30% off", "On Premium above ₹119 · up to ₹50 off", "entertainment", 30, 119, 50, 150, 30),
                promo("PVR INOX", "15% off", "On tickets above ₹600 · up to ₹250 off", "entertainment", 15, 600, 250, 210, 30)
        ));
        log.info("Seeded promotions catalog");
    }

    private Promotion promo(String partner, String title, String desc, String category,
                            int discountPercent, int minSpend, int maxDiscount, int cost, int validityDays) {
        return Promotion.builder()
                .partner(partner).title(title).description(desc).category(category)
                .discountPercent(discountPercent).minSpend(minSpend).maxDiscount(maxDiscount)
                .coinCost(cost).validityDays(validityDays).active(true)
                .build();
    }

    private String generateCode(Promotion promotion) {
        String prefix = promotion.getPartner().replaceAll("[^A-Za-z]", "").toUpperCase();
        prefix = prefix.substring(0, Math.min(3, prefix.length()));
        StringBuilder sb = new StringBuilder(prefix).append('-');
        for (int i = 0; i < 8; i++) {
            sb.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
        }
        return sb.toString();
    }

    private void ensureAccountExists(String accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
    }

    private PromotionResponse mapPromotion(Promotion p) {
        return PromotionResponse.builder()
                .id(p.getId())
                .partner(p.getPartner())
                .title(p.getTitle())
                .description(p.getDescription())
                .coinCost(p.getCoinCost())
                .discountPercent(p.getDiscountPercent())
                .minSpend(p.getMinSpend())
                .maxDiscount(p.getMaxDiscount())
                .validityDays(p.getValidityDays())
                .category(p.getCategory())
                .build();
    }

    private RedemptionResponse mapRedemption(Redemption r) {
        Promotion p = r.getPromotion();
        return RedemptionResponse.builder()
                .id(r.getId().toString())
                .partner(p.getPartner())
                .promotionTitle(p.getTitle())
                .description(p.getDescription())
                .discountPercent(p.getDiscountPercent())
                .minSpend(p.getMinSpend())
                .maxDiscount(p.getMaxDiscount())
                .category(p.getCategory())
                .coinsSpent(r.getCoinsSpent())
                .code(r.getCode())
                .createdOn(r.getCreatedOn())
                .expiresOn(r.getExpiresOn())
                .build();
    }
}
