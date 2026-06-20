import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Reward } from '../models/reward.model';
import { RewardSummary } from '../models/reward-summary.model';
import { environment } from 'environments/environment';

@Injectable({
    providedIn: 'root'
})
export class RewardService {
    private readonly API_ENDPOINT = environment.baseUrl + '/rewards';

    /** Shared, app-wide reward points total (the navbar and any page can react to it). */
    private readonly _points = signal<number>(0);
    readonly points = this._points.asReadonly();

    constructor(private http: HttpClient) { }

    fetchRewards(accountId: string): Observable<Reward[]> {
        return this.http.get<Reward[]>(`${this.API_ENDPOINT}/${accountId}`);
    }

    fetchSummary(accountId: string): Observable<RewardSummary> {
        return this.http.get<RewardSummary>(`${this.API_ENDPOINT}/${accountId}/summary`);
    }

    /** Refresh the shared points total from the backend (call after an eligible transfer). */
    loadPoints(accountId: string): void {
        this.fetchSummary(accountId).subscribe({
            next: (s) => this._points.set(s.availableCoins),
            error: () => { /* leave previous value on failure */ }
        });
    }
}
