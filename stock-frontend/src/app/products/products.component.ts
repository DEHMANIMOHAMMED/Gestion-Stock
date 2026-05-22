import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService, Product } from './product.service';
import { signal } from '@angular/core';
import { ProductFormComponent } from './product-form.component';
import { AuthService } from '../auth/auth.service';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { EmptyStateComponent } from '../shared/ui/empty-state.component';
import { LoadingStateComponent } from '../shared/ui/loading-state.component';
import { ConfirmDialogComponent } from '../shared/ui/confirm-dialog.component';

// Listing page for products with CRUD operations.
@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule, ProductFormComponent, PageHeaderComponent, EmptyStateComponent, LoadingStateComponent, ConfirmDialogComponent],
  templateUrl: './products.component.html',
  styleUrls: ['./products.component.scss']
})
export class ProductsComponent implements OnInit {
  private productService = inject(ProductService);
  auth = inject(AuthService);
  products = signal<Product[]>([]);
  loading = signal(false);
  importMessage = signal<string | null>(null);
  importErrors = signal<string[]>([]);
  importPreview = signal<{ columns: string[]; sampleRows: string[][]; mapping: Array<{ target: string; source: string | null }> } | null>(null);
  selected: Product | null = null;
  productToDelete = signal<Product | null>(null);

  ngOnInit() {
    this.load();
  }

  /**
   * Retrieves the products from the backend.
   */
  load() {
    this.loading.set(true);
    this.productService.getAll().subscribe((data) => {
      this.products.set(data);
      this.loading.set(false);
    }, () => {
      this.loading.set(false);
    });
  }

  /**
   * Opens the product form for creating a new product.
   */
  add() {
    if (!this.auth.hasRole('ADMIN')) {
      return;
    }
    this.selected = {
      id: 0,
      organisationId: 0,
      name: '',
      sku: '',
      category: '',
      minStock: 0,
      unit: ''
    };
  }

  /**
   * Opens the product form for editing an existing product.
   */
  edit(product: Product) {
    if (!this.auth.hasRole('ADMIN')) {
      return;
    }
    // Clone the product so edits don't immediately affect the list
    this.selected = { ...product };
  }

  /**
   * Callback triggered when the form has been saved.
   */
  saved() {
    this.selected = null;
    this.load();
  }

  /**
   * Deletes a product after confirmation.
   */
  delete(product: Product) {
    if (!this.auth.hasRole('ADMIN')) {
      return;
    }
    this.productToDelete.set(product);
  }

  confirmDelete(): void {
    const product = this.productToDelete();
    if (!product) {
      return;
    }
    this.productService.delete(product.id).subscribe(() => {
      this.productToDelete.set(null);
      this.load();
    });
  }

  /**
   * Cancels the current form editing.
   */
  cancel() {
    this.selected = null;
  }

  importProducts(event: Event) {
    if (!this.auth.hasRole('ADMIN')) {
      return;
    }
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.importMessage.set(null);
    this.importErrors.set([]);
    this.previewImport(file);
    this.productService.importProducts(file).subscribe({
      next: (result) => {
        this.importMessage.set(
          `${result.createdProducts} cree(s), ${result.updatedProducts} mis a jour, ${result.stockMovementsCreated} stock initialise(s).`
        );
        this.importErrors.set(result.errors);
        this.load();
        input.value = '';
      },
      error: (error) => {
        this.importMessage.set(error?.error?.detail || 'Import impossible.');
        input.value = '';
      }
    });
  }

  downloadImportErrors(): void {
    const content = this.importErrors().join('\n');
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'product-import-errors.txt';
    anchor.click();
    window.URL.revokeObjectURL(url);
  }

  private previewImport(file: File): void {
    if (!file.name.toLowerCase().endsWith('.csv')) {
      this.importPreview.set({
        columns: ['sku', 'name', 'category', 'minStock', 'unit', 'initialStock'],
        sampleRows: [],
        mapping: this.expectedMapping(['sku', 'name', 'category', 'minStock', 'unit', 'initialStock'])
      });
      return;
    }

    file.text().then((content) => {
      const rows = content.split(/\r?\n/).filter(Boolean).slice(0, 4).map((line) => line.split(',').map((cell) => cell.trim()));
      const columns = rows[0] ?? [];
      this.importPreview.set({
        columns,
        sampleRows: rows.slice(1),
        mapping: this.expectedMapping(columns)
      });
    });
  }

  private expectedMapping(columns: string[]): Array<{ target: string; source: string | null }> {
    const targets = ['sku', 'name', 'category', 'minStock', 'unit', 'initialStock'];
    return targets.map((target) => ({
      target,
      source: columns.find((column) => column.toLowerCase() === target.toLowerCase()) ?? null
    }));
  }
}
