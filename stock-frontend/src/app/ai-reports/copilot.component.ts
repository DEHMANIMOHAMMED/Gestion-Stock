import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  AiCopilotAction,
  AiCopilotConversation,
  AiCopilotResponse,
  AiRecommendationExplanation,
  AiService,
  AiWhatIfResponse
} from './ai.service';
import { AuthService } from '../auth/auth.service';
import { ProcurementService } from '../procurement/procurement.service';

@Component({
  selector: 'app-copilot',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './copilot.component.html',
  styleUrls: ['./ai-reports.scss', './copilot.component.scss']
})
export class CopilotComponent implements OnInit {
  private aiService = inject(AiService);
  private procurementService = inject(ProcurementService);
  private auth = inject(AuthService);
  private router = inject(Router);

  question = signal('Quels produits risquent une rupture cette semaine ?');
  conversationId = signal<number | null>(null);
  response = signal<AiCopilotResponse | null>(null);
  history = signal<AiCopilotConversation[]>([]);
  explanation = signal<AiRecommendationExplanation | null>(null);
  whatIfResult = signal<AiWhatIfResponse | null>(null);
  message = signal<string | null>(null);
  loading = signal(false);
  actionLoading = signal<string | null>(null);

  ngOnInit(): void {
    this.loadHistory();
  }

  ask(): void {
    const question = this.question().trim();
    if (!question) {
      return;
    }
    this.loading.set(true);
    this.message.set(null);
    this.explanation.set(null);
    this.whatIfResult.set(null);
    this.aiService.askCopilot(question, this.conversationId()).subscribe({
      next: (response) => {
        this.response.set(response);
        this.conversationId.set(response.conversationId);
        this.loadHistory();
        this.loading.set(false);
      },
      error: () => {
        this.response.set({
          conversationId: this.conversationId(),
          answer: 'Copilot indisponible pour le moment.',
          bullets: [],
          relatedProductIds: [],
          citations: [],
          source: 'ERROR',
          actions: []
        });
        this.loading.set(false);
      }
    });
  }

  openConversation(conversation: AiCopilotConversation): void {
    this.conversationId.set(conversation.id);
    const lastMessage = conversation.messages[conversation.messages.length - 1];
    if (lastMessage) {
      this.question.set(lastMessage.question);
      this.response.set({
        conversationId: conversation.id,
        answer: lastMessage.answer,
        bullets: [],
        relatedProductIds: [],
        citations: lastMessage.citations,
        source: lastMessage.source,
        actions: []
      });
    }
  }

  newConversation(): void {
    this.conversationId.set(null);
    this.response.set(null);
    this.explanation.set(null);
    this.whatIfResult.set(null);
    this.message.set(null);
    this.question.set("Quels produits dois-je commander aujourd'hui ?");
  }

  executeAction(action: AiCopilotAction): void {
    if (action.requiresAdminConfirmation && this.auth.user()?.role !== 'ADMIN') {
      this.message.set('Action reservee aux administrateurs.');
      return;
    }
    if (action.requiresAdminConfirmation && !confirm(`${action.label}\n\n${action.description}\n\nConfirmer cette action ?`)) {
      return;
    }
    const key = this.actionKey(action);
    this.actionLoading.set(key);
    this.message.set(null);

    if (action.type === 'CREATE_PURCHASE_ORDER' && action.recommendationId) {
      this.procurementService.createPurchaseOrderFromRecommendation(action.recommendationId, null).subscribe({
        next: (order) => {
          this.message.set(`Commande #${order.id} creee depuis le Copilot.`);
          this.actionLoading.set(null);
        },
        error: (error) => this.failAction(error, 'Impossible de creer la commande depuis le Copilot.')
      });
      return;
    }

    if (action.type === 'EXPLAIN_RECOMMENDATION' && action.recommendationId) {
      this.aiService.explainRecommendation(action.recommendationId).subscribe({
        next: (explanation) => {
          this.explanation.set(explanation);
          this.actionLoading.set(null);
        },
        error: (error) => this.failAction(error, "Impossible d'expliquer la recommandation.")
      });
      return;
    }

    if (action.type === 'RUN_WHAT_IF' && action.productId) {
      this.aiService.whatIf(action.productId, action.quantity || 20, action.leadTimeDays || 7).subscribe({
        next: (result) => {
          this.whatIfResult.set(result);
          this.actionLoading.set(null);
        },
        error: (error) => this.failAction(error, 'Impossible de lancer la simulation.')
      });
      return;
    }

    if (action.route) {
      this.router.navigateByUrl(action.route);
      this.actionLoading.set(null);
      return;
    }

    this.actionLoading.set(null);
  }

  actionKey(action: AiCopilotAction): string {
    return `${action.type}-${action.recommendationId || action.productId || action.supplierId || 'nav'}`;
  }

  private failAction(error: { error?: { detail?: string } }, fallback: string): void {
    this.message.set(error?.error?.detail || fallback);
    this.actionLoading.set(null);
  }

  private loadHistory(): void {
    this.aiService.getCopilotHistory().subscribe({
      next: (history) => this.history.set(history),
      error: () => this.history.set([])
    });
  }
}
