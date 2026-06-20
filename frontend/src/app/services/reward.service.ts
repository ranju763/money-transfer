import { Injectable } from '@angular/core';
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

    constructor(private http: HttpClient) { }

    fetchRewards(accountId: string): Observable<Reward[]> {
        return this.http.get<Reward[]>(`${this.API_ENDPOINT}/${accountId}`);
    }

    fetchSummary(accountId: string): Observable<RewardSummary> {
        return this.http.get<RewardSummary>(`${this.API_ENDPOINT}/${accountId}/summary`);
    }
}
