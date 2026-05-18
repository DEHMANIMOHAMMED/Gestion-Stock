import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StockService } from './stock.service';
import { ProductService, Product } from '../products/product.service';
import { signal } from '@angular/core';
import { MovementFormComponent } from './movement-form.component';
import { StockHistoryComponent } from './stock-history.component';

// Displays stock levels per product and allows registering movements.
@Component({
  selector: 'app-stock',
  standalone: true,
  imports: [CommonModule, MovementFormComponent, StockHistoryComponent],
  templateUrl: './stock.component.html',
  styleUrls: ['./stock.component.scss']
})
export class StockComponent implements OnInit {
  private stockService = inject(StockService);
  private productService = inject(ProductService);

  products = signal<Product[]>([]);
  stockMap = signal<Record<number, number>>({});
  selectedProductId: number | null = null;
  showForm = false;

  ngOnInit() {
    this.loadProducts();
  }

  loadProducts() {
    this.productService.getAll().subscribe((data) => {
      this.products.set(data);
      // For each product, fetch current stock
      data.forEach((product) => {
        this.stockService.getStock(product.id).subscribe((stock) => {
          this.stockMap.update((map) => {
            map[product.id] = stock.quantity;
            return map;
          });
        });
      });
    });
  }

  openMovement(productId: number) {
    this.selectedProductId = productId;
    this.showForm = true;
  }

  formClosed() {
    this.showForm = false;
    this.loadProducts();
  }
}