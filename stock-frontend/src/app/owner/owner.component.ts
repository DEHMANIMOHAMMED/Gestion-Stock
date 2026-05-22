import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EmptyStateComponent } from '../shared/ui/empty-state.component';
import { LoadingStateComponent } from '../shared/ui/loading-state.component';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { StatCardComponent } from '../shared/ui/stat-card.component';
import { OwnerDashboard, LegalSettings, OwnerService, OwnerSupportMessage, OwnerSupportUser } from './owner.service';

interface AttachmentTarget {
  attachmentName: string | null;
  attachmentContentType: string | null;
  attachmentData: string | null;
}

@Component({
  selector: 'app-owner',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent, StatCardComponent, LoadingStateComponent, EmptyStateComponent],
  templateUrl: './owner.component.html',
  styleUrls: ['./owner.component.scss']
})
export class OwnerComponent implements OnInit {
  private ownerService = inject(OwnerService);

  dashboard = signal<OwnerDashboard | null>(null);
  legalForm = signal<LegalSettings | null>(null);
  supportMessages = signal<OwnerSupportMessage[]>([]);
  supportUsers = signal<Record<number, OwnerSupportUser[]>>({});
  loading = false;
  saving = false;
  message = '';
  replyForms: Record<number, { message: string; attachmentName: string | null; attachmentContentType: string | null; attachmentData: string | null }> = {};
  passwordForms: Record<number, string> = {};

  ngOnInit(): void {
    this.load();
    this.loadSupportMessages();
  }

  load(): void {
    this.loading = true;
    this.message = '';
    this.ownerService.dashboard().subscribe({
      next: (dashboard) => {
        this.dashboard.set(dashboard);
        this.legalForm.set({ ...dashboard.legalSettings });
        this.loading = false;
      },
      error: () => {
        this.message = 'Impossible de charger l espace owner.';
        this.loading = false;
      }
    });
  }

  loadSupportMessages(): void {
    this.ownerService.supportMessages().subscribe({
      next: (messages) => this.supportMessages.set(messages),
      error: () => this.message = 'Impossible de charger les messages support.'
    });
  }

  markSupportRead(id: number): void {
    this.ownerService.markSupportMessageRead(id).subscribe({
      next: () => this.loadSupportMessages(),
      error: () => this.message = 'Impossible de marquer le message comme lu.'
    });
  }

  resolveSupport(id: number): void {
    this.ownerService.resolveSupportMessage(id).subscribe({
      next: () => this.loadSupportMessages(),
      error: () => this.message = 'Impossible de marquer le message comme resolu.'
    });
  }

  replySupport(id: number): void {
    const form = this.replyForm(id);
    if (!form.message.trim()) {
      return;
    }
    this.ownerService.replySupportMessage(id, form).subscribe({
      next: () => {
        this.replyForms[id] = { message: '', attachmentName: null, attachmentContentType: null, attachmentData: null };
        this.message = 'Reponse support envoyee.';
        this.loadSupportMessages();
      },
      error: () => this.message = "Impossible d'envoyer la reponse support."
    });
  }

  replyForm(id: number) {
    if (!this.replyForms[id]) {
      this.replyForms[id] = { message: '', attachmentName: null, attachmentContentType: null, attachmentData: null };
    }
    return this.replyForms[id];
  }

  loadOrganisationUsers(organisationId: number): void {
    this.ownerService.organisationUsers(organisationId).subscribe({
      next: (users) => this.supportUsers.set({ ...this.supportUsers(), [organisationId]: users }),
      error: () => this.message = 'Impossible de charger les comptes de cette organisation.'
    });
  }

  toggleUser(user: OwnerSupportUser): void {
    const reason = this.requestReason(user.enabled ? 'Raison de la desactivation du compte' : 'Raison de la reactivation du compte');
    if (!reason) {
      return;
    }
    this.ownerService.updateUserStatus(user.id, !user.enabled, reason).subscribe({
      next: () => {
        this.message = user.enabled ? 'Compte desactive.' : 'Compte reactive.';
        this.refreshLoadedUsers();
      },
      error: () => this.message = 'Impossible de modifier le statut du compte.'
    });
  }

  changePassword(user: OwnerSupportUser): void {
    const password = this.passwordForms[user.id]?.trim();
    if (!password || password.length < 8) {
      this.message = 'Le nouveau mot de passe doit contenir au moins 8 caracteres.';
      return;
    }
    const reason = this.requestReason('Raison du changement de mot de passe');
    if (!reason) {
      return;
    }
    this.ownerService.changeUserPassword(user.id, password, reason).subscribe({
      next: () => {
        this.passwordForms[user.id] = '';
        this.message = 'Mot de passe mis a jour.';
      },
      error: () => this.message = 'Impossible de changer le mot de passe.'
    });
  }

  cancelSubscription(organisationId: number): void {
    const reason = this.requestReason("Raison de l'annulation de l'abonnement");
    if (!reason) {
      return;
    }
    this.ownerService.cancelOrganisationSubscription(organisationId, reason).subscribe({
      next: () => {
        this.message = 'Abonnement marque pour resiliation.';
        this.load();
      },
      error: () => this.message = "Impossible d'annuler l'abonnement."
    });
  }

  usersFor(organisationId: number): OwnerSupportUser[] {
    return this.supportUsers()[organisationId] || [];
  }

  onAttachmentSelected(event: Event, target: AttachmentTarget): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      target.attachmentName = null;
      target.attachmentContentType = null;
      target.attachmentData = null;
      return;
    }
    if (!file.type.startsWith('image/')) {
      this.message = 'Seules les images sont acceptees.';
      input.value = '';
      return;
    }
    if (file.size > 2_000_000) {
      this.message = 'Image trop lourde. Limite conseillee: 2 Mo.';
      input.value = '';
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const result = String(reader.result || '');
      target.attachmentName = file.name;
      target.attachmentContentType = file.type;
      target.attachmentData = result.includes(',') ? result.split(',')[1] : result;
    };
    reader.readAsDataURL(file);
  }

  imageSrc(item: { attachmentContentType: string | null; attachmentData: string | null }): string | null {
    return item.attachmentContentType && item.attachmentData
      ? `data:${item.attachmentContentType};base64,${item.attachmentData}`
      : null;
  }

  private refreshLoadedUsers(): void {
    Object.keys(this.supportUsers()).forEach((organisationId) => this.loadOrganisationUsers(Number(organisationId)));
  }

  private requestReason(label: string): string | null {
    const reason = window.prompt(`${label} (obligatoire pour l'audit)`);
    const trimmed = reason?.trim();
    if (!trimmed) {
      this.message = 'Action annulee: une raison est obligatoire pour les actions support.';
      return null;
    }
    return trimmed;
  }

  saveLegalSettings(): void {
    const form = this.legalForm();
    if (!form || this.saving) {
      return;
    }
    this.saving = true;
    this.message = '';
    this.ownerService.updateLegalSettings(form).subscribe({
      next: (settings) => {
        const current = this.dashboard();
        if (current) {
          this.dashboard.set({ ...current, legalSettings: settings });
        }
        this.legalForm.set({ ...settings });
        this.message = 'Parametres legaux mis a jour.';
        this.saving = false;
      },
      error: () => {
        this.message = 'La mise a jour des parametres legaux a echoue.';
        this.saving = false;
      }
    });
  }

  updateLegalField(field: keyof LegalSettings, value: string): void {
    const form = this.legalForm();
    if (!form) {
      return;
    }
    this.legalForm.set({ ...form, [field]: value });
  }
}
