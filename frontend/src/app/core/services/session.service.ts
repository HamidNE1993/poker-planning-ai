import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import {
  CreateSessionDto,
  PlanningSession,
  SessionStatus,
  SessionSummary,
  TagSeverity,
  UpdateSessionConfigDto,
  UpdateSessionStatusDto
} from '../models/session.model';
import {
  JoinSessionDto,
  Participant,
  ParticipantRole,
  UpdateParticipantRoleDto
} from '../models/participant.model';

@Injectable({
  providedIn: 'root'
})
export class SessionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/sessions';

  private readonly _sessions = signal<SessionSummary[]>([
    {
      id: 'sess-101',
      name: 'Refonte Module Facturation & Stripe',
      sprint: 'Sprint 12 - Core',
      status: 'IN_PROGRESS',
      statusLabel: 'En cours',
      severity: 'success',
      deckType: 'FIBONACCI',
      inviteCode: 'FACT12',
      autoReveal: false,
      timerDurationSeconds: 60,
      participantCount: 4,
      storyCount: 8,
      completedStories: 5,
      lastActivity: 'Il y a 10 min',
      aiAgreementRate: 88,
      participants: [
        { name: 'Sarah M.', role: 'FACILITATOR', online: true },
        { name: 'Alex K.', role: 'VOTER', online: true },
        { name: 'Thomas D.', role: 'VOTER', online: true },
        { name: 'Elena V.', role: 'OBSERVER', online: false }
      ],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    },
    {
      id: 'sess-102',
      name: 'Optimisation Performances API REST',
      sprint: 'Sprint 12 - Backend',
      status: 'COMPLETED',
      statusLabel: 'Terminée',
      severity: 'info',
      deckType: 'FIBONACCI',
      inviteCode: 'PERF99',
      autoReveal: true,
      timerDurationSeconds: 45,
      participantCount: 3,
      storyCount: 6,
      completedStories: 6,
      lastActivity: 'Il y a 2 heures',
      aiAgreementRate: 92,
      participants: [
        { name: 'Sarah M.', role: 'FACILITATOR', online: false },
        { name: 'Thomas D.', role: 'VOTER', online: false },
        { name: 'Lucas B.', role: 'VOTER', online: false }
      ],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    },
    {
      id: 'sess-103',
      name: 'Design System & Accessibilité Mobile',
      sprint: 'Sprint 13 - Frontend',
      status: 'CREATED',
      statusLabel: 'Planifiée',
      severity: 'secondary',
      deckType: 'FIBONACCI',
      inviteCode: 'DS1300',
      autoReveal: false,
      timerDurationSeconds: 60,
      participantCount: 2,
      storyCount: 12,
      completedStories: 0,
      lastActivity: 'Créée hier',
      aiAgreementRate: 0,
      participants: [
        { name: 'Elena V.', role: 'FACILITATOR', online: true },
        { name: 'Alex K.', role: 'VOTER', online: true }
      ],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
  ]);

  private readonly _currentSession = signal<PlanningSession | null>(null);
  private readonly _participants = signal<Participant[]>([]);

  readonly sessions = this._sessions.asReadonly();
  readonly currentSession = this._currentSession.asReadonly();
  readonly participants = this._participants.asReadonly();

  constructor() {
    this.refreshSessions();
  }

  refreshSessions(): void {
    this.http.get<SessionSummary[]>(this.baseUrl).pipe(
      catchError(() => of(null))
    ).subscribe(backendSessions => {
      if (backendSessions && backendSessions.length > 0) {
        const enriched = backendSessions.map(s => this.enrichSummary(s));
        this._sessions.set(enriched);
      }
    });
  }

  loadSession(sessionId: string): Observable<PlanningSession | null> {
    return this.http.get<PlanningSession>(`${this.baseUrl}/${sessionId}`).pipe(
      tap(session => {
        this._currentSession.set(session);
        this._participants.set(session.participants || []);
      }),
      catchError(() => {
        // Fallback local mock if not found in backend
        const summary = this._sessions().find(s => s.id === sessionId);
        if (summary) {
          const fallbackSession: PlanningSession = {
            id: summary.id,
            name: summary.name,
            sprint: summary.sprint,
            status: summary.status,
            deckType: summary.deckType,
            deckValues: ['?', '0', '1', '2', '3', '5', '8', '13', '21', '34', '55', '89', '☕'],
            inviteCode: summary.inviteCode,
            autoReveal: summary.autoReveal,
            timerDurationSeconds: summary.timerDurationSeconds,
            participants: summary.participants || [],
            createdAt: summary.createdAt,
            updatedAt: summary.updatedAt
          };
          this._currentSession.set(fallbackSession);
          this._participants.set(fallbackSession.participants);
          return of(fallbackSession);
        }
        return of(null);
      })
    );
  }

  createSession(dto: CreateSessionDto): Observable<PlanningSession> {
    return this.http.post<PlanningSession>(this.baseUrl, dto).pipe(
      tap(created => {
        const summary = this.enrichSummary({
          ...created,
          participantCount: created.participants.length
        });
        this._sessions.update(list => [summary, ...list]);
      }),
      catchError(() => {
        // Fallback local creation
        const newSession: PlanningSession = {
          id: `sess-${Date.now().toString().slice(-4)}`,
          name: dto.name.trim(),
          sprint: dto.sprint.trim() || 'Sprint 1',
          status: 'CREATED',
          deckType: dto.deckType || 'FIBONACCI',
          deckValues: ['?', '0', '1', '2', '3', '5', '8', '13', '21', '34', '55', '89', '☕'],
          inviteCode: Math.random().toString(36).substring(2, 8).toUpperCase(),
          autoReveal: dto.autoReveal ?? false,
          timerDurationSeconds: dto.timerDurationSeconds ?? 60,
          participants: [
            {
              name: dto.facilitatorName || 'Vous',
              role: 'FACILITATOR',
              online: true,
              joinedAt: new Date().toISOString()
            }
          ],
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        };

        const summary = this.enrichSummary({
          ...newSession,
          participantCount: 1
        });

        this._sessions.update(list => [summary, ...list]);
        return of(newSession);
      })
    );
  }

  joinSession(sessionId: string, dto: JoinSessionDto): Observable<Participant> {
    return this.http.post<Participant>(`${this.baseUrl}/${sessionId}/participants`, dto).pipe(
      tap(newParticipant => {
        this._participants.update(list => {
          const exists = list.some(p => p.name.toLowerCase() === newParticipant.name.toLowerCase());
          if (exists) {
            return list.map(p => p.name.toLowerCase() === newParticipant.name.toLowerCase() ? newParticipant : p);
          }
          return [...list, newParticipant];
        });
      }),
      catchError(() => {
        const participant: Participant = {
          id: `part-${Date.now().toString().slice(-4)}`,
          name: dto.name,
          avatar: dto.avatar,
          role: dto.role || 'VOTER',
          online: true,
          joinedAt: new Date().toISOString()
        };
        this._participants.update(list => [...list, participant]);
        return of(participant);
      })
    );
  }

  updateParticipantRole(sessionId: string, participantId: string, role: ParticipantRole): Observable<Participant | null> {
    const dto: UpdateParticipantRoleDto = { role };
    return this.http.patch<Participant>(`${this.baseUrl}/${sessionId}/participants/${participantId}/role`, dto).pipe(
      tap(updated => {
        this._participants.update(list => list.map(p => p.id === participantId ? { ...p, role: updated.role } : p));
      }),
      catchError(() => {
        this._participants.update(list => list.map(p => p.id === participantId ? { ...p, role } : p));
        return of(null);
      })
    );
  }

  updateSessionStatus(sessionId: string, status: SessionStatus): Observable<PlanningSession | null> {
    const dto: UpdateSessionStatusDto = { status };
    return this.http.patch<PlanningSession>(`${this.baseUrl}/${sessionId}/status`, dto).pipe(
      tap(updated => {
        this._currentSession.set(updated);
        this._sessions.update(list => list.map(s => s.id === sessionId ? this.enrichSummary({ ...s, status: updated.status }) : s));
      }),
      catchError(() => {
        this._sessions.update(list => list.map(s => s.id === sessionId ? this.enrichSummary({ ...s, status }) : s));
        return of(null);
      })
    );
  }

  private enrichSummary(summary: Partial<SessionSummary> & { id: string; name: string; sprint: string; status: SessionStatus }): SessionSummary {
    const statusLabel = summary.status === 'IN_PROGRESS' ? 'En cours' : summary.status === 'COMPLETED' ? 'Terminée' : 'Planifiée';
    const severity: TagSeverity = summary.status === 'IN_PROGRESS' ? 'success' : summary.status === 'COMPLETED' ? 'info' : 'secondary';

    return {
      id: summary.id,
      name: summary.name,
      sprint: summary.sprint || 'Sprint 1',
      status: summary.status,
      statusLabel,
      severity,
      deckType: summary.deckType || 'FIBONACCI',
      inviteCode: summary.inviteCode || 'CODE',
      autoReveal: summary.autoReveal ?? false,
      timerDurationSeconds: summary.timerDurationSeconds ?? 60,
      participantCount: summary.participantCount ?? (summary.participants?.length || 1),
      storyCount: summary.storyCount ?? 0,
      completedStories: summary.completedStories ?? 0,
      lastActivity: summary.lastActivity || 'Récemment',
      aiAgreementRate: summary.aiAgreementRate ?? 0,
      participants: summary.participants || [],
      createdAt: summary.createdAt || new Date().toISOString(),
      updatedAt: summary.updatedAt || new Date().toISOString()
    };
  }
}
