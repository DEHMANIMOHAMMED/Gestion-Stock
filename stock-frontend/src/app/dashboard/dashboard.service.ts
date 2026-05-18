import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environment';

export interface RecentStockMovement {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  type: 'IN' | 'OUT' | 'ADJUST';
  createdAt: string;
}

export interface DashboardSummary {
  totalProducts: number;
  totalUnits: number;
  lowStockProducts: number;
  outOfStockProducts: number;
  recentMovements: RecentStockMovement[];
}

export interface LowStockAlert {
  productId: number;
  sku: string;
  name: string;
  quantity: number;
  minStock: number;
  missingQuantity: number;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private http: HttpClient) {}

  getSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${environment.apiUrl}/dashboard/summary`);
  }

  getLowStockAlerts(): Observable<LowStockAlert[]> {
    return this.http.get<LowStockAlert[]>(`${environment.apiUrl}/dashboard/low-stock`);
  }
}
