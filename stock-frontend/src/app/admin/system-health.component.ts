import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { AiService, SystemHealth } from '../ai-reports/ai.service';

@Component({
  selector: 'app-system-health',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './system-health.component.html',
  styleUrls: ['./system-health.component.scss']
})
export class SystemHealthComponent implements OnInit {
  private aiService = inject(AiService);

  health = signal<SystemHealth | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);

  degradedServices = computed(() => this.health()?.services.filter((service) => service.status !== 'UP') ?? []);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.aiService.getSystemHealth().subscribe({
      next: (health) => {
        this.health.set(health);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger le system health.');
        this.loading.set(false);
      }
    });
  }

  statusClass(status: string): string {
    return status === 'UP' || status === 'SUCCEEDED' ? 'up' : status === 'RUNNING' || status === 'QUEUED' ? 'warn' : 'down';
  }
}
