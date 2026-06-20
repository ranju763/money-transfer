import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Promotion } from '../models/promotion.model';
import { Redemption } from '../models/redemption.model';
import { environment } from 'environments/environment';

@Injectable({
    providedIn: 'root'
})
export class PromotionService {
    private readonly API_ENDPOINT = environment.baseUrl + '/promotions';

    constructor(private http: HttpClient) { }

    fetchPromotions(): Observable<Promotion[]> {
        return this.http.get<Promotion[]>(this.API_ENDPOINT);
    }

    fetchRedemptions(accountId: string): Observable<Redemption[]> {
        return this.http.get<Redemption[]>(`${this.API_ENDPOINT}/${accountId}/redemptions`);
    }

    redeem(accountId: string, promotionId: number): Observable<Redemption> {
        return this.http.post<Redemption>(`${this.API_ENDPOINT}/${accountId}/redeem`, { promotionId });
    }
}
