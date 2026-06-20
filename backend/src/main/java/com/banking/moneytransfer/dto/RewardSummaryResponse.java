package com.banking.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated reward standing for an account.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RewardSummaryResponse {

    private String accountId;

    /** Total reward points accumulated across all eligible transactions. */
    private int totalPoints;

    /** Number of transactions that earned a reward. */
    private long rewardedTransactions;

    /** Coins already spent on promotion redemptions. */
    private int redeemedCoins;

    /** Spendable balance = totalPoints − redeemedCoins. */
    private int availableCoins;
}
