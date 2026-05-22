import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  template: `
    <section class="empty-state">
      <strong>{{ title }}</strong>
      <p>{{ message }}</p>
    </section>
  `,
  styles: [`
    .empty-state {
      background: var(--surface);
      border: 1px dashed #cfd8d3;
      border-radius: 16px;
      color: var(--muted);
      padding: 28px;
      text-align: center;
    }

    strong {
      color: var(--text);
      display: block;
      font-size: 18px;
      margin-bottom: 6px;
    }

    p {
      margin: 0;
    }
  `]
})
export class EmptyStateComponent {
  @Input() title = 'Aucune donnee';
  @Input() message = 'Il n y a rien a afficher pour le moment.';
}
