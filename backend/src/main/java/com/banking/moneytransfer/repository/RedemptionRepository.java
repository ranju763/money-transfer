package com.banking.moneytransfer.repository;

import com.banking.moneytransfer.model.entity.Redemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RedemptionRepository extends JpaRepository<Redemption, UUID> {

    List<Redemption> findByAccount_IdOrderByCreatedOnDesc(String accountId);

    /** Total coins an account has spent on redemptions (0 when none). */
    @Query("SELECT COALESCE(SUM(r.coinsSpent), 0) FROM Redemption r WHERE r.account.id = :accountId")
    int sumCoinsByAccount(@Param("accountId") String accountId);
}
