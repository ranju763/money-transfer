package com.banking.moneytransfer.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A record of a user redeeming a promotion with SwiftCoins.
 * Drives both the user's redemption history and the spent-coins balance.
 */
@Entity
@Table(name = "redemptions")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
@Builder
public class Redemption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, targetEntity = Account.class)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(optional = false, targetEntity = Promotion.class)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    /** Coins spent (snapshot of the promotion cost at redemption time). */
    @Column(name = "coins_spent", nullable = false)
    private int coinsSpent;

    /** The generated coupon code the user receives. */
    @Column(nullable = false, length = 40)
    private String code;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    /** When the redeemed coupon expires. */
    @Column(name = "expires_on", nullable = false)
    private LocalDateTime expiresOn;
}
