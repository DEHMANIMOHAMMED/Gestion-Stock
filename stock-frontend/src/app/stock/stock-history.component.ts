import { Component, OnInit, inject, signal } from "@angular/core";
import { CommonModule, DatePipe } from "@angular/common";
import { StockService, StockMovementHistory } from "./stock.service";
import { ProductService, Product } from "../products/product.service";
import { FormsModule } from "@angular/forms";
import { AuthService } from "../auth/auth.service";

@Component({
  selector: "app-stock-history",
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: "./stock-history.component.html",
  styleUrls: ["./stock-history.component.scss"],
})
export class StockHistoryComponent implements OnInit {
  private stockService = inject(StockService);
  private productService = inject(ProductService);
  auth = inject(AuthService);

  history = signal<StockMovementHistory[]>([]);
  total = 0;
  page = 0;
  size = 10;

  products = signal<Product[]>([]);
  filters: {
    productId?: number;
    type?: "IN" | "OUT" | "ADJUST";
  } = {};

  ngOnInit() {
    // Load all products to display names in the dropdown
    this.productService.getAll().subscribe((data) => {
      this.products.set(data);
    });
    this.load();
  }

  load() {
    const params: { productId?: number; type?: string; page: number; size: number } = { page: this.page, size: this.size };
    if (this.filters.productId) {
      params.productId = this.filters.productId;
    }
    if (this.filters.type) {
      params.type = this.filters.type;
    }
    this.stockService.getHistory(params).subscribe((res) => {
      this.history.set(res.items);
      this.total = res.total;
    });
  }

  changePage(offset: number) {
    const newPage = this.page + offset;
    if (newPage >= 0 && newPage < Math.ceil(this.total / this.size)) {
      this.page = newPage;
      this.load();
    }
  }

  exportCsv() {
    if (!this.auth.hasRole("ADMIN")) {
      return;
    }
    const params: { productId?: number; type?: string } = {};
    if (this.filters.productId) params.productId = this.filters.productId;
    if (this.filters.type) params.type = this.filters.type;
    this.stockService.exportCsv(params).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "stock_history.csv";
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  exportPdf() {
    if (!this.auth.hasRole("ADMIN")) {
      return;
    }
    const params: { productId?: number; type?: string } = {};
    if (this.filters.productId) params.productId = this.filters.productId;
    if (this.filters.type) params.type = this.filters.type;
    this.stockService.exportPdf(params).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "stock_history.pdf";
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  onProductChange(productId: number | null): void {
    this.filters.productId = productId ?? undefined;
    this.resetAndLoad();
  }

  onTypeChange(type: "IN" | "OUT" | "ADJUST" | null): void {
    this.filters.type = type ?? undefined;
    this.resetAndLoad();
  }

  previousPage(): void {
    this.changePage(-1);
  }

  nextPage(): void {
    this.changePage(1);
  }

  get currentPage(): number {
    return this.page + 1;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.total / this.size));
  }

  productNameById(productId: number): string {
    return this.products().find((product) => product.id === productId)?.name ?? `Produit #${productId}`;
  }

  private resetAndLoad(): void {
    this.page = 0;
    this.load();
  }
}
