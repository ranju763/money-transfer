import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Account } from '../../models/account.model';
import { Transaction } from '../../models/transaction.model';
import { Reward } from '../../models/reward.model';
import { RewardSummary } from '../../models/reward-summary.model';

import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';
import { RewardService } from '../../services/reward.service';

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
    allRewards = signal<Reward[]>([]);

    isLoading = signal<boolean>(true);
    errorMessage = signal<string>('');
    showBalance = signal<boolean>(true);

    recentTransactions = computed(() => this.allTransactions().slice(0, 5));
    recentRewards = computed(() => this.allRewards().slice(0, 5));

    // Total amount the user has successfully sent out.
    totalSent = computed(() =>
        this.allTransactions()
            .filter(t => t.type === 'SEND' && t.status === 'SUCCESS')
            .reduce((sum, t) => sum + Number(t.amount), 0)
    );

    constructor(
        private authService: AuthService,
        private accountService: AccountService,
        private rewardService: RewardService
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
            rewards: this.rewardService.fetchRewards(accountId),
            summary: this.rewardService.fetchSummary(accountId)
        }).subscribe({
            next: ({ account, transactions, rewards, summary }) => {
                this.account.set(account);
                this.allTransactions.set(transactions);
                this.allRewards.set(rewards);
                this.summary.set(summary);
                this.isLoading.set(false);
            },
            error: () => {
                this.errorMessage.set('Failed to load your dashboard. Please try again.');
                this.isLoading.set(false);
            }
        });
    }

    toggleBalance(): void {
        this.showBalance.update(v => !v);
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
