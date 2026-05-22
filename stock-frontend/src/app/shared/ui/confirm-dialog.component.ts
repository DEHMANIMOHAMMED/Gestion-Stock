import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="overlay" *ngIf="open" role="presentation" (click)="cancel.emit()">
      <section class="dialog" role="dialog" aria-modal="true" [attr.aria-label]="title" (click)="$event.stopPropagation()">
        <h2>{{ title }}</h2>
        <p>{{ message }}</p>
        <div class="actions">
          <button type="button" class="secondary" (click)="cancel.emit()">Annuler</button>
          <button type="button" class="danger" (click)="confirm.emit()">{{ confirmLabel }}</button>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .overlay {
      align-items: center;
      background: rgba(23, 32, 42, 0.42);
      display: flex;
      inset: 0;
      justify-content: center;
      padding: 20px;
      position: fixed;
      z-index: 200;
    }

    .dialog {
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 16px;
      box-shadow: 0 30px 90px rgba(0, 0, 0, 0.25);
      max-width: 440px;
      padding: 24px;
      width: 100%;
    }

    h2 {
      font-size: 22px;
      margin: 0 0 8px;
    }

    p {
      color: var(--muted);
      margin: 0;
    }

    .actions {
      display: flex;
      gap: 10px;
      justify-content: flex-end;
      margin-top: 22px;
    }

    button {
      border: 0;
      border-radius: 10px;
      cursor: pointer;
      font-weight: 800;
      padding: 11px 14px;
    }

    .secondary {
      background: #eef2f0;
      color: var(--text);
    }

    .danger {
      background: var(--red);
      color: #fff;
    }
  `]
})
export class ConfirmDialogComponent {
  @Input() open = false;
  @Input() title = 'Confirmer';
  @Input() message = 'Cette action est irreversible.';
  @Input() confirmLabel = 'Confirmer';
  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
}
