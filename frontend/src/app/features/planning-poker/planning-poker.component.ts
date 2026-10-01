import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { AvatarModule } from 'primeng/avatar';
import { TooltipModule } from 'primeng/tooltip';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { SessionService } from '../../core/services/session.service';
import { Participant, ParticipantRole } from '../../core/models/participant.model';
import { PlanningSession, SessionStatus } from '../../core/models/session.model';

@Component({
  selector: 'app-planning-poker',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    ButtonModule,
    TagModule,
    AvatarModule,
    TooltipModule,
    DialogModule,
    InputTextModule,
    SelectModule
  ],
  templateUrl: './planning-poker.component.html',
  styleUrls: ['./planning-poker.component.scss']
})
export class PlanningPokerComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly sessionService = inject(SessionService);

  // Données de session et participants
  protected readonly currentSession = this.sessionService.currentSession;
  protected readonly participants = this.sessionService.participants;

  // État local de l'utilisateur courant
  protected readonly currentUserName = signal<string>('Sarah M.');
  protected readonly currentUserRole = signal<ParticipantRole>('FACILITATOR');
  protected readonly currentParticipantId = signal<string | null>(null);

  // État du vote
  protected readonly selectedVote = signal<string | null>(null);
  protected readonly votesRevealed = signal<boolean>(false);
  protected readonly copiedCode = signal<boolean>(false);

  // Modal Rejoindre la session
  protected readonly showJoinDialog = signal<boolean>(false);
  protected readonly joinName = signal<string>('');
  protected readonly joinRole = signal<ParticipantRole>('VOTER');

  // Minuteur
  protected readonly remainingSeconds = signal<number>(60);
  protected readonly isTimerRunning = signal<boolean>(false);
  private timerInterval: any = null;

  // Calculs dérivés
  protected readonly deckCards = computed<string[]>(() => {
    const session = this.currentSession();
    if (session && session.deckValues && session.deckValues.length > 0) {
      return session.deckValues;
    }
    return ['0', '1', '2', '3', '5', '8', '13', '21', '34', '55', '89', '?', '☕'];
  });

  protected readonly onlineCount = computed<number>(() => {
    return this.participants().filter(p => p.online).length;
  });

  protected readonly voterCount = computed<number>(() => {
    return this.participants().filter(p => p.role === 'VOTER' || p.role === 'FACILITATOR').length;
  });

  protected readonly votedCount = computed<number>(() => {
    return this.participants().filter(p => p.voted || (p.name === this.currentUserName() && this.selectedVote() !== null)).length;
  });

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const sessionId = params['session'] || 'sess-101';
      this.loadSessionData(sessionId);
    });

    this.startTimer();
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  private loadSessionData(sessionId: string): void {
    this.sessionService.loadSession(sessionId).subscribe(session => {
      if (session) {
        this.remainingSeconds.set(session.timerDurationSeconds || 60);
        // Find if current user is in participants list
        const existing = session.participants.find(p => p.name === this.currentUserName());
        if (existing) {
          this.currentUserRole.set(existing.role);
          if (existing.id) {
            this.currentParticipantId.set(existing.id);
          }
        }
      }
    });
  }

  protected selectCard(card: string): void {
    if (this.currentUserRole() === 'OBSERVER') {
      return;
    }
    if (this.selectedVote() === card) {
      this.selectedVote.set(null);
    } else {
      this.selectedVote.set(card);
    }
  }

  protected toggleRevealVotes(): void {
    this.votesRevealed.update(v => !v);
  }

  protected resetVotes(): void {
    this.selectedVote.set(null);
    this.votesRevealed.set(false);
    this.remainingSeconds.set(this.currentSession()?.timerDurationSeconds || 60);
  }

  protected changeRole(newRole: ParticipantRole): void {
    this.currentUserRole.set(newRole);
    if (newRole === 'OBSERVER') {
      this.selectedVote.set(null);
    }
    const session = this.currentSession();
    const participantId = this.currentParticipantId();
    if (session && participantId) {
      this.sessionService.updateParticipantRole(session.id, participantId, newRole).subscribe();
    }
  }

  protected copyInviteCode(): void {
    const code = this.currentSession()?.inviteCode || '';
    if (code) {
      navigator.clipboard?.writeText(code);
      this.copiedCode.set(true);
      setTimeout(() => this.copiedCode.set(false), 2000);
    }
  }

  protected openJoinDialog(): void {
    this.joinName.set('');
    this.joinRole.set('VOTER');
    this.showJoinDialog.set(true);
  }

  protected confirmJoin(): void {
    const name = this.joinName().trim();
    if (!name) return;

    const session = this.currentSession();
    if (session) {
      this.sessionService.joinSession(session.id, {
        name,
        role: this.joinRole()
      }).subscribe(p => {
        this.currentUserName.set(name);
        this.currentUserRole.set(this.joinRole());
        if (p.id) {
          this.currentParticipantId.set(p.id);
        }
        this.showJoinDialog.set(false);
      });
    }
  }

  protected toggleSessionStatus(): void {
    const session = this.currentSession();
    if (!session) return;

    const newStatus: SessionStatus = session.status === 'IN_PROGRESS' ? 'COMPLETED' : 'IN_PROGRESS';
    this.sessionService.updateSessionStatus(session.id, newStatus).subscribe();
  }

  private startTimer(): void {
    this.isTimerRunning.set(true);
    this.timerInterval = setInterval(() => {
      if (this.remainingSeconds() > 0) {
        this.remainingSeconds.update(s => s - 1);
      }
    }, 1000);
  }
}
