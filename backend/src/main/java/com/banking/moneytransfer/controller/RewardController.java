package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.dto.RewardResponse;
import com.banking.moneytransfer.dto.RewardSummaryResponse;
import com.banking.moneytransfer.service.RewardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for reward operations.
 *
 * <p>All endpoints are restricted to the authenticated owner of the account
 * (same ownership rule used across the account/transfer APIs).</p>
 */
@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
@Slf4j
public class RewardController {

    private final RewardService rewardService;

    /**
     * Get the reward history for an account.
     * GET /api/v1/rewards/{accountId}
     */
    @PreAuthorize("#accountId == authentication.name")
    @GetMapping("/{accountId}")
    public List<RewardResponse> getRewards(@PathVariable String accountId) {
        log.info("Received request to get rewards for account ID: {}", accountId);

        return rewardService.getRewards(accountId);
    }

    /**
     * Get the aggregated reward summary for an account.
     * GET /api/v1/rewards/{accountId}/summary
     */
    @PreAuthorize("#accountId == authentication.name")
    @GetMapping("/{accountId}/summary")
    public RewardSummaryResponse getRewardSummary(@PathVariable String accountId) {
        log.info("Received request to get reward summary for account ID: {}", accountId);

        return rewardService.getRewardSummary(accountId);
    }
}
