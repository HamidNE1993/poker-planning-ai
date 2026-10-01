import { Component, inject, signal } from '@angular/core';
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
import { SelectModule } from 'primeng/select';
import { SessionService } from '../../core/services/session.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { DeckType } from '../../core/models/session.model';

interface DeckOption {
  label: string;
  value: DeckType;
  description: string;
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
    TooltipModule,
    SelectModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
  private readonly router = inject(Router);
  private readonly sessionService = inject(SessionService);
  private readonly dashboardService = inject(DashboardService);

  // Signaux exposés pour le template
  protected readonly metrics = this.dashboardService.metrics;
  protected readonly sessions = this.sessionService.sessions;
  protected readonly aiInsights = this.dashboardService.aiInsights;

  // Options de deck
  protected readonly deckOptions: DeckOption[] = [
    { label: 'Fibonacci Standard', value: 'FIBONACCI', description: '0, 1, 2, 3, 5, 8, 13, 21...' },
    { label: 'Fibonacci Modifié', value: 'MODIFIED_FIBONACCI', description: '0, 0.5, 1, 2, 3, 5, 8, 13, 20...' },
    { label: 'T-Shirt Sizes', value: 'T_SHIRT', description: 'XS, S, M, L, XL, XXL' },
    { label: 'Puissances de 2', value: 'POWERS_OF_TWO', description: '0, 1, 2, 4, 8, 16, 32, 64' }
  ];

  // État du modal de création
  protected readonly showNewSessionDialog = signal<boolean>(false);
  protected readonly newSessionName = signal<string>('');
  protected readonly newSessionSprint = signal<string>('Sprint 13');
  protected readonly newSessionFacilitator = signal<string>('Scrum Master');
  protected readonly selectedDeckType = signal<DeckType>('FIBONACCI');
  protected readonly timerSeconds = signal<number>(60);
  protected readonly isCreating = signal<boolean>(false);

  protected openCreateSessionDialog(): void {
    this.newSessionName.set('');
    this.newSessionSprint.set(`Sprint ${Math.floor(Math.random() * 5) + 13}`);
    this.newSessionFacilitator.set('Vous');
    this.selectedDeckType.set('FIBONACCI');
    this.timerSeconds.set(60);
    this.showNewSessionDialog.set(true);
  }

  protected createSession(): void {
    const name = this.newSessionName().trim();
    if (!name) {
      return;
    }

    this.isCreating.set(true);
    this.sessionService.createSession({
      name,
      sprint: this.newSessionSprint(),
      deckType: this.selectedDeckType(),
      timerDurationSeconds: this.timerSeconds(),
      facilitatorName: this.newSessionFacilitator().trim() || 'Scrum Master'
    }).subscribe({
      next: (created) => {
        this.isCreating.set(false);
        this.showNewSessionDialog.set(false);
        this.router.navigate(['/planning-poker'], { queryParams: { session: created.id } });
      },
      error: () => {
        this.isCreating.set(false);
        this.showNewSessionDialog.set(false);
      }
    });
  }

  protected joinSession(sessionId: string): void {
    this.router.navigate(['/planning-poker'], { queryParams: { session: sessionId } });
  }
}
