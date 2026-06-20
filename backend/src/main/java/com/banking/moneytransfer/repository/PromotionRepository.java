package com.banking.moneytransfer.repository;

import com.banking.moneytransfer.model.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    /** Active catalog, cheapest first. */
    List<Promotion> findByActiveTrueOrderByCoinCostAsc();
}
