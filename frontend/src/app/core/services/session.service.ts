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

  private readonly _sessions = signal<SessionSummary[]>([]);
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
      catchError(err => {
        console.warn('Backend non disponible ou erreur réseau pour les sessions:', err);
        return of([] as SessionSummary[]);
      })
    ).subscribe(backendSessions => {
      const enriched = (backendSessions || []).map(s => this.enrichSummary(s));
      this._sessions.set(enriched);
    });
  }

  loadSession(sessionId: string): Observable<PlanningSession | null> {
    return this.http.get<PlanningSession>(`${this.baseUrl}/${sessionId}`).pipe(
      tap(session => {
        this._currentSession.set(session);
        this._participants.set(session.participants || []);
      }),
      catchError(err => {
        console.error(`Impossible de charger la session ${sessionId} depuis le backend:`, err);
        this._currentSession.set(null);
        this._participants.set([]);
        return of(null);
      })
    );
  }

  createSession(dto: CreateSessionDto): Observable<PlanningSession> {
    return this.http.post<PlanningSession>(this.baseUrl, dto).pipe(
      tap(created => {
        const summary = this.enrichSummary({
          ...created,
          participantCount: created.participants?.length || 1
        });
        this._sessions.update(list => [summary, ...list]);
        this._currentSession.set(created);
        this._participants.set(created.participants || []);
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

  applySessionUpdate(session: PlanningSession): void {
    if (this._currentSession()?.id === session.id) {
      this._currentSession.set(session);
      if (session.participants) {
        this._participants.set(session.participants);
      }
    }
    this._sessions.update(list => list.map(s => s.id === session.id ? this.enrichSummary({ ...s, ...session }) : s));
  }

  applyParticipantUpdate(participant: Participant): void {
    this._participants.update(list => {
      const idx = list.findIndex(p => p.id === participant.id || p.name.toLowerCase() === participant.name.toLowerCase());
      if (idx !== -1) {
        const copy = [...list];
        copy[idx] = { ...copy[idx], ...participant };
        return copy;
      }
      return [...list, participant];
    });
  }

  applyParticipantLeft(participant: Participant): void {
    this._participants.update(list =>
      list.map(p => (p.id === participant.id || p.name.toLowerCase() === participant.name.toLowerCase()) ? { ...p, online: false } : p)
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
