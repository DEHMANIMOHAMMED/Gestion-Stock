import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AiForecast, AiForecastBacktest, AiForecastBacktestPoint, AiRecommendationExplanation, AiReorderRecommendation, AiService, AiStockoutRisk } from './ai.service';
import { ProcurementService, Supplier } from '../procurement/procurement.service';

@Component({
  selector: 'app-prediction',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './prediction.component.html',
  styleUrls: ['./prediction.component.scss']
})
export class PredictionComponent implements OnInit {
  private aiService = inject(AiService);
  private procurementService = inject(ProcurementService);

  forecasts = signal<AiForecast[]>([]);
  backtests = signal<AiForecastBacktest[]>([]);
  risks = signal<AiStockoutRisk[]>([]);
  recommendations = signal<AiReorderRecommendation[]>([]);
  suppliers = signal<Supplier[]>([]);
  selectedSupplierId = signal<number | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  explanation = signal<AiRecommendationExplanation | null>(null);
  selectedBacktestProductId = signal<number | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(clearMessages = true): void {
    this.loading.set(true);
    if (clearMessages) {
      this.error.set(null);
      this.success.set(null);
    }

    this.aiService.getDashboard().subscribe({
      next: (dashboard) => {
        this.forecasts.set(dashboard.forecasts.filter((forecast) => forecast.horizonDays === 30));
        this.risks.set(dashboard.stockoutRisks);
        this.recommendations.set(dashboard.reorderRecommendations);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les predictions IA.');
        this.loading.set(false);
      }
    });

    this.procurementService.getSuppliers().subscribe({
      next: (suppliers) => {
        this.suppliers.set(suppliers);
        if (!this.selectedSupplierId() && suppliers.length) {
          this.selectedSupplierId.set(suppliers[0].id);
        }
      },
      error: () => this.error.set('Impossible de charger les fournisseurs.')
    });
    this.aiService.getForecastBacktests(null, 30).subscribe({
      next: (backtests) => {
        this.backtests.set(backtests);
        if (!this.selectedBacktestProductId() && backtests.length) {
          this.selectedBacktestProductId.set(backtests[0].productId);
        }
      },
      error: () => this.error.set('Impossible de charger le backtesting IA.')
    });
  }

  highRiskCount(): number {
    return this.risks().filter((row) => row.riskLevel === 'HIGH').length;
  }

  totalRecommendedOrder(): number {
    return this.recommendations().reduce((sum, row) => sum + row.recommendedQuantity, 0);
  }

  lowConfidenceCount(): number {
    return this.forecasts().filter((forecast) => forecast.confidenceLevel === 'LOW' || forecast.confidenceLevel === 'INSUFFICIENT_DATA').length;
  }

  riskLabel(risk: AiStockoutRisk['riskLevel']): string {
    return {
      HIGH: 'Critique',
      MEDIUM: 'A surveiller',
      LOW: 'Stable'
    }[risk];
  }

  forecastFor(productId: number): AiForecast | undefined {
    return this.forecasts().find((forecast) => forecast.productId === productId);
  }

  selectedBacktest(): AiForecastBacktest | undefined {
    return this.backtests().find((backtest) => backtest.productId === Number(this.selectedBacktestProductId()));
  }

  maxBacktestUnits(backtest: AiForecastBacktest | undefined): number {
    const values = backtest?.points.flatMap((point) => [point.actualUnits, Number(point.predictedUnits)]) ?? [];
    return Math.max(1, ...values);
  }

  backtestBarHeight(point: AiForecastBacktestPoint, key: 'actualUnits' | 'predictedUnits'): number {
    const value = Number(point[key]);
    return Math.max(4, Math.round((value / this.maxBacktestUnits(this.selectedBacktest())) * 92));
  }

  confidenceLabel(forecast: AiForecast | undefined): string {
    if (!forecast) {
      return 'Non calculee';
    }
    return {
      HIGH: 'Fiable',
      MEDIUM: 'Moyenne',
      LOW: 'Faible',
      INSUFFICIENT_DATA: 'Donnees faibles'
    }[forecast.confidenceLevel];
  }

  qualityLabel(level: AiForecastBacktest['qualityLevel']): string {
    return {
      HIGH: 'Fiable',
      MEDIUM: 'Moyen',
      LOW: 'A recalibrer',
      INSUFFICIENT_DATA: 'Donnees faibles'
    }[level];
  }

  recommendationFor(productId: number): AiReorderRecommendation | undefined {
    return this.recommendations().find((recommendation) => recommendation.productId === productId);
  }

  createDraftOrder(row: AiStockoutRisk): void {
    const recommendation = this.recommendationFor(row.productId);
    const supplierId = this.selectedSupplierId();
    if (recommendation?.purchaseOrderId) {
      this.error.set(`Cette recommandation est deja liee a la commande #${recommendation.purchaseOrderId}.`);
      return;
    }
    if (!recommendation || recommendation.recommendedQuantity <= 0) {
      this.error.set('Aucune recommandation exploitable pour ce produit.');
      return;
    }
    if (!recommendation.preferredSupplierId && !supplierId) {
      this.error.set('Ajoute un fournisseur avant de creer une commande.');
      return;
    }

    this.error.set(null);
    this.success.set(null);
    this.procurementService.createPurchaseOrderFromRecommendation(
      recommendation.id,
      recommendation.preferredSupplierId ? null : supplierId
    ).subscribe({
      next: (order) => {
        this.success.set(`Commande brouillon #${order.id} creee pour ${row.productName}.`);
        this.load(false);
      },
      error: (error) => this.error.set(error?.error?.detail || 'Creation de commande brouillon impossible.')
    });
  }

  explain(row: AiStockoutRisk): void {
    const recommendation = this.recommendationFor(row.productId);
    if (!recommendation) {
      this.error.set('Aucune recommandation IA a expliquer.');
      return;
    }
    this.aiService.explainRecommendation(recommendation.id).subscribe({
      next: (explanation) => {
        this.explanation.set(explanation);
        this.error.set(null);
      },
      error: () => this.error.set('Explication IA indisponible.')
    });
  }
}
