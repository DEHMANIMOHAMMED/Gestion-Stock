import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environment';

export interface SaleLineRequest {
  productId: number;
  quantity: number;
  unitPrice: number;
}

export interface SaleRequest {
  customerName: string | null;
  channel: string;
  lines: SaleLineRequest[];
}

export interface SaleLine {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface Sale {
  id: number;
  reference: string;
  customerName: string | null;
  channel: string;
  status: string;
  totalAmount: number;
  soldAt: string;
  lines: SaleLine[];
}

export interface ProductDemand {
  productId: number;
  productName: string;
  sku: string;
  quantitySold: number;
  revenue: number;
}

export interface SalesSummary {
  revenue30Days: number;
  unitsSold30Days: number;
  salesCount30Days: number;
  averageBasket: number;
  topProducts: ProductDemand[];
}

export interface SalesPeriodPoint {
  date: string;
  revenue: number;
  unitsSold: number;
  salesCount: number;
}

export interface SalesChannel {
  channel: string;
  revenue: number;
  unitsSold: number;
  salesCount: number;
  sharePercent: number;
}

export interface ProductDemandTrend {
  productId: number;
  productName: string;
  sku: string;
  currentPeriodUnits: number;
  previousPeriodUnits: number;
  trendPercent: number;
  revenue: number;
  currentStock: number;
  minStock: number;
  signal: string;
}

export interface DemandSeasonality {
  industry: string;
  pattern: string;
  seasonalityIndex: number;
  weekdaySeries: SalesPeriodPoint[];
  insight: string;
}

export interface DemandForecastComparison {
  productId: number;
  productName: string;
  sku: string;
  actualUnits30Days: number;
  forecastUnits30Days: number;
  variancePercent: number;
  confidenceScore: number;
  status: string;
  recommendation: string;
}

export interface DemandForecastAlert {
  productId: number;
  productName: string;
  sku: string;
  severity: string;
  variancePercent: number;
  message: string;
  recommendedAction: string;
}

export interface DemandAnalytics {
  summary: SalesSummary;
  dailySeries: SalesPeriodPoint[];
  channels: SalesChannel[];
  topRisingProducts: ProductDemandTrend[];
  topDecliningProducts: ProductDemandTrend[];
  highDemandLowStockProducts: ProductDemandTrend[];
  seasonality: DemandSeasonality;
  forecastComparisons: DemandForecastComparison[];
  forecastAlerts: DemandForecastAlert[];
}

@Injectable({ providedIn: 'root' })
export class SalesService {
  constructor(private http: HttpClient) {}

  getRecent(): Observable<Sale[]> {
    return this.http.get<Sale[]>(`${environment.apiUrl}/sales`);
  }

  getSummary(): Observable<SalesSummary> {
    return this.http.get<SalesSummary>(`${environment.apiUrl}/sales/summary`);
  }

  getAnalytics(): Observable<DemandAnalytics> {
    return this.http.get<DemandAnalytics>(`${environment.apiUrl}/sales/analytics`);
  }

  create(request: SaleRequest): Observable<Sale> {
    return this.http.post<Sale>(`${environment.apiUrl}/sales`, request);
  }
}
