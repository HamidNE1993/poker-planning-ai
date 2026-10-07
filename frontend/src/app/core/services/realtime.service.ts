import { Injectable, inject, signal, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, of } from 'rxjs';
import { SessionEvent, SessionEventType, TimerSyncPayload } from '../models/realtime.model';

@Injectable({
  providedIn: 'root'
})
export class RealtimeService {
  private readonly http = inject(HttpClient);
  private readonly zone = inject(NgZone);
  private eventSource: EventSource | null = null;
  private currentSessionId: string | null = null;
  private reconnectTimeoutId: any = null;

  private readonly _isConnected = signal<boolean>(false);
  private readonly _lastEvent = signal<SessionEvent | null>(null);
  private readonly eventsSubject = new Subject<SessionEvent>();

  readonly isConnected = this._isConnected.asReadonly();
  readonly lastEvent = this._lastEvent.asReadonly();
  readonly events$ = this.eventsSubject.asObservable();

  connect(sessionId: string): Observable<SessionEvent> {
    if (this.currentSessionId === sessionId && this.eventSource && this.eventSource.readyState === EventSource.OPEN) {
      return this.events$;
    }

    this.disconnect();
    this.currentSessionId = sessionId;
    this.initEventSource(sessionId);

    return this.events$;
  }

  disconnect(): void {
    if (this.reconnectTimeoutId) {
      clearTimeout(this.reconnectTimeoutId);
      this.reconnectTimeoutId = null;
    }

    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }

    this.currentSessionId = null;
    this._isConnected.set(false);
  }

  syncTimer(sessionId: string, action: 'START' | 'PAUSE' | 'RESET', remainingSeconds: number): Observable<void> {
    const payload: TimerSyncPayload = { action, remainingSeconds };
    return this.http.post<void>(`/api/sessions/${sessionId}/timer`, payload);
  }

  private initEventSource(sessionId: string): void {
    const url = `/api/sessions/${sessionId}/events`;
    this.eventSource = new EventSource(url);

    this.eventSource.onopen = () => {
      this.zone.run(() => {
        this._isConnected.set(true);
      });
    };

    this.eventSource.onerror = (error) => {
      console.warn(`[RealtimeService] Connexion SSE interrompue pour la session ${sessionId}.`, error);
      this.zone.run(() => {
        this._isConnected.set(false);
      });
      this.eventSource?.close();
      this.eventSource = null;

      // Reconnexion automatique après 3 secondes si la session est toujours active
      if (this.currentSessionId === sessionId) {
        this.reconnectTimeoutId = setTimeout(() => {
          if (this.currentSessionId === sessionId) {
            this.initEventSource(sessionId);
          }
        }, 3000);
      }
    };

    // Écoute de tous les types d'événements
    const eventTypes: SessionEventType[] = [
      'SESSION_UPDATED',
      'PARTICIPANT_JOINED',
      'PARTICIPANT_UPDATED',
      'PARTICIPANT_LEFT',
      'STORY_CREATED',
      'STORY_UPDATED',
      'STORY_DELETED',
      'STORY_SELECTED',
      'VOTE_SUBMITTED',
      'VOTES_REVEALED',
      'VOTES_RESET',
      'ESTIMATE_FINALIZED',
      'TIMER_SYNC',
      'HEARTBEAT'
    ];

    eventTypes.forEach(eventType => {
      this.eventSource?.addEventListener(eventType, (event: MessageEvent) => {
        try {
          const data: SessionEvent = JSON.parse(event.data);
          this.zone.run(() => {
            this._lastEvent.set(data);
            this.eventsSubject.next(data);
          });
        } catch (e) {
          console.error(`[RealtimeService] Erreur lors du parsing de l'événement SSE ${eventType}:`, e);
        }
      });
    });

    this.eventSource.onmessage = (event: MessageEvent) => {
      try {
        const data: SessionEvent = JSON.parse(event.data);
        this.zone.run(() => {
          this._lastEvent.set(data);
          this.eventsSubject.next(data);
        });
      } catch (e) {
        // Ping textuel simple ou format brut
      }
    };
  }
}
