import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EmptyStateComponent } from '../shared/ui/empty-state.component';
import { LoadingStateComponent } from '../shared/ui/loading-state.component';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { SupportMessage, SupportService } from './support.service';

interface AttachmentTarget {
  attachmentName: string | null;
  attachmentContentType: string | null;
  attachmentData: string | null;
}

@Component({
  selector: 'app-support',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent, LoadingStateComponent, EmptyStateComponent],
  templateUrl: './support.component.html',
  styleUrls: ['./support.component.scss']
})
export class SupportComponent implements OnInit {
  private supportService = inject(SupportService);

  messages = signal<SupportMessage[]>([]);
  loading = signal(false);
  sending = signal(false);
  feedback = signal('');

  form = {
    subject: '',
    message: '',
    attachmentName: null as string | null,
    attachmentContentType: null as string | null,
    attachmentData: null as string | null
  };
  replyForms: Record<number, { message: string; attachmentName: string | null; attachmentContentType: string | null; attachmentData: string | null }> = {};

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.supportService.messages().subscribe({
      next: (messages) => {
        this.messages.set(messages);
        this.loading.set(false);
      },
      error: () => {
        this.feedback.set('Impossible de charger les messages support.');
        this.loading.set(false);
      }
    });
  }

  send(): void {
    if (this.sending() || !this.form.subject.trim() || !this.form.message.trim()) {
      return;
    }
    this.sending.set(true);
    this.feedback.set('');
    this.supportService.create({
      subject: this.form.subject,
      message: this.form.message,
      attachmentName: this.form.attachmentName,
      attachmentContentType: this.form.attachmentContentType,
      attachmentData: this.form.attachmentData
    }).subscribe({
      next: () => {
        this.form = { subject: '', message: '', attachmentName: null, attachmentContentType: null, attachmentData: null };
        this.feedback.set('Message envoye au support StockPilot.');
        this.sending.set(false);
        this.load();
      },
      error: (error) => {
        this.feedback.set(error?.error?.detail || "Impossible d'envoyer le message.");
        this.sending.set(false);
      }
    });
  }

  sendReply(messageId: number): void {
    const form = this.replyForms[messageId];
    if (!form?.message?.trim()) {
      return;
    }
    this.supportService.reply(messageId, form).subscribe({
      next: () => {
        this.replyForms[messageId] = { message: '', attachmentName: null, attachmentContentType: null, attachmentData: null };
        this.feedback.set('Reponse envoyee au support.');
        this.load();
      },
      error: () => this.feedback.set("Impossible d'envoyer la reponse.")
    });
  }

  replyForm(messageId: number) {
    if (!this.replyForms[messageId]) {
      this.replyForms[messageId] = { message: '', attachmentName: null, attachmentContentType: null, attachmentData: null };
    }
    return this.replyForms[messageId];
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
      this.feedback.set('Seules les images sont acceptees pour le support.');
      input.value = '';
      return;
    }
    if (file.size > 2_000_000) {
      this.feedback.set('Image trop lourde. Limite conseillee: 2 Mo.');
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
}
