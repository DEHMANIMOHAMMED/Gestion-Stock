import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environment';

export interface Supplier {
  id: number;
  organisationId: number;
  name: string;
  email: string | null;
  phone: string | null;
  leadTimeDays: number;
  active: boolean;
  createdAt: string;
}

export interface ProductSupplier {
  id: number;
  organisationId: number;
  productId: number;
  productName: string;
  sku: string;
  supplierId: number;
  supplierName: string;
  supplierLeadTimeDays: number | null;
  unitCost: number | null;
  minimumOrderQuantity: number;
  preferred: boolean;
  supplierScore: number;
  onTimeRate: number;
  conformityRate: number;
  scoreExplanation: string;
  active: boolean;
  createdAt: string;
}

export interface Supplier360Product {
  productId: number;
  productName: string;
  sku: string;
  unitCost: number;
  minimumOrderQuantity: number;
  preferred: boolean;
  supplierScore: number;
  scoreExplanation: string;
}

export interface Supplier360Order {
  id: number;
  status: PurchaseOrderStatus;
  expectedDeliveryDate: string | null;
  createdAt: string;
  receivedAt: string | null;
  orderedQuantity: number;
  receivedQuantity: number;
  totalCost: number;
}

export interface Supplier360 {
  id: number;
  name: string;
  email: string | null;
  phone: string | null;
  leadTimeDays: number;
  active: boolean;
  totalOrders: number;
  draftOrders: number;
  orderedOrders: number;
  receivedOrders: number;
  lateOrders: number;
  coveredProducts: number;
  orderedQuantity: number;
  receivedQuantity: number;
  totalSpend: number;
  averageUnitCost: number;
  onTimeRate: number;
  conformityRate: number;
  averageDelayDays: number;
  healthScore: number;
  products: Supplier360Product[];
  recentOrders: Supplier360Order[];
  recommendations: string[];
}

export interface PurchaseOrderLine {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  quantity: number;
  receivedQuantity: number;
  unitCost: number | null;
}

export interface PurchaseOrder {
  id: number;
  organisationId: number;
  supplierId: number;
  supplierName: string;
  status: PurchaseOrderStatus;
  expectedDeliveryDate: string | null;
  createdAt: string;
  receivedAt: string | null;
  lines: PurchaseOrderLine[];
}

export type PurchaseOrderStatus = 'DRAFT' | 'APPROVED' | 'ORDERED' | 'RECEIVED' | 'CANCELLED';

export interface PurchaseOrderAuditLog {
  id: number;
  purchaseOrderId: number;
  action: string;
  previousStatus: PurchaseOrderStatus | null;
  newStatus: PurchaseOrderStatus;
  actorUserId: number | null;
  actorEmail: string;
  actorRole: 'ADMIN' | 'USER';
  orderTotal: number;
  createdAt: string;
}

export interface PurchaseOrderApprovalItem {
  id: number;
  supplierId: number;
  supplierName: string;
  status: PurchaseOrderStatus;
  orderTotal: number;
  linesCount: number;
  totalQuantity: number;
  maxRiskScore: number;
  maxRiskLevel: string;
  earliestStockoutDate: string | null;
  urgency: 'CRITICAL' | 'HIGH' | 'NORMAL';
  riskReason: string;
  expectedDeliveryDate: string | null;
  createdAt: string;
}

export interface ProcurementApprovalSettings {
  approvalThreshold: number;
  updatedAt: string | null;
  updatedByUserId: number | null;
  defaultValue: boolean;
}

export interface SupplierComparison {
  productId: number;
  productName: string;
  sku: string;
  supplierId: number;
  supplierName: string;
  unitCost: number | null;
  minimumOrderQuantity: number;
  leadTimeDays: number | null;
  supplierScore: number;
  onTimeRate: number;
  conformityRate: number;
  preferred: boolean;
  bestAlternative: boolean;
  recommendation: string;
}

export interface SupplierSla {
  supplierId: number;
  supplierName: string;
  targetLeadTimeDays: number;
  targetOnTimeRate: number;
  targetConformityRate: number;
  notes: string | null;
  updatedAt: string;
}

export interface ProcurementImportResult {
  created: number;
  skipped: number;
  errors: string[];
}

export interface CreateSupplierRequest {
  name: string;
  email?: string | null;
  phone?: string | null;
  leadTimeDays: number;
}

export interface CreatePurchaseOrderRequest {
  supplierId: number;
  expectedDeliveryDate?: string | null;
  lines: Array<{
    productId: number;
    quantity: number;
    unitCost?: number | null;
  }>;
}

export interface ProductSupplierRequest {
  productId: number;
  supplierId: number;
  unitCost?: number | null;
  minimumOrderQuantity: number;
  preferred: boolean;
}

@Injectable({ providedIn: 'root' })
export class ProcurementService {
  constructor(private http: HttpClient) {}

  getSuppliers(): Observable<Supplier[]> {
    return this.http.get<Supplier[]>(`${environment.apiUrl}/suppliers`);
  }

  getSupplier360(id: number): Observable<Supplier360> {
    return this.http.get<Supplier360>(`${environment.apiUrl}/suppliers/${id}/360`);
  }

  createSupplier(request: CreateSupplierRequest): Observable<Supplier> {
    return this.http.post<Supplier>(`${environment.apiUrl}/suppliers`, request);
  }

  getPurchaseOrders(): Observable<PurchaseOrder[]> {
    return this.http.get<PurchaseOrder[]>(`${environment.apiUrl}/purchase-orders`);
  }

  createPurchaseOrder(request: CreatePurchaseOrderRequest): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${environment.apiUrl}/purchase-orders`, request);
  }

  createPurchaseOrderFromRecommendation(recommendationId: number, supplierId?: number | null): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${environment.apiUrl}/purchase-orders/from-recommendation`, {
      recommendationId,
      supplierId: supplierId ?? null
    });
  }

  getProductSuppliers(): Observable<ProductSupplier[]> {
    return this.http.get<ProductSupplier[]>(`${environment.apiUrl}/product-suppliers`);
  }

  upsertProductSupplier(request: ProductSupplierRequest): Observable<ProductSupplier> {
    return this.http.post<ProductSupplier>(`${environment.apiUrl}/product-suppliers`, request);
  }

  updatePurchaseOrder(id: number, request: CreatePurchaseOrderRequest): Observable<PurchaseOrder> {
    return this.http.put<PurchaseOrder>(`${environment.apiUrl}/purchase-orders/${id}`, request);
  }

  confirmPurchaseOrder(id: number): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${environment.apiUrl}/purchase-orders/${id}/confirm`, {});
  }

  approvePurchaseOrder(id: number): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${environment.apiUrl}/purchase-orders/${id}/approve`, {});
  }

  cancelPurchaseOrder(id: number): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${environment.apiUrl}/purchase-orders/${id}/cancel`, {});
  }

  receivePurchaseOrder(id: number, lines?: Array<{ lineId: number; quantity: number }>): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${environment.apiUrl}/purchase-orders/${id}/receive`, lines ? { lines } : {});
  }

  downloadPurchaseOrderPdf(id: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/purchase-orders/${id}/pdf`, { responseType: 'blob' });
  }

  exportAccounting(): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/purchase-orders/accounting-export`, { responseType: 'blob' });
  }

  getPurchaseOrderAudit(id: number): Observable<PurchaseOrderAuditLog[]> {
    return this.http.get<PurchaseOrderAuditLog[]>(`${environment.apiUrl}/purchase-orders/${id}/audit`);
  }

  getApprovalCenter(): Observable<PurchaseOrderApprovalItem[]> {
    return this.http.get<PurchaseOrderApprovalItem[]>(`${environment.apiUrl}/purchase-orders/approval-center`);
  }

  getApprovalSettings(): Observable<ProcurementApprovalSettings> {
    return this.http.get<ProcurementApprovalSettings>(`${environment.apiUrl}/purchase-orders/approval-settings`);
  }

  updateApprovalSettings(approvalThreshold: number): Observable<ProcurementApprovalSettings> {
    return this.http.put<ProcurementApprovalSettings>(`${environment.apiUrl}/purchase-orders/approval-settings`, {
      approvalThreshold
    });
  }

  getSupplierComparison(productId?: number | null): Observable<SupplierComparison[]> {
    const query = productId ? `?productId=${productId}` : '';
    return this.http.get<SupplierComparison[]>(`${environment.apiUrl}/suppliers/comparison${query}`);
  }

  getSupplierSlas(): Observable<SupplierSla[]> {
    return this.http.get<SupplierSla[]>(`${environment.apiUrl}/suppliers/sla`);
  }

  upsertSupplierSla(request: {
    supplierId: number;
    targetLeadTimeDays: number;
    targetOnTimeRate: number;
    targetConformityRate: number;
    notes?: string | null;
  }): Observable<SupplierSla> {
    return this.http.post<SupplierSla>(`${environment.apiUrl}/suppliers/sla`, request);
  }

  importSuppliers(file: File): Observable<ProcurementImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ProcurementImportResult>(`${environment.apiUrl}/suppliers/import`, formData);
  }

  importPurchaseOrders(file: File): Observable<ProcurementImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ProcurementImportResult>(`${environment.apiUrl}/purchase-orders/import`, formData);
  }
}
