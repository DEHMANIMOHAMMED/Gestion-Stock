import { Component, Input } from '@angular/core';
import { Role } from '../../auth/auth.service';

@Component({
  selector: 'app-role-badge',
  standalone: true,
  template: `<span [class.owner]="role === 'OWNER'" [class.admin]="role === 'ADMIN'">{{ role }}</span>`,
  styles: [`
    span {
      background: #eef2f0;
      border-radius: 999px;
      color: var(--muted);
      display: inline-flex;
      font-size: 11px;
      font-weight: 900;
      padding: 5px 9px;
    }

    .admin {
      background: #eaf6ed;
      color: var(--green-strong);
    }

    .owner {
      background: #eef3ff;
      color: #3157c9;
    }
  `]
})
export class RoleBadgeComponent {
  @Input({ required: true }) role!: Role;
}
