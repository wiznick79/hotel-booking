import { del, get, patch, patchWithoutBody, post } from './httpClient';

export type DiscountCode = {
  id: string;
  hotelId: string;
  code: string;
  percentage: number | null;
  fixedAmount: number | null;
  validFrom: string;
  validUntil: string;
  maximumUses: number | null;
  usedCount: number;
  active: boolean;
};

export type CreateDiscountCodeRequest = Omit<DiscountCode, 'id' | 'usedCount' | 'active'>;

export function findDiscountCodes(accessToken: string, hotelId: string) {
  return get<DiscountCode[]>('/discount-codes', accessToken, new URLSearchParams({ hotelId }));
}

export function createDiscountCode(accessToken: string, request: CreateDiscountCodeRequest) {
  return post<DiscountCode>('/discount-codes', request, accessToken);
}

export function deactivateDiscountCode(accessToken: string, id: string) {
  return patchWithoutBody(`/discount-codes/${id}/deactivate`, accessToken);
}

export function deleteDiscountCode(accessToken: string, id: string) {
  return del(`/discount-codes/${id}`, accessToken);
}

export function updateDiscountCode(accessToken: string, id: string, request: CreateDiscountCodeRequest) {
  return patch<DiscountCode>(`/discount-codes/${id}`, request, accessToken);
}
