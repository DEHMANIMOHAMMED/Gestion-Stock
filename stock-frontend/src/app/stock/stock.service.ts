import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environment';
import { Observable } from 'rxjs';

export interface StockResponse {
  productId: number;
  quantity: number;
}

export interface StockMovementHistory {
  id: number;
  productId: number;
  quantity: number;
  type: string;
  createdAt: string;
}

export interface StockHistoryPage {
  items: StockMovementHistory[];
  total: number;
  page: number;
  size: number;
}

export interface StockMovementRequest {
  productId: number;
  quantity: number;
  type: string;
}

@Injectable({ providedIn: 'root' })
export class StockService {
  constructor(private http: HttpClient) {}

  /**
   * Returns the current stock quantity for a given product.
   */
  getStock(productId: number): Observable<StockResponse> {
    return this.http.get<StockResponse>(`${environment.apiUrl}/stock/${productId}`);
  }

  /**
   * Retrieves a paginated history of stock movements. Optional filters include product ID and movement type.
   */
  getHistory(params: { productId?: number; type?: string; page?: number; size?: number }): Observable<StockHistoryPage> {
    const query = new URLSearchParams();
    if (params.productId) query.set('productId', params.productId.toString());
    if (params.type) query.set('type', params.type);
    query.set('page', (params.page ?? 0).toString());
    query.set('size', (params.size ?? 10).toString());
    return this.http.get<StockHistoryPage>(`${environment.apiUrl}/stock/history?${query.toString()}`);
  }

  /**
   * Registers a stock movement (IN, OUT, or ADJUST).
   */
  registerMovement(req: StockMovementRequest) {
    return this.http.post<void>(`${environment.apiUrl}/stock/movement`, req);
  }

  /**
   * Downloads the stock history as a CSV file. Filters are optional.
   */
  exportCsv(params: { productId?: number; type?: string }): Observable<Blob> {
    const query = new URLSearchParams();
    if (params.productId) query.set('productId', params.productId.toString());
    if (params.type) query.set('type', params.type);
    return this.http.get(`${environment.apiUrl}/stock/history/export/csv?${query.toString()}`, {
      responseType: 'blob'
    });
  }

  /**
   * Downloads the stock history as a PDF file. Filters are optional.
   */
  exportPdf(params: { productId?: number; type?: string }): Observable<Blob> {
    const query = new URLSearchParams();
    if (params.productId) query.set('productId', params.productId.toString());
    if (params.type) query.set('type', params.type);
    return this.http.get(`${environment.apiUrl}/stock/history/export/pdf?${query.toString()}`, {
      responseType: 'blob'
    });
  }
}