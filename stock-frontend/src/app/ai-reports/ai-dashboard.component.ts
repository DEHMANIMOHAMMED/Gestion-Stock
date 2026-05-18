import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AiExecutiveDashboard, AiService } from './ai.service';
import { ProcurementService } from '../procurement/procurement.service';

@Component({
  selector: 'app-ai-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './ai-dashboard.component.html',
  styleUrls: ['./ai-reports.scss', './ai-dashboard.component.scss']
})
export class AiDashboardComponent implements OnInit {
  private aiService = inject(AiService);
  private procurementService = inject(ProcurementService);

  dashboard = signal<AiExecutiveDashboard | null>(null);
  loading = signal(true);
  message = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.message.set(null);
    this.aiService.getExperienceDashboard().subscribe({
      next: (dashboard) => {
        this.dashboard.set(dashboard);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Impossible de charger le cockpit IA.');
        this.loading.set(false);
      }
    });
  }

  createOrder(recommendationId: number | null): void {
    if (!recommendationId) {
      return;
    }
    this.message.set(null);
    this.procurementService.createPurchaseOrderFromRecommendation(recommendationId, null).subscribe({
      next: (order) => {
        this.message.set(`Commande brouillon #${order.id} creee depuis la recommandation IA.`);
        this.load();
      },
      error: (error) => this.message.set(error?.error?.detail || 'Creation de commande impossible.')
    });
  }
}
