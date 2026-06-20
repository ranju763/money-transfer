import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Promotion } from '../../models/promotion.model';
import { Redemption } from '../../models/redemption.model';
import { RewardSummary } from '../../models/reward-summary.model';

import { AuthService } from '../../services/auth.service';
import { RewardService } from '../../services/reward.service';
import { PromotionService } from '../../services/promotion.service';

interface PromoGroup { key: string; label: string; items: Promotion[]; }

@Component({
    selector: 'app-rewards',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './rewards.component.html',
    styleUrls: ['./rewards.component.css']
})
export class RewardsComponent implements OnInit {
    private readonly CATEGORY_ORDER = ['shopping', 'dressing', 'gifting', 'travel', 'food', 'entertainment'];
    private readonly CATEGORY_LABELS: Record<string, string> = {
        shopping: 'Shopping',
        dressing: 'Dressing',
        gifting: 'Gifting',
        travel: 'Travel',
        food: 'Food & Dining',
        entertainment: 'Entertainment'
    };

    summary = signal<RewardSummary | null>(null);
    promotions = signal<Promotion[]>([]);
    redemptions = signal<Redemption[]>([]);

    isLoading = signal<boolean>(true);
    errorMessage = signal<string>('');

    /** false = Buy Coupons view, true = My Coupons (redeemed) view. */
    showRedeemed = signal<boolean>(false);

    selectedPromo = signal<Promotion | null>(null);
    redeeming = signal<boolean>(false);
    redeemed = signal<Redemption | null>(null);
    redeemError = signal<string>('');
    expandedId = signal<string | null>(null);
    copiedCode = signal<string | null>(null);

    availableCoins = computed(() => this.summary()?.availableCoins ?? 0);
    redeemableCount = computed(() => this.promotions().filter(p => this.canAfford(p)).length);
    redeemedCount = computed(() => this.redemptions().length);

    groupedPromotions = computed<PromoGroup[]>(() => {
        const groups = new Map<string, Promotion[]>();
        for (const p of this.promotions()) {
            if (!groups.has(p.category)) groups.set(p.category, []);
            groups.get(p.category)!.push(p);
        }
        return this.CATEGORY_ORDER
            .filter(c => groups.has(c))
            .map(c => ({ key: c, label: this.CATEGORY_LABELS[c] ?? c, items: groups.get(c)! }));
    });

    private route = inject(ActivatedRoute);

    constructor(
        private authService: AuthService,
        private rewardService: RewardService,
        private promotionService: PromotionService
    ) { }

    ngOnInit(): void {
        // Deep link: /rewards?view=redeemed opens the "My Coupons" view.
        if (this.route.snapshot.queryParamMap.get('view') === 'redeemed') {
            this.showRedeemed.set(true);
        }

        const accountId = this.authService.accountId;
        if (!this.authService.loggedIn || !accountId) {
            this.errorMessage.set('Session expired. Please re-login.');
            this.isLoading.set(false);
            return;
        }

        forkJoin({
            summary: this.rewardService.fetchSummary(accountId),
            promotions: this.promotionService.fetchPromotions(),
            redemptions: this.promotionService.fetchRedemptions(accountId)
        }).subscribe({
            next: ({ summary, promotions, redemptions }) => {
                this.summary.set(summary);
                this.promotions.set(promotions);
                this.redemptions.set(redemptions);
                this.isLoading.set(false);
            },
            error: () => {
                this.errorMessage.set('Failed to load the rewards store. Please try again.');
                this.isLoading.set(false);
            }
        });
    }

    toggleView(): void {
        this.showRedeemed.update(v => !v);
    }

    canAfford(p: Promotion): boolean {
        return this.availableCoins() >= p.coinCost;
    }

    openPromo(p: Promotion): void {
        this.redeemError.set('');
        this.selectedPromo.set(p);
    }

    closePromo(): void {
        if (this.redeeming()) return;
        this.selectedPromo.set(null);
    }

    redeem(): void {
        const p = this.selectedPromo();
        const accountId = this.authService.accountId;
        if (!p || !accountId || this.redeeming()) return;

        this.redeemError.set('');
        this.redeeming.set(true);

        this.promotionService.redeem(accountId, p.id).subscribe({
            next: (redemption) => {
                this.redeeming.set(false);
                this.selectedPromo.set(null);
                this.redeemed.set(redemption);
                this.refresh(accountId);
                this.rewardService.loadPoints(accountId);
            },
            error: (err) => {
                this.redeeming.set(false);
                this.redeemError.set(err.error?.error || 'Could not redeem this reward. Please try again.');
            }
        });
    }

    private refresh(accountId: string): void {
        this.rewardService.fetchSummary(accountId).subscribe(s => this.summary.set(s));
        this.promotionService.fetchRedemptions(accountId).subscribe(r => this.redemptions.set(r));
    }

    closeModal(): void {
        this.redeemed.set(null);
    }

    toggleRedemption(id: string): void {
        this.expandedId.update(cur => (cur === id ? null : id));
    }

    copyCode(code: string): void {
        if (navigator?.clipboard) {
            navigator.clipboard.writeText(code).then(() => {
                this.copiedCode.set(code);
                setTimeout(() => this.copiedCode.set(null), 2000);
            });
        }
    }

    icon(category: string): string {
        switch (category) {
            case 'travel': return 'bi-airplane';
            case 'food': return 'bi-cup-hot';
            case 'shopping': return 'bi-bag';
            case 'dressing': return 'bi-bag-heart';
            case 'gifting': return 'bi-gift';
            case 'entertainment': return 'bi-film';
            default: return 'bi-tag';
        }
    }
}
