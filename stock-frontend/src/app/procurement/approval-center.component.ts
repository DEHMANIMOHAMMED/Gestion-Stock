import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  ProcurementApprovalSettings,
  ProcurementService,
  PurchaseOrderApprovalItem,
  PurchaseOrderStatus,
  Supplier
} from './procurement.service';

@Component({
  selector: 'app-approval-center',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './approval-center.component.html',
  styleUrls: ['./approval-center.component.scss']
})
export class ApprovalCenterComponent implements OnInit {
  private procurementService = inject(ProcurementService);

  orders = signal<PurchaseOrderApprovalItem[]>([]);
  suppliers = signal<Supplier[]>([]);
  settings = signal<ProcurementApprovalSettings | null>(null);
  thresholdDraft = signal(1000);
  loading = signal(true);
  message = signal<string | null>(null);

  filters = {
    status: 'ALL' as 'ALL' | PurchaseOrderStatus,
    supplierId: 0,
    urgency: 'ALL',
    minAmount: null as number | null,
    riskLevel: 'ALL'
  };

  filteredOrders = computed(() => this.orders().filter((order) => {
    const statusMatches = this.filters.status === 'ALL' || order.status === this.filters.status;
    const supplierMatches = !this.filters.supplierId || order.supplierId === Number(this.filters.supplierId);
    const urgencyMatches = this.filters.urgency === 'ALL' || order.urgency === this.filters.urgency;
    const amountMatches = this.filters.minAmount == null || order.orderTotal >= Number(this.filters.minAmount);
    const riskMatches = this.filters.riskLevel === 'ALL' || order.maxRiskLevel === this.filters.riskLevel;
    return statusMatches && supplierMatches && urgencyMatches && amountMatches && riskMatches;
  }));

  pendingCount = computed(() => this.orders().filter((order) => order.status === 'DRAFT').length);
  approvedCount = computed(() => this.orders().filter((order) => order.status === 'APPROVED').length);
  criticalCount = computed(() => this.orders().filter((order) => order.urgency === 'CRITICAL').length);
  totalAmount = computed(() => this.filteredOrders().reduce((sum, order) => sum + Number(order.orderTotal), 0));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.message.set(null);
    this.procurementService.getSuppliers().subscribe({
      next: (suppliers) => this.suppliers.set(suppliers),
      error: () => this.message.set('Impossible de charger les fournisseurs.')
    });
    this.procurementService.getApprovalSettings().subscribe({
      next: (settings) => {
        this.settings.set(settings);
        this.thresholdDraft.set(Number(settings.approvalThreshold));
      },
      error: (error) => this.message.set(error?.error?.detail || 'Reglage approbation indisponible.')
    });
    this.procurementService.getApprovalCenter().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: (error) => {
        this.message.set(error?.error?.detail || 'Centre approbation indisponible.');
        this.loading.set(false);
      }
    });
  }

  saveThreshold(): void {
    this.message.set(null);
    this.procurementService.updateApprovalSettings(Number(this.thresholdDraft())).subscribe({
      next: (settings) => {
        this.settings.set(settings);
        this.thresholdDraft.set(Number(settings.approvalThreshold));
        this.message.set('Seuil approbation mis a jour.');
      },
      error: (error) => this.message.set(error?.error?.detail || 'Modification du seuil impossible.')
    });
  }

  approve(order: PurchaseOrderApprovalItem): void {
    this.message.set(null);
    this.procurementService.approvePurchaseOrder(order.id).subscribe({
      next: () => this.load(),
      error: (error) => this.message.set(error?.error?.detail || 'Approbation impossible.')
    });
  }

  confirm(order: PurchaseOrderApprovalItem): void {
    this.message.set(null);
    this.procurementService.confirmPurchaseOrder(order.id).subscribe({
      next: () => this.load(),
      error: (error) => this.message.set(error?.error?.detail || 'Confirmation impossible.')
    });
  }

  cancel(order: PurchaseOrderApprovalItem): void {
    this.message.set(null);
    this.procurementService.cancelPurchaseOrder(order.id).subscribe({
      next: () => this.load(),
      error: (error) => this.message.set(error?.error?.detail || 'Annulation impossible.')
    });
  }
}
