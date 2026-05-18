import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AiService, ExecutiveTimeline } from '../ai-reports/ai.service';

@Component({
  selector: 'app-daily-report',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './daily-report.component.html',
  styleUrls: ['./daily-report.component.scss']
})
export class DailyReportComponent implements OnInit {
  private aiService = inject(AiService);

  selectedDate = signal(new Date().toISOString().slice(0, 10));
  report = signal<ExecutiveTimeline | null>(null);
  loading = signal(false);
  message = signal<string | null>(null);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.message.set(null);
    this.aiService.getExecutiveTimeline(this.selectedDate()).subscribe({
      next: (report) => {
        this.report.set(report);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger le daily report.');
        this.loading.set(false);
      }
    });
  }

  exportCsv(): void {
    this.aiService.exportDailyReportCsv(this.selectedDate()).subscribe({
      next: (blob) => {
        this.download(blob, `daily-report-${this.selectedDate()}.csv`);
        this.message.set('Export CSV genere.');
      },
      error: () => this.error.set("Impossible d'exporter le CSV.")
    });
  }

  exportPdf(): void {
    this.aiService.exportDailyReportPdf(this.selectedDate()).subscribe({
      next: (blob) => {
        this.download(blob, `daily-report-${this.selectedDate()}.pdf`);
        this.message.set('Export PDF genere.');
      },
      error: () => this.error.set("Impossible d'exporter le PDF.")
    });
  }

  severityLabel(severity: string): string {
    return severity === 'CRITICAL' ? 'Critique' : severity === 'WARNING' ? 'A surveiller' : severity === 'SUCCESS' ? 'Termine' : 'Info';
  }

  private download(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
