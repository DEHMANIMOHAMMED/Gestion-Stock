import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Subscription, forkJoin, interval, of, switchMap } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { AiDashboard, AiRun, AiService } from './ai.service';
import { StockService } from '../stock/stock.service';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss']
})
export class ReportsComponent implements OnInit, OnDestroy {
  private aiService = inject(AiService);
  private stockService = inject(StockService);
  private polling?: Subscription;

  dashboard = signal<AiDashboard | null>(null);
  latestRun = signal<AiRun | null>(null);
  runs = signal<AiRun[]>([]);
  loading = signal(true);
  recalculating = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.polling?.unsubscribe();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      dashboard: this.aiService.getDashboard(),
      runs: this.aiService.getRuns().pipe(catchError(() => of([] as AiRun[])))
    }).subscribe({
      next: ({ dashboard, runs }) => {
        this.dashboard.set(dashboard);
        this.runs.set(runs);
        this.latestRun.set(runs[0] ?? null);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les rapports IA.');
        this.loading.set(false);
      }
    });
  }

  requestRun(): void {
    this.recalculating.set(true);
    this.error.set(null);
    this.aiService.requestRun().subscribe({
      next: (run) => {
        this.latestRun.set(run);
        this.startPolling();
      },
      error: () => {
        this.error.set('Impossible de lancer le recalcul IA.');
        this.recalculating.set(false);
      }
    });
  }

  exportCsv(): void {
    this.stockService.exportCsv({}).subscribe((blob) => this.download(blob, 'stock-history.csv'));
  }

  exportPdf(): void {
    this.stockService.exportPdf({}).subscribe((blob) => this.download(blob, 'stock-history.pdf'));
  }

  private download(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }

  private startPolling(): void {
    this.polling?.unsubscribe();
    this.polling = interval(1500)
      .pipe(
        switchMap(() => this.aiService.getRuns()),
        finalize(() => this.recalculating.set(false))
      )
      .subscribe({
        next: (runs) => {
          const latest = runs[0] ?? null;
          this.runs.set(runs);
          this.latestRun.set(latest);
          if (latest && latest.status !== 'QUEUED' && latest.status !== 'RUNNING') {
            this.polling?.unsubscribe();
            this.recalculating.set(false);
            this.load();
          }
        },
        error: () => {
          this.error.set('Impossible de suivre le statut du recalcul IA.');
          this.polling?.unsubscribe();
          this.recalculating.set(false);
        }
      });
  }
}
