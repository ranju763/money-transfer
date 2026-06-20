package com.banking.moneytransfer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RedeemRequest {

    @NotNull(message = "Promotion id is required")
    private Long promotionId;
}
