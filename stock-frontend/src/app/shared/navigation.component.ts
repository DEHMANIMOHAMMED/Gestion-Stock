import { Component, effect } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../auth/auth.service';
import { NotificationService } from './notification.service';
import { RoleBadgeComponent } from './ui/role-badge.component';

// Simple top navigation bar that reacts to the authentication state.
@Component({
  selector: 'app-navigation',
  standalone: true,
  imports: [RouterModule, CommonModule, RoleBadgeComponent],
  templateUrl: './navigation.component.html',
  styleUrls: ['./navigation.component.scss']
})
export class NavigationComponent {
  constructor(public auth: AuthService, public notifications: NotificationService, private router: Router) {
    effect(() => {
      const user = this.auth.user();
      if (user?.role === 'ADMIN' && this.auth.canUseProFeatures()) {
        this.notifications.load();
        this.notifications.connect(this.auth.getToken());
      } else {
        this.notifications.disconnect();
      }
    });
  }

  logout() {
    this.notifications.disconnect();
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }

  markNotificationRead(id: number) {
    this.notifications.markRead(id);
  }
}
