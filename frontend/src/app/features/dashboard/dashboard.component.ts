import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { AvatarModule } from 'primeng/avatar';
import { AvatarGroupModule } from 'primeng/avatargroup';
import { ProgressBarModule } from 'primeng/progressbar';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

export interface RecentSession {
  id: string;
  name: string;
  sprint: string;
  storyCount: number;
  completedStories: number;
  lastActivity: string;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'PLANNED';
  statusLabel: string;
  severity: 'success' | 'info' | 'warn' | 'secondary';
  participants: { name: string; avatar?: string }[];
  aiAgreementRate: number;
}

export interface MetricCard {
  title: string;
  value: string;
  change: string;
  trend: 'up' | 'down' | 'neutral';
  icon: string;
  colorClass: string;
  description: string;
}

export interface AiInsight {
  storyTitle: string;
  sessionName: string;
  type: 'AMBIGUITY' | 'ESTIMATE_GAP' | 'CONSENSUS_READY';
  title: string;
  message: string;
  timeAgo: string;
  confidence: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    ButtonModule,
    CardModule,
    TagModule,
    AvatarModule,
    AvatarGroupModule,
    ProgressBarModule,
    DialogModule,
    InputTextModule,
    TooltipModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
  // Modal de création rapide
  protected showNewSessionDialog = signal<boolean>(false);
  protected newSessionName = signal<string>('');
  protected newSessionSprint = signal<string>('Sprint 13');

  // Métriques globales
  protected readonly metrics = signal<MetricCard[]>([
    {
      title: 'Sessions actives',
      value: '3',
      change: '+1 ce matin',
      trend: 'up',
      icon: 'pi-bolt',
      colorClass: 'purple',
      description: 'Sessions en cours d\'estimation'
    },
    {
      title: 'Stories estimées',
      value: '148',
      change: '+24 ce mois',
      trend: 'up',
      icon: 'pi-check-circle',
      colorClass: 'blue',
      description: 'Stories validées par l\'équipe'
    },
    {
      title: 'Accord moyen avec l\'IA',
      value: '84%',
      change: '+6% vs sprint précédent',
      trend: 'up',
      icon: 'pi-sparkles',
      colorClass: 'green',
      description: 'Convergence des votes équipe & IA'
    },
    {
      title: 'Temps moyen / Story',
      value: '2.4 min',
      change: '-30s d\'optimisation',
      trend: 'up',
      icon: 'pi-clock',
      colorClass: 'orange',
      description: 'Grâce aux pré-analyses d\'ambiguïté'
    }
  ]);

  // Sessions récentes
  protected readonly sessions = signal<RecentSession[]>([
    {
      id: 'sess-101',
      name: 'Refonte Module Facturation & Stripe',
      sprint: 'Sprint 12 - Core',
      storyCount: 8,
      completedStories: 5,
      lastActivity: 'Il y a 10 min',
      status: 'IN_PROGRESS',
      statusLabel: 'En cours',
      severity: 'success',
      participants: [
        { name: 'Sarah M.' },
        { name: 'Alex K.' },
        { name: 'Thomas D.' },
        { name: 'Elena V.' }
      ],
      aiAgreementRate: 88
    },
    {
      id: 'sess-102',
      name: 'Optimisation Performances API REST',
      sprint: 'Sprint 12 - Backend',
      storyCount: 6,
      completedStories: 6,
      lastActivity: 'Il y a 2 heures',
      status: 'COMPLETED',
      statusLabel: 'Terminée',
      severity: 'info',
      participants: [
        { name: 'Sarah M.' },
        { name: 'Thomas D.' },
        { name: 'Lucas B.' }
      ],
      aiAgreementRate: 92
    },
    {
      id: 'sess-103',
      name: 'Design System & Accessibilité Mobile',
      sprint: 'Sprint 13 - Frontend',
      storyCount: 12,
      completedStories: 0,
      lastActivity: 'Créée hier',
      status: 'PLANNED',
      statusLabel: 'Planifiée',
      severity: 'secondary',
      participants: [
        { name: 'Elena V.' },
        { name: 'Alex K.' }
      ],
      aiAgreementRate: 0
    }
  ]);

  // Alertes et insights IA
  protected readonly aiInsights = signal<AiInsight[]>([
    {
      storyTitle: 'AUTHENT-42: Authentification Biométrique WebAuthn',
      sessionName: 'Refonte Module Facturation',
      type: 'AMBIGUITY',
      title: 'Critères d\'acceptation incomplets détectés',
      message: 'L\'IA suggère de préciser la gestion du fallback OTP en cas d\'échec matériel avant l\'estimation finale.',
      timeAgo: 'Il y a 15 min',
      confidence: 94
    },
    {
      storyTitle: 'PAY-108: Webhook de réconciliation bancaire asynchrone',
      sessionName: 'Refonte Module Facturation',
      type: 'ESTIMATE_GAP',
      title: 'Écart de complexité détecté',
      message: 'Dispersion importante des votes (3 vs 13). Risque de dépendance externe identifié sur le fournisseur de paiement.',
      timeAgo: 'Il y a 35 min',
      confidence: 89
    }
  ]);

  constructor(private readonly router: Router) {}

  protected openCreateSessionDialog(): void {
    this.newSessionName.set('');
    this.newSessionSprint.set('Sprint ' + (Math.floor(Math.random() * 5) + 13));
    this.showNewSessionDialog.set(true);
  }

  protected createSession(): void {
    if (!this.newSessionName().trim()) return;

    const newId = 'sess-' + Date.now().toString().slice(-4);
    const newSession: RecentSession = {
      id: newId,
      name: this.newSessionName().trim(),
      sprint: this.newSessionSprint(),
      storyCount: 0,
      completedStories: 0,
      lastActivity: 'À l\'instant',
      status: 'IN_PROGRESS',
      statusLabel: 'En cours',
      severity: 'success',
      participants: [{ name: 'Vous' }],
      aiAgreementRate: 0
    };

    this.sessions.update(current => [newSession, ...current]);
    this.showNewSessionDialog.set(false);
    this.router.navigate(['/planning-poker']);
  }

  protected joinSession(sessionId: string): void {
    this.router.navigate(['/planning-poker'], { queryParams: { session: sessionId } });
  }
}
