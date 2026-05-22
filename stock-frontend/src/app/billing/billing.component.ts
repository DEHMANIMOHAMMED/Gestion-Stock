import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { BillingService, BillingSubscription } from './billing.service';

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './billing.component.html',
  styleUrls: ['./billing.component.scss']
})
export class BillingComponent implements OnInit {
  private billingService = inject(BillingService);

  subscription = signal<BillingSubscription | null>(null);
  loading = signal(false);
  actionLoading = signal<string | null>(null);
  message = signal<string | null>(null);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.billingService.subscription().subscribe({
      next: (subscription) => {
        this.subscription.set(subscription);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger l'abonnement.");
        this.loading.set(false);
      }
    });
  }

  startCheckout(planCode: 'STARTER' | 'PRO'): void {
    this.actionLoading.set(planCode);
    this.message.set(null);
    this.error.set(null);
    this.billingService.checkout(planCode).subscribe({
      next: (response) => {
        this.actionLoading.set(null);
        if (response.checkoutUrl) {
          window.location.href = response.checkoutUrl;
          return;
        }
        this.message.set(response.message);
        this.load();
      },
      error: (error) => {
        this.actionLoading.set(null);
        this.error.set(error?.error?.detail ?? 'Impossible de demarrer le paiement.');
      }
    });
  }

  cancelSubscription(): void {
    const current = this.subscription();
    if (!current || current.cancelAtPeriodEnd) {
      return;
    }
    const confirmed = window.confirm("Confirmer la resiliation de l'abonnement a la fin de la periode en cours ?");
    if (!confirmed) {
      return;
    }
    this.actionLoading.set('CANCEL');
    this.message.set(null);
    this.error.set(null);
    this.billingService.cancelSubscription().subscribe({
      next: (subscription) => {
        this.subscription.set(subscription);
        this.actionLoading.set(null);
        this.message.set("La resiliation est programmee a la fin de la periode en cours.");
      },
      error: (error) => {
        this.actionLoading.set(null);
        this.error.set(error?.error?.detail ?? "Impossible de programmer la resiliation.");
      }
    });
  }

  statusLabel(status: string): string {
    return status.replaceAll('_', ' ').toLowerCase();
  }
}
