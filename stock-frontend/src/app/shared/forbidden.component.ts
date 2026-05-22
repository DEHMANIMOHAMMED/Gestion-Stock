import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <main class="forbidden-page">
      <section class="forbidden-card">
        <span>403</span>
        <h1>Acces refuse</h1>
        <p>Ton role actuel ne permet pas d'acceder a cette section.</p>
        <a [routerLink]="auth.landingPath()">Retour a mon espace</a>
      </section>
    </main>
  `,
  styles: [`
    .forbidden-page {
      display: grid;
      min-height: calc(100vh - 120px);
      place-items: center;
    }

    .forbidden-card {
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 16px;
      box-shadow: 0 24px 70px rgba(20, 34, 30, 0.08);
      max-width: 460px;
      padding: 34px;
      text-align: center;
    }

    span {
      color: var(--green-strong);
      font-size: 13px;
      font-weight: 900;
    }

    h1 {
      font-size: 32px;
      margin: 8px 0;
    }

    p {
      color: var(--muted);
      margin-bottom: 22px;
    }

    a {
      background: var(--green);
      border-radius: 10px;
      color: #fff;
      display: inline-flex;
      font-weight: 800;
      padding: 12px 16px;
      text-decoration: none;
    }
  `]
})
export class ForbiddenComponent {
  auth = inject(AuthService);
}
