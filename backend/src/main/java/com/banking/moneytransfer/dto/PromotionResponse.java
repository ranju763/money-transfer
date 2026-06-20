package com.banking.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PromotionResponse {
    private Long id;
    private String partner;
    private String title;
    private String description;
    private int coinCost;
    private int discountPercent;
    private int minSpend;
    private int maxDiscount;
    private int validityDays;
    private String category;
}
