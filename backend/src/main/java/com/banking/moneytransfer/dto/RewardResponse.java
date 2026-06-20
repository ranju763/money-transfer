package com.banking.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single reward record in an account's reward history.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RewardResponse {

    /** The transaction that earned this reward. */
    private String transactionId;

    /** Counterparty (the account the money was sent to). */
    private String counterpartyId;

    private String counterpartyName;

    /** Transaction amount that earned the reward. */
    private BigDecimal amount;

    /** Points earned for this transaction. */
    private Integer points;

    private LocalDateTime createdOn;
}
