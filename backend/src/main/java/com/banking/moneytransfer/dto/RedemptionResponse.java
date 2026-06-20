package com.banking.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RedemptionResponse {
    private String id;
    private String partner;
    private String promotionTitle;
    private String description;
    private int discountPercent;
    private int minSpend;
    private int maxDiscount;
    private String category;
    private int coinsSpent;
    private String code;
    private LocalDateTime createdOn;
    private LocalDateTime expiresOn;
}
