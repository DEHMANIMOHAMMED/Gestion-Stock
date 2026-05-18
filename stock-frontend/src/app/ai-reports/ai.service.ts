import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environment';

export interface AiForecast {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  horizonDays: number;
  predictedQuantity: number;
  confidenceScore: number;
  modelName: string;
  generatedAt: string;
}

export interface AiStockoutRisk {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  estimatedStockoutDate: string | null;
  riskScore: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  reason: string;
  generatedAt: string;
}

export interface AiReorderRecommendation {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  recommendedQuantity: number;
  leadTimeDays: number;
  safetyStock: number;
  reason: string;
  status: string;
  purchaseOrderId: number | null;
  preferredSupplierId: number | null;
  preferredSupplierName: string | null;
  preferredSupplierLeadTimeDays: number | null;
  preferredSupplierUnitCost: number | null;
  preferredSupplierMinimumOrderQuantity: number | null;
  preferredSupplierScore: number | null;
  preferredSupplierOnTimeRate: number | null;
  preferredSupplierConformityRate: number | null;
  preferredSupplierScoreExplanation: string | null;
  generatedAt: string;
}

export interface AiAnomaly {
  id: number;
  productId: number | null;
  productName: string | null;
  stockMovementId: number | null;
  anomalyType: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  score: number;
  explanation: string;
  detectedAt: string;
}

export interface AiInsight {
  id: number;
  title: string;
  content: string;
  insightType: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  generatedAt: string;
}

export interface AiDashboard {
  forecasts: AiForecast[];
  stockoutRisks: AiStockoutRisk[];
  reorderRecommendations: AiReorderRecommendation[];
  anomalies: AiAnomaly[];
  insights: AiInsight[];
}

export interface AiProductHealth {
  productId: number;
  productName: string;
  sku: string;
  category: string | null;
  currentStock: number;
  minStock: number;
  averageDailyDemand: number;
  forecast30Days: number;
  stockCoverageDays: number;
  riskScore: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  recommendedQuantity: number;
  recommendationId: number | null;
  purchaseOrderId: number | null;
  supplierId: number | null;
  supplierName: string | null;
  supplierScore: number | null;
  openPurchaseOrders: number;
  healthScore: number;
  actionRecommendation: string;
}

export interface SupplierIssue {
  supplierId: number;
  supplierName: string;
  leadTimeDays: number;
  openOrders: number;
  issueScore: number;
  reason: string;
}

export interface AiExecutiveDashboard {
  totalProducts: number;
  criticalStockCount: number;
  highRiskCount: number;
  pendingRecommendationsCount: number;
  pendingPurchaseOrdersCount: number;
  supplierIssuesCount: number;
  stockHealthAverage: number;
  topRisks: AiProductHealth[];
  recommendations: AiReorderRecommendation[];
  pendingOrders: Array<{ id: number; supplierName: string; status: string; createdAt: string; lines: unknown[] }>;
  supplierIssues: SupplierIssue[];
  insights: AiInsight[];
}

export interface AiWhatIfResponse {
  productId: number;
  productName: string;
  currentStock: number;
  orderQuantity: number;
  leadTimeDays: number;
  averageDailyDemand: number;
  currentCoverageDays: number;
  projectedCoverageDays: number;
  projectedStockAfterLeadTime: number;
  recommendation: string;
}

export interface AiCitation {
  type: string;
  label: string;
  productId: number | null;
  supplierId: number | null;
  purchaseOrderId: number | null;
}

export interface AiCopilotResponse {
  conversationId: number | null;
  answer: string;
  bullets: string[];
  relatedProductIds: number[];
  citations: AiCitation[];
  source: string;
  actions: AiCopilotAction[];
}

export interface AiCopilotAction {
  type: 'CREATE_PURCHASE_ORDER' | 'EXPLAIN_RECOMMENDATION' | 'RUN_WHAT_IF' | 'OPEN_SUPPLIER_360' | 'OPEN_AI_DASHBOARD';
  label: string;
  description: string;
  productId: number | null;
  recommendationId: number | null;
  purchaseOrderId: number | null;
  supplierId: number | null;
  quantity: number | null;
  leadTimeDays: number | null;
  route: string | null;
  requiresAdminConfirmation: boolean;
}

export interface AiRecommendationExplanation {
  recommendationId: number;
  summary: string;
  drivers: string[];
  risks: string[];
  nextAction: string;
  citations: AiCitation[];
  source: string;
}

export interface AiCopilotMessage {
  id: number;
  question: string;
  answer: string;
  source: string;
  citations: AiCitation[];
  createdAt: string;
}

export interface AiCopilotConversation {
  id: number;
  title: string;
  updatedAt: string;
  messages: AiCopilotMessage[];
}

export interface AiAuditLog {
  id: number;
  actorEmail: string;
  action: string;
  targetType: string;
  targetId: number | null;
  source: string;
  summary: string;
  createdAt: string;
}

export interface AiAuditLogFilters {
  action?: string;
  actorEmail?: string;
  targetType?: string;
  source?: string;
  module?: string;
  severity?: string;
  from?: string;
  to?: string;
}

export interface AiRun {
  id: number;
  status: 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';
  triggerType: 'MANUAL' | 'SCHEDULED' | 'INITIAL';
  startedAt: string;
  completedAt: string | null;
  errorMessage: string | null;
  forecastsCount: number;
  risksCount: number;
  recommendationsCount: number;
  anomaliesCount: number;
  insightsCount: number;
  modelSource: 'FASTAPI' | 'LOCAL_FALLBACK';
}

export interface SystemHealthService {
  name: string;
  status: 'UP' | 'DOWN';
  latencyMs: number;
  detail: string;
}

export interface SystemHealth {
  overallStatus: 'UP' | 'DEGRADED';
  checkedAt: string;
  services: SystemHealthService[];
  latestAiRun: AiRun | null;
  unreadAdminNotifications: number;
  productsCount: number;
  stocksCount: number;
  purchaseOrdersCount: number;
}

export interface ExecutiveTimelineItem {
  type: string;
  severity: 'INFO' | 'WARNING' | 'CRITICAL' | 'SUCCESS';
  priorityScore: number;
  title: string;
  description: string;
  occurredAt: string;
  notificationId: number | null;
  productId: number | null;
  purchaseOrderId: number | null;
  recommendationId: number | null;
  supplierId: number | null;
}

export interface ExecutiveTimelineAction {
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  title: string;
  description: string;
  route: string;
}

export interface ExecutiveTimeline {
  date: string;
  criticalRisks: number;
  openNotifications: number;
  pendingOrders: number;
  pendingRecommendations: number;
  executiveSummary: string;
  keyDecisions: string[];
  items: ExecutiveTimelineItem[];
  actions: ExecutiveTimelineAction[];
}

@Injectable({ providedIn: 'root' })
export class AiService {
  constructor(private http: HttpClient) {}

  getDashboard(): Observable<AiDashboard> {
    return this.http.get<AiDashboard>(`${environment.apiUrl}/ai/dashboard`);
  }

  requestRun(): Observable<AiRun> {
    return this.http.post<AiRun>(`${environment.apiUrl}/ai/runs`, {});
  }

  getRuns(): Observable<AiRun[]> {
    return this.http.get<AiRun[]>(`${environment.apiUrl}/ai/runs`);
  }

  getLatestRun(): Observable<AiRun | null> {
    return this.http.get<AiRun | null>(`${environment.apiUrl}/ai/runs/latest`);
  }

  getForecasts(horizon?: number): Observable<AiForecast[]> {
    const query = horizon ? `?horizon=${horizon}` : '';
    return this.http.get<AiForecast[]>(`${environment.apiUrl}/ai/forecasts${query}`);
  }

  getStockoutRisks(): Observable<AiStockoutRisk[]> {
    return this.http.get<AiStockoutRisk[]>(`${environment.apiUrl}/ai/stockout-risks`);
  }

  getReorderRecommendations(): Observable<AiReorderRecommendation[]> {
    return this.http.get<AiReorderRecommendation[]>(`${environment.apiUrl}/ai/reorder-recommendations`);
  }

  getAnomalies(): Observable<AiAnomaly[]> {
    return this.http.get<AiAnomaly[]>(`${environment.apiUrl}/ai/anomalies`);
  }

  getInsights(): Observable<AiInsight[]> {
    return this.http.get<AiInsight[]>(`${environment.apiUrl}/ai/insights`);
  }

  getExperienceDashboard(): Observable<AiExecutiveDashboard> {
    return this.http.get<AiExecutiveDashboard>(`${environment.apiUrl}/ai/experience-dashboard`);
  }

  getStockHealth(): Observable<AiProductHealth[]> {
    return this.http.get<AiProductHealth[]>(`${environment.apiUrl}/ai/stock-health`);
  }

  whatIf(productId: number, orderQuantity: number, leadTimeDays: number): Observable<AiWhatIfResponse> {
    return this.http.post<AiWhatIfResponse>(`${environment.apiUrl}/ai/what-if`, {
      productId,
      orderQuantity,
      leadTimeDays
    });
  }

  askCopilot(question: string, conversationId?: number | null): Observable<AiCopilotResponse> {
    return this.http.post<AiCopilotResponse>(`${environment.apiUrl}/ai/copilot`, {
      question,
      conversationId: conversationId ?? null
    });
  }

  getCopilotHistory(): Observable<AiCopilotConversation[]> {
    return this.http.get<AiCopilotConversation[]>(`${environment.apiUrl}/ai/copilot/history`);
  }

  getAiAuditLogs(filters: AiAuditLogFilters = {}): Observable<AiAuditLog[]> {
    return this.http.get<AiAuditLog[]>(`${environment.apiUrl}/ai/audit-logs`, { params: this.auditParams(filters) });
  }

  exportAiAuditLogsCsv(filters: AiAuditLogFilters = {}): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/ai/audit-logs/export/csv`, {
      params: this.auditParams(filters),
      responseType: 'blob'
    });
  }

  exportAiAuditLogsPdf(filters: AiAuditLogFilters = {}): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/ai/audit-logs/export/pdf`, {
      params: this.auditParams(filters),
      responseType: 'blob'
    });
  }

  private auditParams(filters: AiAuditLogFilters): HttpParams {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value?.trim()) {
        params = params.set(key, value.trim());
      }
    });
    return params;
  }

  explainRecommendation(id: number): Observable<AiRecommendationExplanation> {
    return this.http.get<AiRecommendationExplanation>(`${environment.apiUrl}/ai/reorder-recommendations/${id}/explanation`);
  }

  getSystemHealth(): Observable<SystemHealth> {
    return this.http.get<SystemHealth>(`${environment.apiUrl}/admin/system-health`);
  }

  getExecutiveTimeline(date?: string): Observable<ExecutiveTimeline> {
    const params = date ? new HttpParams().set('date', date) : undefined;
    return this.http.get<ExecutiveTimeline>(`${environment.apiUrl}/admin/executive-timeline`, { params });
  }

  exportDailyReportCsv(date?: string): Observable<Blob> {
    const params = date ? new HttpParams().set('date', date) : undefined;
    return this.http.get(`${environment.apiUrl}/admin/executive-timeline/export/csv`, {
      params,
      responseType: 'blob'
    });
  }

  exportDailyReportPdf(date?: string): Observable<Blob> {
    const params = date ? new HttpParams().set('date', date) : undefined;
    return this.http.get(`${environment.apiUrl}/admin/executive-timeline/export/pdf`, {
      params,
      responseType: 'blob'
    });
  }
}
