package com.banking.moneytransfer.model.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A promotional item that can be redeemed with SwiftCoins (e.g. a MakeMyTrip coupon).
 * This is the catalog — the same items are offered to every user.
 */
@Entity
@Table(name = "promotions")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Brand offering the promotion, e.g. "MakeMyTrip". */
    @Column(nullable = false, length = 100)
    private String partner;

    /** Short offer headline, e.g. "10% off". */
    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    /** Cost in SwiftCoins to redeem this promotion. */
    @Column(name = "coin_cost", nullable = false)
    private int coinCost;

    /** Percentage discount the coupon gives. */
    @Column(name = "discount_percent", nullable = false)
    private int discountPercent;

    /** Minimum spend required for the coupon to apply. */
    @Column(name = "min_spend", nullable = false)
    private int minSpend;

    /** Maximum discount the coupon can give (cap). */
    @Column(name = "max_discount", nullable = false)
    private int maxDiscount;

    /** Days the coupon stays valid after redemption. */
    @Column(name = "validity_days", nullable = false)
    private int validityDays;

    /** Category used for grouping/icon (shopping, dressing, gifting, travel, food, entertainment). */
    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    private boolean active;
}
