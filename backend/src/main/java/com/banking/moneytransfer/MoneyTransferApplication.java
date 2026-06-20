package com.banking.moneytransfer;

import com.banking.moneytransfer.service.AccountService;
import com.banking.moneytransfer.service.DemoDataSeeder;
import com.banking.moneytransfer.service.PromotionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Main Spring Boot Application Class
 */
@SpringBootApplication
@EnableAspectJAutoProxy
public class MoneyTransferApplication {

    public static void main(String[] args) { SpringApplication.run(MoneyTransferApplication.class, args); }

    @Bean
    @ConditionalOnProperty(name = "dummy.enable", havingValue = "true")
    CommandLineRunner seedRunner(AccountService accountService, PromotionService promotionService,
                                 DemoDataSeeder demoDataSeeder) {
        return args -> {
            // Clear accounts/redemptions first, (re)seed promotions, then add demo activity.
            accountService.createDummyAccounts();
            promotionService.seedPromotions();
            demoDataSeeder.seed();
        };
    }
}