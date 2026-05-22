import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { AiService, ModelRegistry, ModelRegistryModel } from '../ai-reports/ai.service';

@Component({
  selector: 'app-model-registry',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './model-registry.component.html',
  styleUrls: ['./model-registry.component.scss']
})
export class ModelRegistryComponent implements OnInit {
  private aiService = inject(AiService);

  registry = signal<ModelRegistry | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);

  modelsToWatch = computed(() => this.registry()?.models.filter((model) => model.status !== 'HEALTHY') ?? []);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.aiService.getModelRegistry().subscribe({
      next: (registry) => {
        this.registry.set(registry);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger le model registry.');
        this.loading.set(false);
      }
    });
  }

  statusLabel(status: ModelRegistryModel['status']): string {
    return {
      HEALTHY: 'Sain',
      WATCH: 'A surveiller',
      RECALIBRATE: 'Recalibrer'
    }[status];
  }
}
