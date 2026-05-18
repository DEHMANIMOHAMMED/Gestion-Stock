import { Component, EventEmitter, Input, Output, inject, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProductService, Product } from './product.service';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './product-form.component.html',
  styleUrls: ['./product-form.component.scss']
})
export class ProductFormComponent implements OnChanges {
  @Input() product!: Product;
  @Output() saved = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private service = inject(ProductService);

  message = '';

  form = this.fb.group({
    id: 0,
    name: ['', [Validators.required, Validators.maxLength(120)]],
    sku: ['', [Validators.required, Validators.maxLength(80)]],
    category: ['', [Validators.maxLength(120)]],
    minStock: [0, [Validators.required, Validators.min(0)]],
    unit: ['', [Validators.required, Validators.maxLength(40)]]
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['product'] && this.product) {
      this.message = '';
      this.form.patchValue(this.product);
    }
  }

  submit() {
    this.message = '';
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }

    const value = this.form.value;
    const productPayload = {
      ...value,
      minStock: Number(value.minStock ?? 0)
    } as Product;

    if (value.id && value.id !== 0) {
      this.service.update(productPayload).subscribe({
        next: () => this.saved.emit(),
        error: (err) => {
          this.message = this.readError(err, 'Impossible de modifier le produit');
        }
      });
    } else {
      const { id, ...data } = productPayload;
      this.service.create(data).subscribe({
        next: () => this.saved.emit(),
        error: (err) => {
          this.message = this.readError(err, 'Impossible de creer le produit');
        }
      });
    }
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
