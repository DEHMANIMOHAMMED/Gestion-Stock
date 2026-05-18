import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { AiService, AiStockoutRisk } from './ai.service';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alerts.component.html',
  styleUrls: ['./alerts.component.scss']
})
export class AlertsComponent implements OnInit {
  private aiService = inject(AiService);

  alerts = signal<AiStockoutRisk[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.aiService.getStockoutRisks().subscribe({
      next: (alerts) => {
        this.alerts.set(alerts);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les alertes IA.');
        this.loading.set(false);
      }
    });
  }

  criticalCount(): number {
    return this.alerts().filter((alert) => alert.riskLevel === 'HIGH').length;
  }

  warningCount(): number {
    return this.alerts().filter((alert) => alert.riskLevel === 'MEDIUM').length;
  }
}
