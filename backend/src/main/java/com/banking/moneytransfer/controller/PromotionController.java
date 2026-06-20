package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.dto.PromotionResponse;
import com.banking.moneytransfer.dto.RedeemRequest;
import com.banking.moneytransfer.dto.RedemptionResponse;
import com.banking.moneytransfer.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for the SwiftCoins promotions store.
 */
@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Slf4j
public class PromotionController {

    private final PromotionService promotionService;

    /**
     * The promotions catalog (same for every authenticated user).
     * GET /api/v1/promotions
     */
    @GetMapping
    public List<PromotionResponse> getPromotions() {
        log.info("Received request for promotions catalog");
        return promotionService.getPromotions();
    }

    /**
     * A user's redemption history.
     * GET /api/v1/promotions/{accountId}/redemptions
     */
    @PreAuthorize("#accountId == authentication.name")
    @GetMapping("/{accountId}/redemptions")
    public List<RedemptionResponse> getRedemptions(@PathVariable String accountId) {
        log.info("Received request for redemptions of account {}", accountId);
        return promotionService.getRedemptions(accountId);
    }

    /**
     * Redeem a promotion with SwiftCoins.
     * POST /api/v1/promotions/{accountId}/redeem
     */
    @PreAuthorize("#accountId == authentication.name")
    @PostMapping("/{accountId}/redeem")
    public RedemptionResponse redeem(@PathVariable String accountId, @Valid @RequestBody RedeemRequest request) {
        log.info("Account {} redeeming promotion {}", accountId, request.getPromotionId());
        return promotionService.redeem(accountId, request.getPromotionId());
    }
}
