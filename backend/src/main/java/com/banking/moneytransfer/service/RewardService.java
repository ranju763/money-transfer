package com.banking.moneytransfer.service;

import com.banking.moneytransfer.dto.RewardResponse;
import com.banking.moneytransfer.dto.RewardSummaryResponse;
import com.banking.moneytransfer.exception.AccountNotFoundException;
import com.banking.moneytransfer.model.entity.Account;
import com.banking.moneytransfer.model.entity.Reward;
import com.banking.moneytransfer.model.entity.TransactionLog;
import com.banking.moneytransfer.model.enums.TransactionStatus;
import com.banking.moneytransfer.repository.AccountRepository;
import com.banking.moneytransfer.repository.RedemptionRepository;
import com.banking.moneytransfer.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reward module business logic.
 *
 * <p>Implements clear, rule-based reward logic that sits on top of the existing
 * transfer system. A transaction earns the sender reward points only when ALL
 * eligibility rules are met, and every grant is recorded for full traceability.</p>
 *
 * <h3>Eligibility rules</h3>
 * <ol>
 *   <li>Transaction status is SUCCESS</li>
 *   <li>Transaction amount is strictly greater than {@value #MIN_ELIGIBLE_AMOUNT_LITERAL}</li>
 *   <li>Sender and receiver are different accounts (no self-transfer)</li>
 * </ol>
 *
 * <h3>Collection logic</h3>
 * <p>1 reward point per ₹100 transferred, rounded down
 * (e.g. ₹250 → 2 points, ₹99 → 0 points).</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RewardService {

    private static final String MIN_ELIGIBLE_AMOUNT_LITERAL = "100";

    /** A transaction must be strictly greater than this amount to be eligible. */
    public static final BigDecimal MIN_ELIGIBLE_AMOUNT = new BigDecimal(MIN_ELIGIBLE_AMOUNT_LITERAL);

    /** One point is earned per this many rupees transferred. */
    public static final BigDecimal POINTS_DIVISOR = new BigDecimal("100");

    private final RewardRepository rewardRepository;
    private final RedemptionRepository redemptionRepository;
    private final AccountRepository accountRepository;

    /**
     * Evaluate a transaction against the eligibility rules and, if eligible,
     * grant reward points to the sender.
     *
     * <p>Designed to be called from within the transfer transaction so the reward
     * is committed atomically with the transfer. It is idempotent: a transaction
     * that already has a reward will never be rewarded again.</p>
     *
     * @param transaction the freshly persisted transaction log
     */
    @Transactional
    public void evaluateAndGrant(TransactionLog transaction) {
        if (!isEligible(transaction)) {
            log.debug("Transaction {} is not eligible for rewards", transaction.getId());
            return;
        }

        // Idempotency guard (also enforced by the unique transaction_id column).
        if (rewardRepository.existsByTransactionLog_Id(transaction.getId())) {
            log.warn("Reward already granted for transaction {} - skipping", transaction.getId());
            return;
        }

        int points = calculatePoints(transaction.getAmount());
        if (points <= 0) {
            // Defensive: an eligible (> 100) amount always yields >= 1 point.
            return;
        }

        Account sender = transaction.getFromAccount();
        Reward reward = Reward.builder()
                .account(sender)
                .transactionLog(transaction)
                .points(points)
                .transactionAmount(transaction.getAmount())
                .build();

        rewardRepository.save(reward);

        // Audit trail for full traceability of every reward granted.
        log.info("AUDIT | REWARD_GRANTED | account={} | transaction={} | amount={} | points={}",
                sender.getId(), transaction.getId(), transaction.getAmount(), points);
    }

    /**
     * Check whether a transaction satisfies ALL reward eligibility rules.
     */
    public boolean isEligible(TransactionLog transaction) {
        // Rule 1: status must be SUCCESS
        if (transaction.getStatus() != TransactionStatus.SUCCESS) {
            return false;
        }

        // Rule 2: amount must be strictly greater than the minimum
        BigDecimal amount = transaction.getAmount();
        if (amount == null || amount.compareTo(MIN_ELIGIBLE_AMOUNT) <= 0) {
            return false;
        }

        // Rule 3: sender and receiver must be different (no self-transfer)
        String fromId = transaction.getFromAccount().getId();
        String toId = transaction.getToAccount().getId();
        return !fromId.equals(toId);
    }

    /**
     * Reward points = floor(amount / 100).
     * e.g. 250 → 2, 199 → 1, 99 → 0.
     */
    public int calculatePoints(BigDecimal amount) {
        return amount.divideToIntegralValue(POINTS_DIVISOR).intValue();
    }

    /**
     * Reward history for an account (newest first).
     *
     * @throws AccountNotFoundException if the account does not exist
     */
    @Transactional(readOnly = true)
    public List<RewardResponse> getRewards(String accountId) {
        ensureAccountExists(accountId);

        return rewardRepository.findByAccount_IdOrderByCreatedOnDesc(accountId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Aggregated reward standing for an account.
     *
     * @throws AccountNotFoundException if the account does not exist
     */
    @Transactional(readOnly = true)
    public RewardSummaryResponse getRewardSummary(String accountId) {
        ensureAccountExists(accountId);

        int earned = rewardRepository.sumPointsByAccount(accountId);
        int redeemed = redemptionRepository.sumCoinsByAccount(accountId);

        return RewardSummaryResponse.builder()
                .accountId(accountId)
                .totalPoints(earned)
                .rewardedTransactions(rewardRepository.countByAccount_Id(accountId))
                .redeemedCoins(redeemed)
                .availableCoins(earned - redeemed)
                .build();
    }

    private void ensureAccountExists(String accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
    }

    private RewardResponse mapToResponse(Reward reward) {
        TransactionLog txn = reward.getTransactionLog();
        return RewardResponse.builder()
                .transactionId(txn.getId().toString())
                .counterpartyId(txn.getToAccount().getId())
                .counterpartyName(txn.getToAccount().getHolderName())
                .amount(reward.getTransactionAmount())
                .points(reward.getPoints())
                .createdOn(reward.getCreatedOn())
                .build();
    }
}
