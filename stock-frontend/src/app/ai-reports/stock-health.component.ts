import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AiProductHealth, AiService, AiWhatIfResponse } from './ai.service';
import { ProcurementService } from '../procurement/procurement.service';

@Component({
  selector: 'app-stock-health',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './stock-health.component.html',
  styleUrls: ['./ai-reports.scss', './stock-health.component.scss']
})
export class StockHealthComponent implements OnInit {
  private aiService = inject(AiService);
  private procurementService = inject(ProcurementService);

  rows = signal<AiProductHealth[]>([]);
  selected = signal<AiProductHealth | null>(null);
  orderQuantity = signal(0);
  leadTimeDays = signal(7);
  whatIfResult = signal<AiWhatIfResponse | null>(null);
  message = signal<string | null>(null);
  loading = signal(true);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.aiService.getStockHealth().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        if (!this.selected() && rows.length) {
          this.pick(rows[0]);
        }
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Impossible de charger le Stock Health Score.');
        this.loading.set(false);
      }
    });
  }

  pick(row: AiProductHealth): void {
    this.selected.set(row);
    this.orderQuantity.set(Math.max(row.recommendedQuantity, 0));
    this.leadTimeDays.set(7);
    this.whatIfResult.set(null);
  }

  simulate(): void {
    const row = this.selected();
    if (!row) {
      return;
    }
    this.aiService.whatIf(row.productId, this.orderQuantity(), this.leadTimeDays()).subscribe({
      next: (result) => this.whatIfResult.set(result),
      error: () => this.message.set('Simulation impossible.')
    });
  }

  createOrder(row: AiProductHealth): void {
    if (!row.recommendationId) {
      this.message.set('Aucune recommandation IA exploitable pour ce produit.');
      return;
    }
    this.procurementService.createPurchaseOrderFromRecommendation(row.recommendationId, null).subscribe({
      next: (order) => {
        this.message.set(`Commande brouillon #${order.id} creee pour ${row.productName}.`);
        this.load();
      },
      error: (error) => this.message.set(error?.error?.detail || 'Creation de commande impossible.')
    });
  }
}
