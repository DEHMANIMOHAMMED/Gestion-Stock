import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProcurementService, Supplier360 } from './procurement.service';

@Component({
  selector: 'app-supplier-360',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './supplier-360.component.html',
  styleUrls: ['./supplier-360.component.scss']
})
export class Supplier360Component implements OnInit {
  private route = inject(ActivatedRoute);
  private procurementService = inject(ProcurementService);

  supplier = signal<Supplier360 | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.procurementService.getSupplier360(id).subscribe({
      next: (supplier) => {
        this.supplier.set(supplier);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(error?.error?.detail || 'Impossible de charger le fournisseur.');
        this.loading.set(false);
      }
    });
  }

  healthLabel(score: number): string {
    if (score >= 85) {
      return 'Excellent';
    }
    if (score >= 70) {
      return 'Solide';
    }
    if (score >= 50) {
      return 'A surveiller';
    }
    return 'Risque eleve';
  }
}
