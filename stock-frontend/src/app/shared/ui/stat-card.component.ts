import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="stat-card" [class.warning]="tone === 'warning'" [class.danger]="tone === 'danger'">
      <small>{{ label }}</small>
      <strong>{{ value }}</strong>
      <span *ngIf="hint">{{ hint }}</span>
    </article>
  `,
  styles: [`
    .stat-card {
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 16px;
      box-shadow: var(--shadow);
      display: grid;
      gap: 8px;
      min-height: 132px;
      padding: 20px;
    }

    small,
    span {
      color: var(--muted);
    }

    small {
      font-size: 13px;
      font-weight: 800;
    }

    strong {
      color: var(--text);
      font-size: 34px;
      line-height: 1;
    }

    .warning strong {
      color: var(--amber);
    }

    .danger strong {
      color: var(--red);
    }
  `]
})
export class StatCardComponent {
  @Input({ required: true }) label = '';
  @Input({ required: true }) value: string | number = '';
  @Input() hint = '';
  @Input() tone: 'default' | 'warning' | 'danger' = 'default';
}
