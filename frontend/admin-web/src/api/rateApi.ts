import { del, get, post, put } from './httpClient';

export type PricingRuleType = 'RECURRING_SEASON' | 'DATE_OVERRIDE';

export type PricingRuleRoomTypePrice = {
  roomTypeId: string;
  normalNightlyPrice: number;
  weekendNightlyPrice: number | null;
};

export type PricingRule = {
  id: string;
  hotelId: string;
  active: boolean;
  name: string;
  ruleType: PricingRuleType;
  recurringStartMonth: number | null;
  recurringStartDay: number | null;
  recurringEndMonth: number | null;
  recurringEndDay: number | null;
  startDate: string | null;
  endDate: string | null;
  priority: number;
  roomTypePrices: PricingRuleRoomTypePrice[];
};

export type CreatePricingRuleRequest = Omit<PricingRule, 'id' | 'active'>;

export function findPricingRules(accessToken: string, hotelId: string) {
  return get<PricingRule[]>('/pricing-rules', accessToken, new URLSearchParams({ hotelId }));
}

export function createPricingRule(accessToken: string, request: CreatePricingRuleRequest) {
  return post<PricingRule>('/pricing-rules', request, accessToken);
}

export function updatePricingRule(accessToken: string, id: string, request: CreatePricingRuleRequest) {
  return put<PricingRule>(`/pricing-rules/${id}`, request, accessToken);
}

export function deactivatePricingRule(accessToken: string, id: string) {
  return del(`/pricing-rules/${id}`, accessToken);
}
