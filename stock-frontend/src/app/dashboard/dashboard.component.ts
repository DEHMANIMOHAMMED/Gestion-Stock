import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardService, DashboardSummary, LowStockAlert } from './dashboard.service';
import { AiExecutiveDashboard, AiProductHealth, AiService } from '../ai-reports/ai.service';
import { LoadingStateComponent } from '../shared/ui/loading-state.component';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { StatCardComponent } from '../shared/ui/stat-card.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent, StatCardComponent, LoadingStateComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private aiService = inject(AiService);

  summary = signal<DashboardSummary | null>(null);
  aiDashboard = signal<AiExecutiveDashboard | null>(null);
  alerts = signal<LowStockAlert[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  criticalAlerts = computed(() =>
    this.alerts().filter((alert) => alert.quantity === 0)
  );

  todayActions = computed(() => {
    const dashboard = this.aiDashboard();
    if (!dashboard) {
      return [];
    }
    return [
      ...dashboard.topRisks.slice(0, 3).map((item) => `Traiter ${item.productName}: ${item.actionRecommendation}`),
      ...dashboard.supplierIssues.slice(0, 2).map((item) => `Verifier fournisseur ${item.supplierName}: ${item.reason}`)
    ].slice(0, 5);
  });

  businessHealth = computed(() => {
    const dashboard = this.aiDashboard();
    if (!dashboard) {
      return { label: 'Indisponible', tone: 'warning' };
    }
    if (dashboard.stockHealthAverage >= 75 && dashboard.highRiskCount === 0) {
      return { label: 'Stable', tone: 'success' };
    }
    if (dashboard.stockHealthAverage >= 55) {
      return { label: 'A surveiller', tone: 'warning' };
    }
    return { label: 'Prioritaire', tone: 'danger' };
  });

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);

    this.dashboardService.getSummary().subscribe({
      next: (summary) => this.summary.set(summary),
      error: () => {
        this.error.set('Impossible de charger le tableau de bord.');
        this.loading.set(false);
      }
    });

    this.dashboardService.getLowStockAlerts().subscribe({
      next: (alerts) => {
        this.alerts.set(alerts);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les alertes.');
        this.loading.set(false);
      }
    });

    this.aiService.getExperienceDashboard().subscribe({
      next: (dashboard) => this.aiDashboard.set(dashboard),
      error: () => this.aiDashboard.set(null)
    });
  }

  healthColor(product: AiProductHealth): string {
    if (product.healthScore < 45) {
      return '#ef4444';
    }
    if (product.healthScore < 70) {
      return '#f59e0b';
    }
    return '#16a34a';
  }

  typeLabel(type: string): string {
    const labels: Record<string, string> = {
      IN: 'Entree',
      OUT: 'Sortie',
      ADJUST: 'Ajustement'
    };
    return labels[type] ?? type;
  }
}
