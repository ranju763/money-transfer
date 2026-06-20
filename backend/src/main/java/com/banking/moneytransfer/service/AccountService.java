package com.banking.moneytransfer.service;

import com.banking.moneytransfer.dto.AccountBalanceResponse;
import com.banking.moneytransfer.dto.AccountResponse;
import com.banking.moneytransfer.dto.TransactionLogResponse;
import com.banking.moneytransfer.exception.AccountNotFoundException;
import com.banking.moneytransfer.model.entity.Account;
import com.banking.moneytransfer.model.enums.AccountStatus;
import com.banking.moneytransfer.model.enums.TransactionStatus;
import com.banking.moneytransfer.model.enums.TransactionType;
import com.banking.moneytransfer.repository.AccountRepository;
import com.banking.moneytransfer.repository.RewardRepository;
import com.banking.moneytransfer.repository.TransactionLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service class for account operations
 */
@Slf4j
@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Get account by ID
     *
     * @param id Account ID
     * @return AccountResponse
     * @throws AccountNotFoundException if account not found
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String id) {
        log.info("Fetching account with ID: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        return mapToResponse(account);
    }

    /**
     * Get account balance
     *
     * @param id Account ID
     * @return Account balance
     * @throws AccountNotFoundException if account not found
     */
    @Transactional(readOnly = true)
    public AccountBalanceResponse getBalance(String id) {
        log.info("Fetching balance for account ID: {}", id);

        return accountRepository.findById(id)
                .map(account -> new AccountBalanceResponse(account.getBalance()))
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    /**
     * Get transaction history for an account
     *
     * @param id Account ID
     * @return List of transactions (order by createdOn)
     * @throws AccountNotFoundException if account not found
     */
    @Transactional(readOnly = true)
    public List<TransactionLogResponse> getTransactions(String id) {
        log.info("Fetching transactions for account ID: {}", id);

        // Verify account exists
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        // List send and receive transaction, map to TransactionLogResponse object, and
        // Sort by CreateOn timestamp (newest to oldest)
        return Stream.concat(
                        account.getSentTransactions()
                                .stream()
                                .map(t -> TransactionLogResponse.builder()
                                        .accountId(t.getToAccount().getId())
                                        .holderName(t.getToAccount().getHolderName())
                                        .type(TransactionType.SEND)
                                        .status(t.getStatus())
                                        .failureReason(t.getFailureReason())
                                        .amount(t.getAmount())
                                        .createdOn(t.getCreatedOn())
                                        .build()),
                        account.getReceivedTransactions()
                                .stream()
                                .filter(t -> t.getStatus().equals(TransactionStatus.SUCCESS))
                                .map(t -> TransactionLogResponse.builder()
                                        .accountId(t.getFromAccount().getId())
                                        .holderName(t.getFromAccount().getHolderName())
                                        .type(TransactionType.RECEIVE)
                                        .status(t.getStatus())
                                        .failureReason(t.getFailureReason())
                                        .amount(t.getAmount())
                                        .createdOn(t.getCreatedOn())
                                        .build())
                )
                .sorted(Comparator.comparing(TransactionLogResponse::getCreatedOn).reversed())
                .toList();
    }

    /**
     * Map Account entity to AccountResponse DTO
     */
    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .holderName(account.getHolderName())
                .balance(account.getBalance())
                .status(account.getStatus().name())
                .lastUpdated(account.getLastUpdated())
                .build();
    }

    private Account mapToAccount(String id, String name, double balance, AccountStatus status) {
        return Account.builder()
                .id(id)
                .holderName(name)
                .passwordHash(passwordEncoder.encode("pwd@" + id.substring(id.length() - 4)))
                .status(status)
                .balance(BigDecimal.valueOf(balance))
                .build();
    }

    @Transactional
    public void createDummyAccounts() {
        // clear existing data (rewards first: they reference transaction_logs and accounts)
        rewardRepository.deleteAll();
        transactionLogRepository.deleteAll();
        accountRepository.deleteAll();
        log.info("Delete all entities from rewards, accounts and transaction_logs table");

        // dummy accounts
        List<Account> accounts = List.of(
                mapToAccount("1000-1000-1001", "John Doe",   10000.00,  AccountStatus.ACTIVE),
                mapToAccount("1000-1000-1002", "Jane Smith",   5000.00,  AccountStatus.ACTIVE),
                mapToAccount("1000-1000-1003", "Bob Johnson",  15000.00, AccountStatus.ACTIVE),
                mapToAccount("1000-1000-1004", "Alice Williams", 8000.00, AccountStatus.ACTIVE),
                mapToAccount("1000-1000-1005", "Charlie Brown", 12000.00, AccountStatus.LOCKED),
                mapToAccount("1000-1000-1006", "Diana Prince", 20000.00, AccountStatus.ACTIVE),
                mapToAccount("1000-1000-1007", "Eve Davis",    3000.00,  AccountStatus.CLOSED),
                mapToAccount("1000-1000-1008", "Frank Miller", 7500.00,  AccountStatus.ACTIVE)
        );

        // save to repository
        accountRepository.saveAll(accounts);
        log.info("Inserted dummy accounts");
    }
}