import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { StockService } from './stock.service';

@Component({
  selector: 'app-movement-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './movement-form.component.html',
  styleUrls: ['./movement-form.component.scss']
})
export class MovementFormComponent {
  @Input() productId!: number;
  @Input() set initialType(value: 'IN' | 'OUT' | 'ADJUST' | null) {
    if (value) {
      this.form.patchValue({ type: value });
    }
  }
  @Output() closed = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private stockService = inject(StockService);

  message = '';

  form = this.fb.group({
    quantity: [0, [Validators.required, Validators.min(1)]],
    type: ['IN', [Validators.required]]
  });

  save() {
    this.message = '';
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }

    const { quantity, type } = this.form.value;
    this.stockService
      .registerMovement({
        productId: this.productId,
        quantity: Number(quantity),
        type: type!
      })
      .subscribe({
        next: () => this.closed.emit(),
        error: (err) => {
          this.message = this.readError(err, "Impossible d'enregistrer le mouvement");
        }
      });
  }

  private readError(err: unknown, fallback: string): string {
    const rawError = (err as { error?: unknown })?.error;
    const error = typeof rawError === 'string' ? this.parseError(rawError) : rawError as {
      detail?: string;
      message?: string;
      errors?: Record<string, string>;
    };
    if (error?.detail) {
      return error.detail;
    }
    if (error?.message) {
      return error.message;
    }
    if (error?.errors) {
      return Object.values(error.errors).join(' ');
    }
    const httpError = err as { status?: number; message?: string; statusText?: string };
    if (httpError?.status || httpError?.message) {
      return `${fallback} (${httpError.status ?? 'network'} ${httpError.statusText ?? ''}) ${httpError.message ?? ''}`.trim();
    }
    return fallback;
  }

  private parseError(rawError: string): { detail?: string; message?: string; errors?: Record<string, string> } {
    try {
      return JSON.parse(rawError);
    } catch {
      return { message: rawError };
    }
  }
}
