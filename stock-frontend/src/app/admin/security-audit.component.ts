import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AiAuditLog, AiAuditLogFilters, AiService } from '../ai-reports/ai.service';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-security-audit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './security-audit.component.html',
  styleUrls: ['./security-audit.component.scss']
})
export class SecurityAuditComponent implements OnInit {
  private aiService = inject(AiService);
  auth = inject(AuthService);

  logs = signal<AiAuditLog[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  filters: AiAuditLogFilters = {
    action: '',
    actorEmail: '',
    targetType: '',
    source: '',
    module: '',
    severity: '',
    from: '',
    to: ''
  };

  totalEvents = computed(() => this.logs().length);
  procurementEvents = computed(() => this.logs().filter((log) => log.action.includes('PURCHASE_ORDER') || log.action.includes('APPROVAL')).length);
  notificationEvents = computed(() => this.logs().filter((log) => log.action.includes('NOTIFICATION')).length);
  aiEvents = computed(() => this.logs().filter((log) => log.action.includes('COPILOT') || log.action.includes('RECOMMENDATION')).length);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.aiService.getAiAuditLogs(this.filters).subscribe({
      next: (logs) => {
        this.logs.set(logs);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger les evenements d'audit.");
        this.loading.set(false);
      }
    });
  }

  reset(): void {
    this.filters = {
      action: '',
      actorEmail: '',
      targetType: '',
      source: '',
      module: '',
      severity: '',
      from: '',
      to: ''
    };
    this.load();
  }

  exportCsv(): void {
    this.aiService.exportAiAuditLogsCsv(this.filters).subscribe((blob) => this.download(blob, 'audit-logs.csv'));
  }

  exportPdf(): void {
    this.aiService.exportAiAuditLogsPdf(this.filters).subscribe((blob) => this.download(blob, 'audit-logs.pdf'));
  }

  trackById(_: number, log: AiAuditLog): number {
    return log.id;
  }

  moduleFor(log: AiAuditLog): string {
    const value = `${log.action} ${log.targetType}`.toUpperCase();
    if (value.includes('AUTH') || value.includes('USER')) return 'AUTH';
    if (value.includes('PURCHASE_ORDER') || value.includes('SUPPLIER') || value.includes('APPROVAL')) return 'PROCUREMENT';
    if (value.includes('NOTIFICATION')) return 'NOTIFICATION';
    if (value.includes('IMPORT')) return 'IMPORT';
    if (value.includes('COPILOT') || value.includes('RECOMMENDATION') || value.includes('AI_')) return 'AI';
    if (value.includes('PRODUCT')) return 'PRODUCT';
    return 'SYSTEM';
  }

  severityFor(log: AiAuditLog): string {
    const value = `${log.action} ${log.source} ${log.summary}`.toUpperCase();
    if (value.includes('CRITICAL') || value.includes('FAILED') || value.includes('CANCELLED')) return 'CRITICAL';
    if (value.includes('WARNING') || value.includes('THRESHOLD') || value.includes('DISMISSED')) return 'WARNING';
    return 'INFO';
  }

  private download(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }
}
