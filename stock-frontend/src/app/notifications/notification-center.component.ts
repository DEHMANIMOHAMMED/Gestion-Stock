import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  AdminNotificationAction,
  AdminNotification,
  AdminNotificationPreferences,
  NotificationService
} from '../shared/notification.service';

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './notification-center.component.html',
  styleUrls: ['./notification-center.component.scss']
})
export class NotificationCenterComponent implements OnInit {
  private notificationService = inject(NotificationService);

  notifications = signal<AdminNotification[]>([]);
  preferences = signal<AdminNotificationPreferences | null>(null);
  loading = signal(true);
  message = signal<string | null>(null);
  actionHistory = signal<Record<number, AdminNotificationAction[]>>({});

  filters = {
    type: 'ALL',
    severity: 'ALL',
    readStatus: 'ALL',
    status: 'ALL'
  };

  dismissReasons = signal<Record<number, string>>({});

  preferenceForm = {
    thresholdNotificationsEnabled: true,
    criticalStockoutNotificationsEnabled: true
  };

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.message.set(null);
    this.notificationService.history(this.filters).subscribe({
      next: (notifications) => {
        this.notifications.set(this.filterByActionStatus(notifications));
        this.loading.set(false);
      },
      error: (error) => {
        this.message.set(error?.error?.detail || 'Historique notifications indisponible.');
        this.loading.set(false);
      }
    });
    this.notificationService.getPreferences().subscribe({
      next: (preferences) => {
        this.preferences.set(preferences);
        this.preferenceForm = {
          thresholdNotificationsEnabled: preferences.thresholdNotificationsEnabled,
          criticalStockoutNotificationsEnabled: preferences.criticalStockoutNotificationsEnabled
        };
      },
      error: (error) => this.message.set(error?.error?.detail || 'Preferences notifications indisponibles.')
    });
  }

  savePreferences(): void {
    this.notificationService.updatePreferences(this.preferenceForm).subscribe({
      next: (preferences) => {
        this.preferences.set(preferences);
        this.message.set('Preferences notifications mises a jour.');
      },
      error: (error) => this.message.set(error?.error?.detail || 'Modification preferences impossible.')
    });
  }

  markRead(notification: AdminNotification): void {
    if (notification.readAt) {
      return;
    }
    this.notificationService.markRead(notification.id);
    this.notifications.update((notifications) => notifications.map((item) => (
      item.id === notification.id ? { ...item, readAt: new Date().toISOString() } : item
    )));
  }

  setDismissReason(id: number, reason: string): void {
    this.dismissReasons.update((values) => ({ ...values, [id]: reason }));
  }

  action(notification: AdminNotification, action: 'APPROVE_ORDER' | 'CONFIRM_ORDER' | 'DISMISS'): void {
    const reason = this.dismissReasons()[notification.id] || null;
    this.notificationService.action(notification.id, action, reason).subscribe({
      next: () => {
        this.loadActions(notification.id);
        this.load();
      },
      error: (error) => this.message.set(error?.error?.detail || 'Action notification impossible.')
    });
  }

  loadActions(notificationId: number): void {
    this.notificationService.actions(notificationId).subscribe({
      next: (actions) => this.actionHistory.update((history) => ({ ...history, [notificationId]: actions })),
      error: (error) => this.message.set(error?.error?.detail || 'Historique action indisponible.')
    });
  }

  private filterByActionStatus(notifications: AdminNotification[]): AdminNotification[] {
    if (this.filters.status === 'ALL') {
      return notifications;
    }
    return notifications.filter((notification) => notification.status === this.filters.status);
  }
}
