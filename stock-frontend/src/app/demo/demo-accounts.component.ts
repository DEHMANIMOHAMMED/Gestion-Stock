import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { DemoAccount, DemoService } from './demo.service';

@Component({
  selector: 'app-demo-accounts',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './demo-accounts.component.html',
  styleUrls: ['./demo-accounts.component.scss']
})
export class DemoAccountsComponent implements OnInit {
  private demoService = inject(DemoService);
  accounts = signal<DemoAccount[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    this.demoService.getAccounts().subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }
}
