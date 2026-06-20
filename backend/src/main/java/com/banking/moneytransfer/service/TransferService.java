package com.banking.moneytransfer.service;

import com.banking.moneytransfer.dto.TransferRequest;
import com.banking.moneytransfer.dto.TransferResponse;
import com.banking.moneytransfer.exception.AccountNotFoundException;
import com.banking.moneytransfer.exception.DuplicateTransferException;
import com.banking.moneytransfer.exception.AccountNotActiveException;
import com.banking.moneytransfer.exception.InsufficientBalanceException;
import com.banking.moneytransfer.model.entity.Account;
import com.banking.moneytransfer.model.entity.TransactionLog;
import com.banking.moneytransfer.model.enums.TransactionStatus;
import com.banking.moneytransfer.repository.AccountRepository;
import com.banking.moneytransfer.repository.TransactionLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service class for fund transfer operations
 */
@Service
@Slf4j
public class TransferService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private TransactionLogService transactionLogService;

    @Autowired
    private RewardService rewardService;

    /**
     * Execute fund transfer between accounts
     *
     * @param request Transfer request
     * @return TransferResponse
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        log.info("Starting transfer from account {} to account {} for amount {}",
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        // Validate transfer request
        validateTransfer(request);

        // Check for duplicate transfer (idempotency)
        checkDuplicateTransfer(request.getIdempotencyKey());

        // Execute transfer
        return executeTransfer(request);
    }

    /**
     * Validate transfer request
     */
    private void validateTransfer(TransferRequest request) {
        // Rule 1: Accounts must be different
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        // Rule 6: Amount must be greater than 0
        if (request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than 0");
        }
    }

    /**
     * Check for duplicate transfer using idempotency key
     */
    private void checkDuplicateTransfer(String idempotencyKey) {
        Optional<TransactionLog> existingTransaction =
                transactionLogRepository.findByIdempotencyKey(idempotencyKey);

        if (existingTransaction.isPresent()) {
            log.warn("Duplicate transfer detected with idempotency key: {}", idempotencyKey);
            throw DuplicateTransferException.withKey(idempotencyKey);
        }
    }

    /**
     * Execute the actual transfer
     */
    private TransferResponse executeTransfer(TransferRequest request) {
        // Fetch and lock accounts (pessimistic locking for concurrency control)
        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getFromAccountId()));

        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getToAccountId()));

        try {
            // Debit before credit
            fromAccount.debit(request.getAmount());
            toAccount.credit(request.getAmount());

        } catch (AccountNotActiveException | InsufficientBalanceException e) {
            // Log Failure (in a new transaction log)
            log.error("Transfer failed: {}", e.getMessage());

            TransactionLog failedLog = TransactionLog.builder()
                    .fromAccount(fromAccount)
                    .toAccount(toAccount)
                    .amount(request.getAmount())
                    .status(TransactionStatus.FAILED)
                    .failureReason(e.getMessage())
                    .idempotencyKey(request.getIdempotencyKey())
                    .build();
            transactionLogService.saveLog(failedLog);

            // RETHROW the exception so GlobalExceptionHandler sends the correct HTTP response
            throw e;
        }

        // Save accounts
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Log transaction
        TransactionLog transactionLog = TransactionLog.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(request.getAmount())
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        // Save transactionLog
        transactionLog = transactionLogRepository.save(transactionLog);

        log.info("Transfer completed successfully. Transaction ID: {}", transactionLog.getId());

        // Evaluate and grant rewards for this transaction (atomic with the transfer).
        // The reward module decides eligibility and points; the transfer is never
        // affected by reward outcomes.
        rewardService.evaluateAndGrant(transactionLog);

        // Build response
        return TransferResponse.builder()
                .transactionId(transactionLog.getId().toString())
                .status("SUCCESS")
                .message("Transfer completed successfully")
                .debitedFrom(request.getFromAccountId())
                .creditedTo(request.getToAccountId())
                .amount(request.getAmount())
                .build();
    }
}