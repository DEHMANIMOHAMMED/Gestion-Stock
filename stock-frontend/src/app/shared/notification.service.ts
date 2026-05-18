import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environment';

export interface AdminNotification {
  id: number;
  type: string;
  severity: 'CRITICAL' | 'WARNING' | 'INFO';
  title: string;
  message: string;
  purchaseOrderId: number | null;
  supplierId: number | null;
  readAt: string | null;
  status: 'OPEN' | 'ACTIONED' | 'DISMISSED';
  actionTaken: string | null;
  actionedAt: string | null;
  actionedByUserId: number | null;
  dismissalReason: string | null;
  createdAt: string;
}

export interface AdminNotificationPreferences {
  thresholdNotificationsEnabled: boolean;
  criticalStockoutNotificationsEnabled: boolean;
  updatedAt: string | null;
  updatedByUserId: number | null;
  defaultValue: boolean;
}

export interface AdminNotificationAction {
  id: number;
  action: string;
  reason: string | null;
  actorUserId: number;
  createdAt: string;
}

interface NotificationStreamPayload {
  unreadCount: number;
  notifications: AdminNotification[];
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private eventSource: EventSource | null = null;
  private notificationsSignal = signal<AdminNotification[]>([]);
  private unreadCountSignal = signal(0);

  readonly notifications = this.notificationsSignal.asReadonly();
  readonly unreadCount = this.unreadCountSignal.asReadonly();
  readonly hasCritical = computed(() => this.notifications().some((notification) => notification.severity === 'CRITICAL'));

  constructor(private http: HttpClient) {}

  load(): void {
    this.http.get<AdminNotification[]>(`${environment.apiUrl}/notifications`).subscribe({
      next: (notifications) => this.notificationsSignal.set(notifications),
      error: () => this.notificationsSignal.set([])
    });
    this.http.get<{ unreadCount: number }>(`${environment.apiUrl}/notifications/unread-count`).subscribe({
      next: (response) => this.unreadCountSignal.set(response.unreadCount),
      error: () => this.unreadCountSignal.set(0)
    });
  }

  connect(token: string | null): void {
    this.disconnect();
    if (!token) {
      return;
    }
    this.eventSource = new EventSource(`${environment.apiUrl}/notifications/stream?access_token=${encodeURIComponent(token)}`);
    this.eventSource.addEventListener('notifications', (event) => {
      const payload = JSON.parse((event as MessageEvent).data) as NotificationStreamPayload;
      this.unreadCountSignal.set(payload.unreadCount);
      this.notificationsSignal.set(payload.notifications);
    });
    this.eventSource.onerror = () => {
      this.disconnect();
    };
  }

  markRead(id: number): void {
    this.markReadRequest(id).subscribe({
      next: () => {
        this.notificationsSignal.update((notifications) => notifications.map((notification) => (
          notification.id === id ? { ...notification, readAt: new Date().toISOString() } : notification
        )));
        this.unreadCountSignal.update((count) => Math.max(0, count - 1));
      }
    });
  }

  markReadRequest(id: number) {
    return this.http.post<void>(`${environment.apiUrl}/notifications/${id}/read`, {});
  }

  history(filters: { type?: string; severity?: string; readStatus?: string }) {
    const params = new URLSearchParams();
    if (filters.type && filters.type !== 'ALL') {
      params.set('type', filters.type);
    }
    if (filters.severity && filters.severity !== 'ALL') {
      params.set('severity', filters.severity);
    }
    if (filters.readStatus && filters.readStatus !== 'ALL') {
      params.set('readStatus', filters.readStatus);
    }
    const query = params.toString();
    return this.http.get<AdminNotification[]>(`${environment.apiUrl}/notifications/history${query ? `?${query}` : ''}`);
  }

  getPreferences() {
    return this.http.get<AdminNotificationPreferences>(`${environment.apiUrl}/notifications/preferences`);
  }

  updatePreferences(preferences: Pick<AdminNotificationPreferences, 'thresholdNotificationsEnabled' | 'criticalStockoutNotificationsEnabled'>) {
    return this.http.put<AdminNotificationPreferences>(`${environment.apiUrl}/notifications/preferences`, preferences);
  }

  action(id: number, action: string, reason?: string | null) {
    return this.http.post<AdminNotification>(`${environment.apiUrl}/notifications/${id}/action`, {
      action,
      reason: reason || null
    });
  }

  actions(id: number) {
    return this.http.get<AdminNotificationAction[]>(`${environment.apiUrl}/notifications/${id}/actions`);
  }

  disconnect(): void {
    this.eventSource?.close();
    this.eventSource = null;
  }
}
