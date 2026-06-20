import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Reward } from '../../models/reward.model';
import { RewardSummary } from '../../models/reward-summary.model';

import { AuthService } from '../../services/auth.service';
import { RewardService } from '../../services/reward.service';
import { environment } from 'environments/environment';

@Component({
    selector: 'app-rewards',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './rewards.component.html',
    styleUrls: ['./rewards.component.css']
})
export class RewardsComponent implements OnInit {
    readonly RECORDS_PER_PAGE = environment.pageLimit;

    protected Math = Math;

    summary = signal<RewardSummary | null>(null);
    allRewards = signal<Reward[]>([]);
    isLoading = signal<boolean>(true);
    errorMessage = signal<string>('');

    currentPage = signal<number>(1);

    totalPages = computed(() => Math.ceil(this.allRewards().length / this.RECORDS_PER_PAGE) || 1);

    rewards = computed(() => {
        const start = (this.currentPage() - 1) * this.RECORDS_PER_PAGE;
        return this.allRewards().slice(start, start + this.RECORDS_PER_PAGE);
    });

    constructor(
        private authService: AuthService,
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
            summary: this.rewardService.fetchSummary(accountId),
            rewards: this.rewardService.fetchRewards(accountId)
        }).subscribe({
            next: ({ summary, rewards }) => {
                this.summary.set(summary);
                this.allRewards.set(rewards);
                this.isLoading.set(false);
            },
            error: () => {
                this.errorMessage.set('Failed to load your rewards. Please try again.');
                this.isLoading.set(false);
            }
        });
    }

    nextPage(): void {
        if (this.currentPage() < this.totalPages()) {
            this.currentPage.update(p => p + 1);
        }
    }

    prevPage(): void {
        if (this.currentPage() > 1) {
            this.currentPage.update(p => p - 1);
        }
    }
}
