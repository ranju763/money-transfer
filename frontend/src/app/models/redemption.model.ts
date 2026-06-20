export interface Redemption {
    id: string;
    partner: string;
    promotionTitle: string;
    description: string;
    discountPercent: number;
    minSpend: number;
    maxDiscount: number;
    category: string;
    coinsSpent: number;
    code: string;
    createdOn: string;
    expiresOn: string;
}
