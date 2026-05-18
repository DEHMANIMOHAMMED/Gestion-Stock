import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AiRecommendationExplanation, AiService, AiWhatIfResponse, ExecutiveTimeline, ExecutiveTimelineItem } from '../ai-reports/ai.service';
import { ProcurementService } from '../procurement/procurement.service';
import { NotificationService } from '../shared/notification.service';

@Component({
  selector: 'app-executive-timeline',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './executive-timeline.component.html',
  styleUrls: ['./executive-timeline.component.scss']
})
export class ExecutiveTimelineComponent implements OnInit {
  private aiService = inject(AiService);
  private procurementService = inject(ProcurementService);
  private notificationService = inject(NotificationService);

  timeline = signal<ExecutiveTimeline | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  message = signal<string | null>(null);
  actionLoading = signal<string | null>(null);
  explanation = signal<AiRecommendationExplanation | null>(null);
  whatIfResult = signal<AiWhatIfResponse | null>(null);
  whatIfQuantities: Record<number, number> = {};
  whatIfLeadTimes: Record<number, number> = {};

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.message.set(null);
    this.aiService.getExecutiveTimeline().subscribe({
      next: (timeline) => {
        this.timeline.set(timeline);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger la timeline dirigeant.");
        this.loading.set(false);
      }
    });
  }

  severityLabel(severity: string): string {
    return severity === 'CRITICAL' ? 'Critique' : severity === 'WARNING' ? 'A surveiller' : severity === 'SUCCESS' ? 'Termine' : 'Info';
  }

  typeLabel(type: string): string {
    return type.replace('_', ' ');
  }

  markNotificationRead(item: ExecutiveTimelineItem): void {
    if (!item.notificationId) {
      return;
    }
    this.runAction(`notification-${item.notificationId}`, () => {
      this.notificationService.markReadRequest(item.notificationId!).subscribe({
        next: () => {
          this.message.set('Notification marquee comme lue.');
          this.load();
          this.notificationService.load();
          this.actionLoading.set(null);
        },
        error: (error) => this.failAction(error, 'Impossible de marquer la notification.')
      });
    });
  }

  createOrder(item: ExecutiveTimelineItem): void {
    if (!item.recommendationId) {
      return;
    }
    this.runAction(`recommendation-${item.recommendationId}`, () => {
      this.procurementService.createPurchaseOrderFromRecommendation(item.recommendationId!, null).subscribe({
        next: (order) => {
          this.message.set(`Commande #${order.id} creee depuis la recommandation IA.`);
          this.load();
          this.actionLoading.set(null);
        },
        error: (error) => this.failAction(error, 'Impossible de creer la commande.')
      });
    });
  }

  explainRecommendation(item: ExecutiveTimelineItem): void {
    if (!item.recommendationId) {
      return;
    }
    this.runAction(`explain-${item.recommendationId}`, () => {
      this.aiService.explainRecommendation(item.recommendationId!).subscribe({
        next: (explanation) => {
          this.explanation.set(explanation);
          this.message.set('Explication IA chargee.');
          this.actionLoading.set(null);
        },
        error: (error) => this.failAction(error, "Impossible de charger l'explication IA.")
      });
    });
  }

  simulateWhatIf(item: ExecutiveTimelineItem): void {
    if (!item.productId) {
      return;
    }
    const quantity = this.whatIfQuantities[item.productId] || 20;
    const leadTime = this.whatIfLeadTimes[item.productId] || 7;
    this.runAction(`what-if-${item.productId}`, () => {
      this.aiService.whatIf(item.productId!, quantity, leadTime).subscribe({
        next: (result) => {
          this.whatIfResult.set(result);
          this.message.set('Simulation what-if calculee.');
          this.actionLoading.set(null);
        },
        error: (error) => this.failAction(error, 'Impossible de lancer la simulation.')
      });
    });
  }

  defaultQuantity(item: ExecutiveTimelineItem): number {
    if (!item.productId) {
      return 20;
    }
    this.whatIfQuantities[item.productId] ??= 20;
    return this.whatIfQuantities[item.productId];
  }

  defaultLeadTime(item: ExecutiveTimelineItem): number {
    if (!item.productId) {
      return 7;
    }
    this.whatIfLeadTimes[item.productId] ??= 7;
    return this.whatIfLeadTimes[item.productId];
  }

  private runAction(key: string, action: () => void): void {
    if (this.actionLoading()) {
      return;
    }
    this.message.set(null);
    this.error.set(null);
    this.actionLoading.set(key);
    action();
  }

  private failAction(error: { error?: { detail?: string } }, fallback: string): void {
    this.error.set(error?.error?.detail || fallback);
    this.actionLoading.set(null);
  }
}
