import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Product, ProductService } from '../products/product.service';
import {
  ProcurementService,
  ProductSupplier,
  PurchaseOrder,
  PurchaseOrderAuditLog,
  Supplier,
  SupplierComparison,
  SupplierSla
} from './procurement.service';

@Component({
  selector: 'app-procurement',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './procurement.component.html',
  styleUrls: ['./procurement.component.scss']
})
export class ProcurementComponent implements OnInit {
  private procurementService = inject(ProcurementService);
  private productService = inject(ProductService);

  suppliers = signal<Supplier[]>([]);
  orders = signal<PurchaseOrder[]>([]);
  products = signal<Product[]>([]);
  productSuppliers = signal<ProductSupplier[]>([]);
  supplierComparisons = signal<SupplierComparison[]>([]);
  supplierSlas = signal<SupplierSla[]>([]);
  auditLogs = signal<Record<number, PurchaseOrderAuditLog[]>>({});
  expandedAuditOrderId = signal<number | null>(null);
  draftQuantities = signal<Record<number, number>>({});
  receiveQuantities = signal<Record<number, number>>({});
  importErrors = signal<string[]>([]);
  loading = signal(true);
  message = signal<string | null>(null);

  supplierForm = {
    name: '',
    email: '',
    phone: '',
    leadTimeDays: 7
  };

  orderForm = {
    supplierId: 0,
    productId: 0,
    quantity: 1,
    unitCost: null as number | null,
    expectedDeliveryDate: ''
  };

  productSupplierForm = {
    productId: 0,
    supplierId: 0,
    unitCost: null as number | null,
    minimumOrderQuantity: 1,
    preferred: true
  };

  slaForm = {
    supplierId: 0,
    targetLeadTimeDays: 7,
    targetOnTimeRate: 95,
    targetConformityRate: 98,
    notes: ''
  };

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.message.set(null);
    this.procurementService.getSuppliers().subscribe((suppliers) => {
      this.suppliers.set(suppliers);
      if (!this.orderForm.supplierId && suppliers.length) {
        this.orderForm.supplierId = suppliers[0].id;
      }
      if (!this.productSupplierForm.supplierId && suppliers.length) {
        this.productSupplierForm.supplierId = suppliers[0].id;
      }
      if (!this.slaForm.supplierId && suppliers.length) {
        this.slaForm.supplierId = suppliers[0].id;
      }
    });
    this.procurementService.getProductSuppliers().subscribe((productSuppliers) => this.productSuppliers.set(productSuppliers));
    this.procurementService.getSupplierComparison().subscribe((comparison) => this.supplierComparisons.set(comparison));
    this.procurementService.getSupplierSlas().subscribe((slas) => this.supplierSlas.set(slas));
    this.procurementService.getPurchaseOrders().subscribe((orders) => {
      this.orders.set(orders);
      this.draftQuantities.set(this.buildDraftQuantities(orders));
      this.receiveQuantities.set(this.buildReceiveQuantities(orders));
    });
    this.productService.getAll().subscribe({
      next: (products) => {
        this.products.set(products);
        if (!this.orderForm.productId && products.length) {
          this.orderForm.productId = products[0].id;
        }
        if (!this.productSupplierForm.productId && products.length) {
          this.productSupplierForm.productId = products[0].id;
        }
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Impossible de charger les donnees achats.');
        this.loading.set(false);
      }
    });
  }

  saveSla(): void {
    this.message.set(null);
    this.procurementService.upsertSupplierSla({
      supplierId: Number(this.slaForm.supplierId),
      targetLeadTimeDays: Number(this.slaForm.targetLeadTimeDays),
      targetOnTimeRate: Number(this.slaForm.targetOnTimeRate),
      targetConformityRate: Number(this.slaForm.targetConformityRate),
      notes: this.slaForm.notes || null
    }).subscribe({
      next: () => {
        this.slaForm.notes = '';
        this.load();
      },
      error: (error) => this.message.set(error?.error?.detail || 'SLA fournisseur impossible.')
    });
  }

  createSupplier(): void {
    this.message.set(null);
    this.procurementService.createSupplier({
      name: this.supplierForm.name,
      email: this.supplierForm.email || null,
      phone: this.supplierForm.phone || null,
      leadTimeDays: Number(this.supplierForm.leadTimeDays)
    }).subscribe({
      next: () => {
        this.supplierForm = { name: '', email: '', phone: '', leadTimeDays: 7 };
        this.load();
      },
      error: (error) => this.message.set(error?.error?.detail || 'Creation fournisseur impossible.')
    });
  }

  createOrder(): void {
    this.message.set(null);
    this.procurementService.createPurchaseOrder({
      supplierId: Number(this.orderForm.supplierId),
      expectedDeliveryDate: this.orderForm.expectedDeliveryDate || null,
      lines: [{
        productId: Number(this.orderForm.productId),
        quantity: Number(this.orderForm.quantity),
        unitCost: this.orderForm.unitCost
      }]
    }).subscribe({
      next: () => {
        this.orderForm.quantity = 1;
        this.orderForm.unitCost = null;
        this.orderForm.expectedDeliveryDate = '';
        this.load();
      },
      error: (error) => this.message.set(error?.error?.detail || 'Creation commande impossible.')
    });
  }

  saveProductSupplier(): void {
    this.message.set(null);
    this.procurementService.upsertProductSupplier({
      productId: Number(this.productSupplierForm.productId),
      supplierId: Number(this.productSupplierForm.supplierId),
      unitCost: this.productSupplierForm.unitCost,
      minimumOrderQuantity: Number(this.productSupplierForm.minimumOrderQuantity),
      preferred: this.productSupplierForm.preferred
    }).subscribe({
      next: () => {
        this.productSupplierForm.unitCost = null;
        this.productSupplierForm.minimumOrderQuantity = 1;
        this.productSupplierForm.preferred = true;
        this.load();
      },
      error: (error) => this.message.set(error?.error?.detail || 'Association produit-fournisseur impossible.')
    });
  }

  receive(order: PurchaseOrder): void {
    this.message.set(null);
    this.procurementService.receivePurchaseOrder(order.id, this.receivableLines(order)).subscribe({
      next: () => this.load(),
      error: (error) => this.message.set(error?.error?.detail || 'Reception impossible.')
    });
  }

  receiveAll(order: PurchaseOrder): void {
    this.message.set(null);
    this.procurementService.receivePurchaseOrder(order.id).subscribe({
      next: () => this.load(),
      error: (error) => this.message.set(error?.error?.detail || 'Reception impossible.')
    });
  }

  updateDraft(order: PurchaseOrder): void {
    this.message.set(null);
    this.procurementService.updatePurchaseOrder(order.id, {
      supplierId: order.supplierId,
      expectedDeliveryDate: order.expectedDeliveryDate,
      lines: order.lines.map((line) => ({
        productId: line.productId,
        quantity: Number(this.draftQuantities()[line.id] || line.quantity),
        unitCost: line.unitCost
      }))
    }).subscribe({
      next: () => this.load(),
      error: (error) => this.message.set(error?.error?.detail || 'Modification brouillon impossible.')
    });
  }

  confirm(order: PurchaseOrder): void {
    this.message.set(null);
    this.procurementService.confirmPurchaseOrder(order.id).subscribe({
      next: () => this.load(),
      error: (error) => this.message.set(error?.error?.detail || 'Confirmation impossible.')
    });
  }

  approve(order: PurchaseOrder): void {
    this.message.set(null);
    this.procurementService.approvePurchaseOrder(order.id).subscribe({
      next: () => this.load(),
      error: (error) => this.message.set(error?.error?.detail || 'Approbation impossible.')
    });
  }

  cancel(order: PurchaseOrder): void {
    this.message.set(null);
    this.procurementService.cancelPurchaseOrder(order.id).subscribe({
      next: () => this.load(),
      error: (error) => this.message.set(error?.error?.detail || 'Annulation impossible.')
    });
  }

  toggleAudit(order: PurchaseOrder): void {
    if (this.expandedAuditOrderId() === order.id) {
      this.expandedAuditOrderId.set(null);
      return;
    }

    this.expandedAuditOrderId.set(order.id);
    if (this.auditLogs()[order.id]) {
      return;
    }

    this.procurementService.getPurchaseOrderAudit(order.id).subscribe({
      next: (logs) => this.auditLogs.update((values) => ({ ...values, [order.id]: logs })),
      error: (error) => this.message.set(error?.error?.detail || 'Historique audit indisponible.')
    });
  }

  downloadPdf(order: PurchaseOrder): void {
    this.procurementService.downloadPurchaseOrderPdf(order.id).subscribe((blob) => {
      this.download(blob, `purchase-order-${order.id}.pdf`);
    });
  }

  exportAccounting(): void {
    this.procurementService.exportAccounting().subscribe((blob) => {
      this.download(blob, 'purchase-orders-accounting.csv');
    });
  }

  importSuppliers(event: Event): void {
    this.importFile(event, (file) => this.procurementService.importSuppliers(file), 'fournisseur');
  }

  importPurchaseOrders(event: Event): void {
    this.importFile(event, (file) => this.procurementService.importPurchaseOrders(file), 'commande');
  }

  downloadImportErrors(): void {
    this.download(new Blob([this.importErrors().join('\n')], { type: 'text/plain;charset=utf-8' }), 'procurement-import-errors.txt');
  }

  setDraftQuantity(lineId: number, quantity: number): void {
    this.draftQuantities.update((values) => ({ ...values, [lineId]: quantity }));
  }

  setReceiveQuantity(lineId: number, quantity: number): void {
    this.receiveQuantities.update((values) => ({ ...values, [lineId]: quantity }));
  }

  remaining(line: PurchaseOrder['lines'][number]): number {
    return line.quantity - line.receivedQuantity;
  }

  private receivableLines(order: PurchaseOrder): Array<{ lineId: number; quantity: number }> {
    return order.lines
      .map((line) => ({
        lineId: line.id,
        quantity: Number(this.receiveQuantities()[line.id] || 0)
      }))
      .filter((line) => line.quantity > 0);
  }

  private buildDraftQuantities(orders: PurchaseOrder[]): Record<number, number> {
    return orders.flatMap((order) => order.lines)
      .reduce<Record<number, number>>((values, line) => ({ ...values, [line.id]: line.quantity }), {});
  }

  private buildReceiveQuantities(orders: PurchaseOrder[]): Record<number, number> {
    return orders.flatMap((order) => order.lines)
      .reduce<Record<number, number>>((values, line) => ({ ...values, [line.id]: Math.max(0, this.remaining(line)) }), {});
  }

  private download(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    window.URL.revokeObjectURL(url);
  }

  private importFile(
    event: Event,
    importer: (file: File) => import('rxjs').Observable<{ created: number; skipped: number; errors: string[] }>,
    label: string
  ): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.importErrors.set([]);
    importer(file).subscribe({
      next: (result) => {
        this.message.set(`${result.created} ${label}(s) importe(s), ${result.skipped} ignore(s).`);
        this.importErrors.set(result.errors);
        input.value = '';
        this.load();
      },
      error: (error) => {
        this.message.set(error?.error?.detail || `Import ${label} impossible.`);
        input.value = '';
      }
    });
  }
}
