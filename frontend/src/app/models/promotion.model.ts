export interface Promotion {
    id: number;
    partner: string;
    title: string;
    description: string;
    coinCost: number;
    discountPercent: number;
    minSpend: number;
    maxDiscount: number;
    validityDays: number;
    category: string;
}
