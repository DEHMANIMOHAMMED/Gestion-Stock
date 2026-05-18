import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavigationComponent } from './shared/navigation.component';
import { AuthService } from './auth/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [NavigationComponent, RouterOutlet],
  template: `
    <div class="app-shell" [class.authenticated]="auth.isLoggedIn()">
      <app-navigation></app-navigation>
      <main class="app-main">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [
    `
      .app-shell {
        min-height: 100vh;
      }

      .app-main {
        margin: 0 auto;
        max-width: 1480px;
        min-height: 100vh;
        padding: 28px;
      }

      .app-shell.authenticated .app-main {
        margin-left: 282px;
        padding: 28px 32px;
      }

      @media (max-width: 920px) {
        .app-shell.authenticated .app-main {
          margin-left: 0;
          padding: 92px 16px 20px;
        }

        .app-main {
          padding: 16px;
        }
      }
    `
  ]
})
export class AppComponent {
  protected auth = inject(AuthService);
}
