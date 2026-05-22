import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [CommonModule],
  template: `
    <header class="page-header">
      <div>
        <p *ngIf="eyebrow">{{ eyebrow }}</p>
        <h1>{{ title }}</h1>
        <span *ngIf="description">{{ description }}</span>
      </div>
      <div class="actions">
        <ng-content></ng-content>
      </div>
    </header>
  `,
  styles: [`
    .page-header {
      align-items: center;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 16px;
      box-shadow: var(--shadow);
      display: flex;
      justify-content: space-between;
      gap: 18px;
      padding: 24px;
    }

    p {
      color: var(--green-strong);
      font-size: 12px;
      font-weight: 900;
      margin: 0 0 6px;
      text-transform: uppercase;
    }

    h1 {
      font-size: 34px;
      letter-spacing: 0;
      margin: 0;
    }

    span {
      color: var(--muted);
      display: block;
      margin-top: 8px;
      max-width: 760px;
    }

    .actions {
      align-items: center;
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      justify-content: flex-end;
    }

    @media (max-width: 760px) {
      .page-header,
      .actions {
        align-items: stretch;
        flex-direction: column;
      }
    }
  `]
})
export class PageHeaderComponent {
  @Input({ required: true }) title = '';
  @Input() eyebrow = '';
  @Input() description = '';
}
