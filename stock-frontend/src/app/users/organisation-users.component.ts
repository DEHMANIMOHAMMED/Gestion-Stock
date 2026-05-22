import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService, Role } from '../auth/auth.service';
import { EmptyStateComponent } from '../shared/ui/empty-state.component';
import { LoadingStateComponent } from '../shared/ui/loading-state.component';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { RoleBadgeComponent } from '../shared/ui/role-badge.component';
import { OrganisationUser, OrganisationUserService } from './organisation-user.service';

type TenantRole = Exclude<Role, 'OWNER'>;

@Component({
  selector: 'app-organisation-users',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, PageHeaderComponent, LoadingStateComponent, EmptyStateComponent, RoleBadgeComponent],
  templateUrl: './organisation-users.component.html',
  styleUrls: ['./organisation-users.component.scss']
})
export class OrganisationUsersComponent implements OnInit {
  private readonly starterUserLimit = 3;
  private userService = inject(OrganisationUserService);
  auth = inject(AuthService);

  users = signal<OrganisationUser[]>([]);
  loading = signal(false);
  saving = signal(false);
  message = signal('');

  form = {
    email: '',
    password: '',
    role: 'USER' as TenantRole
  };
  passwordForm = {
    currentPassword: '',
    newPassword: ''
  };
  resetPasswords = signal<Record<number, string>>({});

  isStarterPlan(): boolean {
    return this.auth.user()?.planCode === 'STARTER';
  }

  userLimitLabel(): string {
    return this.isStarterPlan() ? `${this.users().length}/${this.starterUserLimit} comptes Starter` : 'Comptes illimites Pro';
  }

  starterLimitReached(): boolean {
    return this.isStarterPlan() && this.users().length >= this.starterUserLimit;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.message.set('');
    this.userService.list().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Impossible de charger les utilisateurs.');
        this.loading.set(false);
      }
    });
  }

  create(): void {
    if (this.saving()) {
      return;
    }
    if (this.starterLimitReached()) {
      this.message.set('Limite Starter atteinte: passez en Pro pour ajouter plus de comptes.');
      return;
    }
    this.saving.set(true);
    this.message.set('');
    this.userService.create({
      email: this.form.email,
      password: this.form.password,
      role: this.form.role
    }).subscribe({
      next: () => {
        this.form = { email: '', password: '', role: 'USER' };
        this.message.set('Utilisateur cree.');
        this.saving.set(false);
        this.load();
      },
      error: (error) => {
        this.message.set(error?.error?.detail || 'Creation utilisateur impossible.');
        this.saving.set(false);
      }
    });
  }

  updateRole(user: OrganisationUser, role: TenantRole): void {
    if (user.role === role || user.id === this.auth.user()?.userId) {
      return;
    }
    this.message.set('');
    this.userService.updateRole(user.id, role).subscribe({
      next: () => {
        this.message.set('Role utilisateur mis a jour.');
        this.load();
      },
      error: (error) => this.message.set(error?.error?.detail || 'Mise a jour du role impossible.')
    });
  }

  setEnabled(user: OrganisationUser, enabled: boolean): void {
    if (user.id === this.auth.user()?.userId) {
      return;
    }
    this.userService.setEnabled(user.id, enabled).subscribe({
      next: () => {
        this.message.set(enabled ? 'Utilisateur reactive.' : 'Utilisateur desactive.');
        this.load();
      },
      error: (error) => this.message.set(error?.error?.detail || 'Changement de statut impossible.')
    });
  }

  setResetPassword(userId: number, value: string): void {
    this.resetPasswords.update((passwords) => ({ ...passwords, [userId]: value }));
  }

  resetPassword(user: OrganisationUser): void {
    const temporaryPassword = this.resetPasswords()[user.id];
    if (!temporaryPassword || temporaryPassword.length < 8) {
      this.message.set('Mot de passe temporaire: 8 caracteres minimum.');
      return;
    }
    this.userService.resetPassword(user.id, temporaryPassword).subscribe({
      next: () => {
        this.resetPasswords.update((passwords) => ({ ...passwords, [user.id]: '' }));
        this.message.set('Mot de passe temporaire mis a jour.');
      },
      error: (error) => this.message.set(error?.error?.detail || 'Reset mot de passe impossible.')
    });
  }

  changeOwnPassword(): void {
    if (!this.passwordForm.currentPassword || this.passwordForm.newPassword.length < 8) {
      this.message.set('Nouveau mot de passe: 8 caracteres minimum.');
      return;
    }
    this.auth.changePassword(this.passwordForm.currentPassword, this.passwordForm.newPassword).subscribe({
      next: () => {
        this.passwordForm = { currentPassword: '', newPassword: '' };
        this.message.set('Ton mot de passe a ete mis a jour.');
      },
      error: (error) => this.message.set(error?.error?.detail || 'Changement de mot de passe impossible.')
    });
  }
}
