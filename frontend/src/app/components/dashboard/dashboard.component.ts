import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Account } from '../../models/account.model';
import { Transaction } from '../../models/transaction.model';
import { Redemption } from '../../models/redemption.model';
import { RewardSummary } from '../../models/reward-summary.model';

import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';
import { RewardService } from '../../services/reward.service';
import { PromotionService } from '../../services/promotion.service';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
    account = signal<Account | null>(null);
    summary = signal<RewardSummary | null>(null);
    allTransactions = signal<Transaction[]>([]);
    redemptions = signal<Redemption[]>([]);

    isLoading = signal<boolean>(true);
    errorMessage = signal<string>('');

    // Password-protected balance
    balanceRevealed = signal<boolean>(false);
    promptingPassword = signal<boolean>(false);
    passwordError = signal<string>('');

    recentTransactions = computed(() => this.allTransactions().slice(0, 5));
    redeemedCount = computed(() => this.redemptions().length);

    earnedThisMonth = computed(() =>
        this.allTransactions()
            .filter(t => t.type === 'RECEIVE' && t.status === 'SUCCESS' && this.isThisMonth(t.createdOn))
            .reduce((sum, t) => sum + Number(t.amount), 0)
    );

    spentThisMonth = computed(() =>
        this.allTransactions()
            .filter(t => t.type === 'SEND' && t.status === 'SUCCESS' && this.isThisMonth(t.createdOn))
            .reduce((sum, t) => sum + Number(t.amount), 0)
    );

    constructor(
        private authService: AuthService,
        private accountService: AccountService,
        private rewardService: RewardService,
        private promotionService: PromotionService
    ) { }

    ngOnInit(): void {
        const accountId = this.authService.accountId;
        if (!this.authService.loggedIn || !accountId) {
            this.errorMessage.set('Session expired. Please re-login.');
            this.isLoading.set(false);
            return;
        }

        forkJoin({
            account: this.accountService.fetchAccount(accountId),
            transactions: this.accountService.fetchTransactions(accountId),
            summary: this.rewardService.fetchSummary(accountId),
            redemptions: this.promotionService.fetchRedemptions(accountId)
        }).subscribe({
            next: ({ account, transactions, summary, redemptions }) => {
                this.account.set(account);
                this.allTransactions.set(transactions);
                this.summary.set(summary);
                this.redemptions.set(redemptions);
                this.isLoading.set(false);
            },
            error: () => {
                this.errorMessage.set('Failed to load your dashboard. Please try again.');
                this.isLoading.set(false);
            }
        });
    }

    private isThisMonth(dateStr: string): boolean {
        const d = new Date(dateStr);
        const now = new Date();
        return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
    }

    // ---- Balance reveal flow ----
    promptBalance(): void {
        this.passwordError.set('');
        this.promptingPassword.set(true);
    }

    unlockBalance(password: string): void {
        if (this.authService.checkPassword(password)) {
            this.balanceRevealed.set(true);
            this.promptingPassword.set(false);
            this.passwordError.set('');
        } else {
            this.passwordError.set('Incorrect password');
        }
    }

    hideBalance(): void {
        this.balanceRevealed.set(false);
        this.promptingPassword.set(false);
    }

    get firstName(): string {
        const name = this.account()?.holderName ?? '';
        return name.split(' ')[0] || 'there';
    }

    get greeting(): string {
        const hour = new Date().getHours();
        if (hour < 12) return 'Good morning';
        if (hour < 17) return 'Good afternoon';
        return 'Good evening';
    }
}
