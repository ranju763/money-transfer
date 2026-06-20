package com.banking.moneytransfer.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity representing a reward earned by an account for an eligible transaction.
 *
 * <p>Each reward is tied to exactly one transaction (enforced by the unique
 * {@code transaction_id} column) which guarantees a transaction can never be
 * rewarded twice, even if the grant logic runs more than once.</p>
 */
@Entity
@Table(name = "rewards")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
@Builder
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The account that earned the reward (the sender of the transaction).
     */
    @ManyToOne(optional = false, targetEntity = Account.class)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /**
     * The transaction this reward was granted for. Unique: one reward per transaction.
     */
    @OneToOne(optional = false, targetEntity = TransactionLog.class)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private TransactionLog transactionLog;

    /**
     * Reward points earned (1 point per 100 transferred, rounded down).
     */
    @Column(nullable = false)
    private Integer points;

    /**
     * Snapshot of the transaction amount that earned this reward.
     */
    @Column(name = "transaction_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal transactionAmount;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;
}
