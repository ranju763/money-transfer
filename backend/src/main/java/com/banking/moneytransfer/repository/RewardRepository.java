package com.banking.moneytransfer.repository;

import com.banking.moneytransfer.model.entity.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Reward entity
 */
@Repository
public interface RewardRepository extends JpaRepository<Reward, UUID> {

    /**
     * Reward history for an account, newest first.
     */
    List<Reward> findByAccount_IdOrderByCreatedOnDesc(String accountId);

    /**
     * Guards against granting a reward for the same transaction twice.
     */
    boolean existsByTransactionLog_Id(UUID transactionId);

    /**
     * Number of transactions that earned the account a reward.
     */
    long countByAccount_Id(String accountId);

    /**
     * Total reward points accumulated by an account (0 when none).
     */
    @Query("SELECT COALESCE(SUM(r.points), 0) FROM Reward r WHERE r.account.id = :accountId")
    int sumPointsByAccount(@Param("accountId") String accountId);
}
