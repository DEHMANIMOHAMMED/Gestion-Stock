import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loading-state',
  standalone: true,
  template: `
    <section class="loading-state">
      <span></span>
      {{ message }}
    </section>
  `,
  styles: [`
    .loading-state {
      align-items: center;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 14px;
      color: var(--muted);
      display: flex;
      gap: 10px;
      padding: 16px;
    }

    span {
      animation: spin 0.8s linear infinite;
      border: 3px solid #dce5df;
      border-top-color: var(--green);
      border-radius: 50%;
      height: 18px;
      width: 18px;
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }
  `]
})
export class LoadingStateComponent {
  @Input() message = 'Chargement...';
}
