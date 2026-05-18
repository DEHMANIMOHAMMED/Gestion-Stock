import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environment';
import { Observable } from 'rxjs';

export interface Product {
  id: number;
  organisationId: number;
  name: string;
  sku: string;
  category: string;
  minStock: number;
  unit: string;
}

export interface ProductImportResult {
  createdProducts: number;
  updatedProducts: number;
  stockMovementsCreated: number;
  errors: string[];
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  constructor(private http: HttpClient) {}

  /**
   * Fetches all products for the current organisation.
   */
  getAll(): Observable<Product[]> {
    return this.http.get<Product[]>(`${environment.apiUrl}/products`);
  }

  /**
   * Creates a new product. The backend automatically sets the organisation ID.
   */
  create(product: Omit<Product, 'id' | 'organisationId'>): Observable<Product> {
    return this.http.post<Product>(`${environment.apiUrl}/products`, product);
  }

  /**
   * Updates an existing product.
   */
  update(product: Product): Observable<Product> {
    return this.http.patch<Product>(`${environment.apiUrl}/products`, product);
  }

  /**
   * Deletes a product by its ID.
   */
  delete(id: number) {
    return this.http.delete<void>(`${environment.apiUrl}/products/${id}`);
  }

  importProducts(file: File): Observable<ProductImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ProductImportResult>(`${environment.apiUrl}/products/import`, formData);
  }
}
