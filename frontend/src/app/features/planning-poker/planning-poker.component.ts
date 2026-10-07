import { Component, OnInit, OnDestroy, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { AvatarModule } from 'primeng/avatar';
import { TooltipModule } from 'primeng/tooltip';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SessionService } from '../../core/services/session.service';
import { StoryService } from '../../core/services/story.service';
import { RealtimeService } from '../../core/services/realtime.service';
import { AiAssistantService } from '../../core/services/ai-assistant.service';
import { Participant, ParticipantRole } from '../../core/models/participant.model';
import { PlanningSession, SessionStatus } from '../../core/models/session.model';
import { StoryPriority, UserStory } from '../../core/models/story.model';
import { ConsensusStatistics, VoteDetail } from '../../core/models/vote.model';
import { SessionEvent } from '../../core/models/realtime.model';
import { StoryAnalysis } from '../../core/models/ai-analysis.model';

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
    TextareaModule
  ],
  templateUrl: './planning-poker.component.html',
  styleUrls: ['./planning-poker.component.scss']
})
export class PlanningPokerComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly sessionService = inject(SessionService);
  private readonly storyService = inject(StoryService);
  private readonly realtimeService = inject(RealtimeService);
  private readonly aiAssistantService = inject(AiAssistantService);
  private readonly router = inject(Router);

  private realtimeSubscription: Subscription | null = null;

  // Real-time status
  protected readonly isRealtimeConnected = this.realtimeService.isConnected;

  // AI Assistant State
  protected readonly aiAnalysis = this.aiAssistantService.currentAnalysis;
  protected readonly isAiLoading = this.aiAssistantService.isLoading;
  protected readonly aiError = this.aiAssistantService.error;
  protected readonly copiedQuestion = signal<string | null>(null);

  // Session & Participants
  protected readonly currentSession = this.sessionService.currentSession;
  protected readonly participants = this.sessionService.participants;

  // Backlog & Stories
  protected readonly stories = this.storyService.stories;
  protected readonly activeStory = this.storyService.activeStory;
  protected readonly currentVotesResponse = this.storyService.currentVotes;

  // Current User Local State
  protected readonly currentUserName = signal<string>('');
  protected readonly currentUserRole = signal<ParticipantRole>('VOTER');
  protected readonly currentParticipantId = signal<string | null>(null);

  // Voting State
  protected readonly selectedVote = signal<string | null>(null);
  protected readonly votesRevealed = signal<boolean>(false);
  protected readonly copiedCode = signal<boolean>(false);
  protected readonly finalEstimateInput = signal<string>('5');

  // Role Computations
  protected readonly isFacilitator = computed<boolean>(() => this.currentUserRole() === 'FACILITATOR');
  protected readonly isObserver = computed<boolean>(() => this.currentUserRole() === 'OBSERVER');
  protected readonly isVoter = computed<boolean>(() => this.currentUserRole() === 'VOTER');

  // Dialogs
  protected readonly showPseudoDialog = signal<boolean>(false);
  protected readonly pseudoInput = signal<string>('');

  protected readonly showAddStoryDialog = signal<boolean>(false);
  protected readonly newStoryTitle = signal<string>('');
  protected readonly newStoryDesc = signal<string>('');
  protected readonly newStoryAC = signal<string>('');
  protected readonly newStoryPriority = signal<StoryPriority>('MEDIUM');

  protected readonly showBacklogDrawer = signal<boolean>(false);

  // Timer & Polling
  protected readonly remainingSeconds = signal<number>(60);
  protected readonly isTimerRunning = signal<boolean>(false);
  private timerInterval: any = null;
  private pollingInterval: any = null;

  // Derived Computations
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

  protected readonly voteDetailsList = computed<VoteDetail[]>(() => {
    const resp = this.currentVotesResponse();
    if (resp && resp.votes) {
      return resp.votes;
    }
    // Fallback based on participants signal
    return this.participants().map(p => ({
      participantId: p.id || p.name,
      participantName: p.name,
      participantRole: p.role,
      voteValue: p.name === this.currentUserName() ? this.selectedVote() : (p.currentVote || null),
      hasVoted: p.name === this.currentUserName() ? this.selectedVote() !== null : (p.voted || false)
    }));
  });

  protected readonly votedCount = computed<number>(() => {
    return this.voteDetailsList().filter(v => v.hasVoted).length;
  });

  protected readonly consensusStats = computed<ConsensusStatistics | null>(() => {
    return this.currentVotesResponse()?.consensus || null;
  });

  constructor() {
    effect(() => {
      const active = this.activeStory();
      if (active) {
        this.loadAiAnalysisForActiveStory();
      } else {
        this.aiAssistantService.clearCurrentAnalysis();
      }
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      let sessionParam = params['session'];
      if (!sessionParam) {
        const available = this.sessionService.sessions();
        if (available.length > 0) {
          sessionParam = available[0].id;
        }
      }

      if (sessionParam) {
        this.initSession(sessionParam);
      } else {
        this.router.navigate(['/']);
      }
    });

    this.startTimer();
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
    }
    if (this.realtimeSubscription) {
      this.realtimeSubscription.unsubscribe();
      this.realtimeSubscription = null;
    }
    this.realtimeService.disconnect();
  }

  private initSession(identifier: string): void {
    this.sessionService.loadSession(identifier).subscribe(session => {
      if (session) {
        const realSessionId = session.id;
        this.remainingSeconds.set(session.timerDurationSeconds || 60);

        // Check if tab already has a stored pseudo in sessionStorage
        const storedPseudo = sessionStorage.getItem(`poker_user_${realSessionId}`);
        if (storedPseudo) {
          this.currentUserName.set(storedPseudo);
          const existing = session.participants.find(p => p.name.toLowerCase() === storedPseudo.toLowerCase());
          if (existing) {
            this.currentUserRole.set(existing.role);
            if (existing.id) {
              this.currentParticipantId.set(existing.id);
            }
          } else {
            this.joinWithPseudo(storedPseudo, realSessionId);
          }
        } else {
          // Open pseudo prompt modal so this tab can pick its own name
          this.pseudoInput.set('');
          this.showPseudoDialog.set(true);
        }

        // Load Stories & Votes with the real UUID
        this.storyService.loadStories(realSessionId).subscribe(() => {
          this.loadVotesForActiveStory();
        });

        // Connect real-time SSE stream
        this.subscribeToRealtimeEvents(realSessionId);

        // Backup gentle polling (every 10s) to keep presence heartbeat
        this.startHeartbeatPolling(realSessionId);
      }
    });
  }

  private subscribeToRealtimeEvents(sessionId: string): void {
    this.realtimeSubscription?.unsubscribe();
    this.realtimeSubscription = this.realtimeService.connect(sessionId).subscribe({
      next: (event: SessionEvent) => {
        this.handleRealtimeEvent(event);
      },
      error: (err) => {
        console.warn('[PlanningPokerComponent] Erreur flux SSE:', err);
      }
    });
  }

  private handleRealtimeEvent(event: SessionEvent): void {
    if (!event || !event.type) return;

    switch (event.type) {
      case 'SESSION_UPDATED':
        if (event.payload) {
          this.sessionService.applySessionUpdate(event.payload);
        }
        break;

      case 'PARTICIPANT_JOINED':
      case 'PARTICIPANT_UPDATED':
        if (event.payload) {
          this.sessionService.applyParticipantUpdate(event.payload);
          // If this update matches current user, ensure participantId is tracked
          if (event.payload.name?.toLowerCase() === this.currentUserName().toLowerCase() && event.payload.id) {
            this.currentParticipantId.set(event.payload.id);
            this.currentUserRole.set(event.payload.role);
          }
        }
        break;

      case 'PARTICIPANT_LEFT':
        if (event.payload) {
          this.sessionService.applyParticipantLeft(event.payload);
        }
        break;

      case 'STORY_CREATED':
        if (event.payload) {
          this.storyService.applyStoryCreated(event.payload);
        }
        break;

      case 'STORY_UPDATED':
        if (event.payload) {
          this.storyService.applyStoryUpdated(event.payload);
        }
        break;

      case 'STORY_DELETED':
        if (event.payload) {
          this.storyService.applyStoryDeleted(typeof event.payload === 'string' ? event.payload : event.payload.id);
        }
        break;

      case 'STORY_SELECTED':
        if (event.payload) {
          this.storyService.applyStorySelected(event.payload);
          this.selectedVote.set(null);
          this.votesRevealed.set(event.payload.votesRevealed || false);
          this.remainingSeconds.set(this.currentSession()?.timerDurationSeconds || 60);
          this.loadVotesForActiveStory();
        }
        break;

      case 'VOTE_SUBMITTED':
        if (event.payload) {
          this.storyService.applyVotesUpdate(event.payload);
          if (event.payload.revealed) {
            this.votesRevealed.set(true);
            if (event.payload.consensus?.median) {
              this.finalEstimateInput.set(event.payload.consensus.median);
            } else if (event.payload.consensus?.average) {
              this.finalEstimateInput.set(String(event.payload.consensus.average));
            }
          }
        }
        break;

      case 'VOTES_REVEALED':
        if (event.payload) {
          this.storyService.applyVotesUpdate(event.payload);
          this.votesRevealed.set(true);
          if (event.payload.consensus?.median) {
            this.finalEstimateInput.set(event.payload.consensus.median);
          } else if (event.payload.consensus?.average) {
            this.finalEstimateInput.set(String(event.payload.consensus.average));
          }
        }
        break;

      case 'VOTES_RESET':
        if (event.payload) {
          this.storyService.applyVotesUpdate(event.payload);
          this.votesRevealed.set(false);
          this.selectedVote.set(null);
          this.remainingSeconds.set(this.currentSession()?.timerDurationSeconds || 60);
        }
        break;

      case 'ESTIMATE_FINALIZED':
        if (event.payload) {
          this.storyService.applyFinalizeEstimate(event.payload);
          this.votesRevealed.set(true);
        }
        break;

      case 'TIMER_SYNC':
        if (event.payload) {
          if (event.payload.action === 'START') {
            this.startTimer();
          } else if (event.payload.action === 'PAUSE') {
            this.isTimerRunning.set(false);
            if (this.timerInterval) clearInterval(this.timerInterval);
          } else if (event.payload.action === 'RESET') {
            this.remainingSeconds.set(event.payload.remainingSeconds || 60);
          }
        }
        break;
    }
  }

  private startHeartbeatPolling(sessionId: string): void {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
    }
    this.pollingInterval = setInterval(() => {
      this.sessionService.loadSession(sessionId).subscribe(session => {
        if (session && this.currentUserName()) {
          const me = session.participants.find(p => p.name.toLowerCase() === this.currentUserName().toLowerCase());
          if (me && me.id && !this.currentParticipantId()) {
            this.currentParticipantId.set(me.id);
          }
        }
      });
    }, 10000);
  }

  protected confirmPseudo(): void {
    const pseudo = this.pseudoInput().trim();
    if (!pseudo) return;

    const session = this.currentSession();
    if (!session) return;

    this.joinWithPseudo(pseudo, session.id);
    this.showPseudoDialog.set(false);
  }

  private joinWithPseudo(pseudo: string, sessionId: string): void {
    this.currentUserName.set(pseudo);
    sessionStorage.setItem(`poker_user_${sessionId}`, pseudo);

    this.sessionService.joinSession(sessionId, {
      name: pseudo,
      role: 'VOTER'
    }).subscribe(p => {
      this.currentUserRole.set(p.role);
      if (p.id) {
        this.currentParticipantId.set(p.id);
      }
      this.loadVotesForActiveStory();
    });
  }

  protected selectCard(card: string): void {
    if (this.currentUserRole() === 'OBSERVER') {
      return;
    }

    const session = this.currentSession();
    const active = this.activeStory();
    const participantId = this.currentParticipantId();

    if (this.selectedVote() === card) {
      this.selectedVote.set(null);
    } else {
      this.selectedVote.set(card);
      if (session && active && participantId) {
        this.storyService.submitVote(session.id, active.id, participantId, card).subscribe(resp => {
          if (resp) {
            this.votesRevealed.set(resp.revealed);
          }
        });
      }
    }
  }

  protected toggleRevealVotes(): void {
    const session = this.currentSession();
    const active = this.activeStory();
    if (session && active) {
      if (!this.votesRevealed()) {
        this.storyService.revealVotes(session.id, active.id).subscribe(resp => {
          this.votesRevealed.set(true);
          if (resp?.consensus?.median) {
            this.finalEstimateInput.set(resp.consensus.median);
          } else if (resp?.consensus?.average) {
            this.finalEstimateInput.set(String(resp.consensus.average));
          }
        });
      } else {
        this.votesRevealed.set(false);
      }
    } else {
      this.votesRevealed.update(v => !v);
    }
  }

  protected resetVotes(): void {
    this.selectedVote.set(null);
    this.votesRevealed.set(false);
    this.remainingSeconds.set(this.currentSession()?.timerDurationSeconds || 60);

    const session = this.currentSession();
    const active = this.activeStory();
    if (session && active) {
      this.storyService.resetVotes(session.id, active.id).subscribe();
    }
  }

  protected finalizeEstimate(): void {
    const score = this.finalEstimateInput().trim();
    if (!score) return;

    const session = this.currentSession();
    const active = this.activeStory();
    if (session && active) {
      this.storyService.finalizeEstimate(session.id, active.id, score).subscribe(() => {
        // Find next pending story if available
        const next = this.stories().find(s => s.id !== active.id && s.status === 'PENDING');
        if (next) {
          this.selectStory(next);
        }
      });
    }
  }

  protected selectStory(story: UserStory): void {
    const session = this.currentSession();
    if (session) {
      this.storyService.selectActiveStory(session.id, story.id).subscribe(() => {
        this.selectedVote.set(null);
        this.votesRevealed.set(false);
        this.remainingSeconds.set(session.timerDurationSeconds || 60);
        this.loadVotesForActiveStory();
      });
    }
  }

  protected openAddStoryDialog(): void {
    this.newStoryTitle.set('');
    this.newStoryDesc.set('');
    this.newStoryAC.set('');
    this.newStoryPriority.set('MEDIUM');
    this.showAddStoryDialog.set(true);
  }

  protected confirmAddStory(): void {
    const title = this.newStoryTitle().trim();
    if (!title) return;

    const session = this.currentSession();
    if (!session) return;

    const acList = this.newStoryAC().split('\n').map(s => s.trim()).filter(s => s.length > 0);

    this.storyService.createStory(session.id, {
      title,
      description: this.newStoryDesc().trim(),
      acceptanceCriteria: acList,
      priority: this.newStoryPriority()
    }).subscribe(() => {
      this.showAddStoryDialog.set(false);
    });
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

  protected simulateTestVoter(): void {
    const session = this.currentSession();
    const active = this.activeStory();
    if (!session) return;

    const testNames = ['Bob (Dev)', 'Charlie (QA)', 'Diana (Frontend)', 'Eric (Backend)', 'Sophie (Lead)'];
    const available = testNames.find(n => !this.participants().some(p => p.name === n)) || `Testeur ${Date.now().toString().slice(-3)}`;

    this.sessionService.joinSession(session.id, {
      name: available,
      role: 'VOTER'
    }).subscribe(p => {
      if (p.id && active) {
        const cards = this.deckCards().filter(c => c !== '?' && c !== '☕');
        const randomCard = cards[Math.floor(Math.random() * cards.length)];
        this.storyService.submitVote(session.id, active.id, p.id, randomCard).subscribe(() => {
          this.loadVotesForActiveStory();
        });
      }
    });
  }

  protected copyInviteCode(): void {
    const code = this.currentSession()?.inviteCode || '';
    if (code) {
      navigator.clipboard?.writeText(code);
      this.copiedCode.set(true);
      setTimeout(() => this.copiedCode.set(false), 2000);
    }
  }

  protected toggleSessionStatus(): void {
    const session = this.currentSession();
    if (!session) return;

    const newStatus: SessionStatus = session.status === 'IN_PROGRESS' ? 'COMPLETED' : 'IN_PROGRESS';
    this.sessionService.updateSessionStatus(session.id, newStatus).subscribe();
  }

  protected loadAiAnalysisForActiveStory(forceRefresh: boolean = false): void {
    const active = this.activeStory();
    if (active) {
      this.aiAssistantService.getStoryAnalysis(active.id, forceRefresh).subscribe({
        error: (err) => console.warn('[AiAssistant] Erreur lors de la récupération de l\'analyse:', err)
      });
    } else {
      this.aiAssistantService.clearCurrentAnalysis();
    }
  }

  protected refreshAiAnalysis(): void {
    this.loadAiAnalysisForActiveStory(true);
  }

  protected copyQuestionToClipboard(question: string): void {
    if (navigator?.clipboard) {
      navigator.clipboard.writeText(question);
      this.copiedQuestion.set(question);
      setTimeout(() => {
        if (this.copiedQuestion() === question) {
          this.copiedQuestion.set(null);
        }
      }, 2000);
    }
  }

  private loadVotesForActiveStory(): void {
    const session = this.currentSession();
    const active = this.activeStory();
    const participantId = this.currentParticipantId();
    if (session && active) {
      this.storyService.getVotes(session.id, active.id, participantId || undefined).subscribe(resp => {
        if (resp) {
          this.votesRevealed.set(resp.revealed);
          if (resp.revealed) {
            if (resp.consensus?.median) {
              this.finalEstimateInput.set(resp.consensus.median);
            } else if (resp.consensus?.average) {
              this.finalEstimateInput.set(String(resp.consensus.average));
            }
          }
          const myVote = resp.votes?.find(v =>
            (participantId && v.participantId === participantId) ||
            (this.currentUserName() && v.participantName?.toLowerCase() === this.currentUserName().toLowerCase())
          );
          if (myVote && myVote.voteValue) {
            this.selectedVote.set(myVote.voteValue);
          }
        }
      });
    }
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
