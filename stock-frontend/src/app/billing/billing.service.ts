import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../environment';

export interface BillingSubscription {
  planCode: string;
  status: string;
  stripeConfigured: boolean;
  stripeCustomerId?: string | null;
  stripeSubscriptionId?: string | null;
  currentPeriodEnd?: string | null;
  trialEndsAt?: string | null;
  cancelAtPeriodEnd: boolean;
}

export interface BillingCheckoutResponse {
  checkoutUrl?: string | null;
  provider: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class BillingService {
  constructor(private http: HttpClient) {}

  subscription(): Observable<BillingSubscription> {
    return this.http.get<BillingSubscription>(`${environment.apiUrl}/billing/subscription`);
  }

  checkout(planCode: 'STARTER' | 'PRO'): Observable<BillingCheckoutResponse> {
    const origin = window.location.origin;
    return this.http.post<BillingCheckoutResponse>(`${environment.apiUrl}/billing/checkout-session`, {
      planCode,
      successUrl: `${origin}/billing?payment=success`,
      cancelUrl: `${origin}/billing?payment=cancel`
    });
  }

  cancelSubscription(): Observable<BillingSubscription> {
    return this.http.post<BillingSubscription>(`${environment.apiUrl}/billing/subscription/cancel`, {});
  }
}
