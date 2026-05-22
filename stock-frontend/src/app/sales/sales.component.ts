import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Product, ProductService } from '../products/product.service';
import { DemandAnalytics, Sale, SalesService, SalesSummary } from './sales.service';

interface SaleLineForm {
  productId: number | null;
  quantity: number;
  unitPrice: number;
}

@Component({
  selector: 'app-sales',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sales.component.html',
  styleUrls: ['./sales.component.scss']
})
export class SalesComponent implements OnInit {
  private salesService = inject(SalesService);
  private productService = inject(ProductService);

  products = signal<Product[]>([]);
  sales = signal<Sale[]>([]);
  summary = signal<SalesSummary | null>(null);
  analytics = signal<DemandAnalytics | null>(null);
  loading = signal(false);
  message = signal('');
  customerName = signal('');
  channel = signal('STORE');
  lines = signal<SaleLineForm[]>([{ productId: null, quantity: 1, unitPrice: 0 }]);

  total = computed(() =>
    this.lines().reduce((sum, line) => sum + Math.max(0, Number(line.quantity || 0)) * Math.max(0, Number(line.unitPrice || 0)), 0)
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.message.set('');
    this.productService.getAll().subscribe({
      next: (products) => this.products.set(products),
      error: () => this.message.set('Impossible de charger les produits.')
    });
    this.salesService.getRecent().subscribe({
      next: (sales) => {
        this.sales.set(sales);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Impossible de charger les ventes.');
        this.loading.set(false);
      }
    });
    this.salesService.getSummary().subscribe({
      next: (summary) => this.summary.set(summary),
      error: () => this.message.set('Impossible de charger le resume ventes.')
    });
    this.salesService.getAnalytics().subscribe({
      next: (analytics) => {
        this.analytics.set(analytics);
        this.summary.set(analytics.summary);
      },
      error: () => this.message.set('Impossible de charger le dashboard demande.')
    });
  }

  addLine(): void {
    this.lines.update((lines) => [...lines, { productId: null, quantity: 1, unitPrice: 0 }]);
  }

  removeLine(index: number): void {
    this.lines.update((lines) => lines.filter((_, current) => current !== index));
  }

  updateLine(index: number, patch: Partial<SaleLineForm>): void {
    this.lines.update((lines) => lines.map((line, current) => current === index ? { ...line, ...patch } : line));
  }

  submit(): void {
    const validLines = this.lines()
      .filter((line) => line.productId && line.quantity > 0)
      .map((line) => ({
        productId: Number(line.productId),
        quantity: Number(line.quantity),
        unitPrice: Number(line.unitPrice || 0)
      }));
    if (!validLines.length) {
      this.message.set('Ajoute au moins une ligne de vente valide.');
      return;
    }
    this.loading.set(true);
    this.salesService.create({
      customerName: this.customerName().trim() || null,
      channel: this.channel(),
      lines: validLines
    }).subscribe({
      next: () => {
        this.customerName.set('');
        this.channel.set('STORE');
        this.lines.set([{ productId: null, quantity: 1, unitPrice: 0 }]);
        this.message.set('Vente enregistree et stock decremente.');
        this.load();
      },
      error: (error) => {
        this.message.set(error?.error?.detail || 'La vente a echoue.');
        this.loading.set(false);
      }
    });
  }

  maxDailyUnits(): number {
    return Math.max(1, ...((this.analytics()?.dailySeries ?? []).map((point) => point.unitsSold)));
  }

  maxWeekdayUnits(): number {
    return Math.max(1, ...((this.analytics()?.seasonality?.weekdaySeries ?? []).map((point) => point.unitsSold)));
  }

  barHeight(units: number): number {
    return Math.max(6, Math.round((units / this.maxDailyUnits()) * 120));
  }

  weekdayBarHeight(units: number): number {
    return Math.max(6, Math.round((units / this.maxWeekdayUnits()) * 86));
  }

  varianceClass(value: number): string {
    if (value >= 35) {
      return 'danger';
    }
    if (value <= -35) {
      return 'warning';
    }
    return 'ok';
  }
}
