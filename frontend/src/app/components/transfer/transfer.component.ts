import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { TransferRequest } from '../../models/transfer-request.model';
import { TransferResponse } from '../../models/transfer-response.model';

import { AuthService } from '../../services/auth.service';
import { TransferService } from '../../services/transfer.service';
import { RewardService } from 'app/services/reward.service';
import { notEqual } from 'app/validators/matches.validator';
import { AccountMaskDirective } from 'app/directives/account-mask.directive';

@Component({
    selector: 'app-transfer',
    standalone: true,
    imports: [ReactiveFormsModule, CommonModule, AccountMaskDirective],
    templateUrl: './transfer.component.html',
    styleUrls: ['./transfer.component.css']
})
export class TransferComponent implements OnInit {
    /** A transfer must be strictly greater than this to earn coins (matches backend rule). */
    private readonly MIN_REWARDABLE = 100;

    transferForm!: FormGroup;
    submitted = signal<boolean>(false);
    isLoading = signal<boolean>(false);

    transferResponse = signal<TransferResponse | null>(null);
    failedTransaction = signal<any | null>(null);

    /** Live coins preview as the user types an amount. */
    coinsToEarn = signal<number>(0);
    /** Snapshot of the in-flight / completed transfer (amount, recipient, coins). */
    pending = signal<{ amount: number; to: string; coins: number } | null>(null);
    /** "Learn more" reward details, collapsed by default. */
    showInfo = signal<boolean>(false);

    constructor(
        private fb: FormBuilder,
        private authService: AuthService,
        private transferService: TransferService,
        private rewardService: RewardService
    ) { }

    ngOnInit(): void {
        this.transferForm = this.fb.group({
            toAccountId: ['', [
                Validators.required,
                Validators.minLength(14),
                Validators.maxLength(14),
                notEqual(this.authService.accountId!)
            ]],
            amount: [null, [Validators.required, Validators.min(0.01)]]
        });

        this.transferForm.get('amount')!.valueChanges.subscribe(v =>
            this.coinsToEarn.set(this.calcCoins(Number(v)))
        );
    }

    /** floor(amount / 100), but only when amount is strictly greater than ₹100. */
    private calcCoins(amount: number): number {
        if (!amount || amount <= this.MIN_REWARDABLE) return 0;
        return Math.floor(amount / 100);
    }

    onSubmit(): void {
        this.submitted.set(true);
        if (this.transferForm.invalid) return;

        const { toAccountId, amount } = this.transferForm.value;
        const amt = Number(amount);
        this.pending.set({ amount: amt, to: toAccountId, coins: this.calcCoins(amt) });

        const request: TransferRequest = {
            fromAccountId: this.authService.accountId!,
            toAccountId,
            amount: amt,
            idempotencyKey: crypto.randomUUID()
        };

        this.isLoading.set(true);
        const startedAt = Date.now();

        this.transferService.transfer(request).subscribe({
            next: (response) => {
                this.afterDelay(startedAt, 1300, () => {
                    this.transferResponse.set(response);
                    this.isLoading.set(false);
                    // Bump the shared points total -> navbar updates live.
                    this.rewardService.loadPoints(this.authService.accountId!);
                });
            },
            error: (error) => {
                this.afterDelay(startedAt, 800, () => {
                    this.failedTransaction.set({
                        message: error.error?.error || 'Transaction failed due to an unknown error. Please contact support.',
                        toAccountId,
                        amount: amt
                    });
                    this.isLoading.set(false);
                });
            }
        });
    }

    toggleInfo(): void { this.showInfo.update(v => !v); }

    private afterDelay(startedAt: number, minMs: number, fn: () => void): void {
        const wait = Math.max(0, minMs - (Date.now() - startedAt));
        setTimeout(fn, wait);
    }

    resetForm(): void {
        this.transferResponse.set(null);
        this.failedTransaction.set(null);
        this.pending.set(null);
        this.transferForm.reset();
        this.submitted.set(false);
        this.coinsToEarn.set(0);
    }

    get earnedCoins(): number {
        return this.pending()?.coins ?? 0;
    }
}
