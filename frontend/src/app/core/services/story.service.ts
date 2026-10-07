import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { CreateStoryDto, UpdateStoryDto, UserStory } from '../models/story.model';
import { FinalizeEstimateDto, StoryVotesResponse, SubmitVoteDto } from '../models/vote.model';

@Injectable({
  providedIn: 'root'
})
export class StoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/sessions';

  private readonly _stories = signal<UserStory[]>([]);
  private readonly _activeStory = signal<UserStory | null>(null);
  private readonly _currentVotes = signal<StoryVotesResponse | null>(null);

  readonly stories = this._stories.asReadonly();
  readonly activeStory = this._activeStory.asReadonly();
  readonly currentVotes = this._currentVotes.asReadonly();

  loadStories(sessionId: string): Observable<UserStory[]> {
    return this.http.get<UserStory[]>(`${this.baseUrl}/${sessionId}/stories`).pipe(
      tap(stories => {
        this._stories.set(stories || []);
        const active = (stories || []).find(s => s.status === 'VOTING') || (stories && stories.length > 0 ? stories[0] : null);
        this._activeStory.set(active);
      }),
      catchError(err => {
        console.error(`Erreur chargement des stories pour la session ${sessionId}:`, err);
        this._stories.set([]);
        this._activeStory.set(null);
        return of([]);
      })
    );
  }

  createStory(sessionId: string, dto: CreateStoryDto): Observable<UserStory> {
    return this.http.post<UserStory>(`${this.baseUrl}/${sessionId}/stories`, dto).pipe(
      tap(created => {
        this._stories.update(list => [...list, created]);
        if (!this._activeStory()) {
          this._activeStory.set(created);
        }
      })
    );
  }

  selectActiveStory(sessionId: string, storyId: string): Observable<UserStory | null> {
    return this.http.post<UserStory>(`${this.baseUrl}/${sessionId}/stories/${storyId}/select`, {}).pipe(
      tap(selected => {
        this._activeStory.set(selected);
        this._stories.update(list => list.map(s => {
          if (s.id === storyId) return { ...s, status: 'VOTING' };
          if (s.status === 'VOTING') return { ...s, status: 'PENDING' };
          return s;
        }));
      }),
      catchError(() => {
        const found = this._stories().find(s => s.id === storyId);
        if (found) {
          const updated = { ...found, status: 'VOTING' as const };
          this._activeStory.set(updated);
          this._stories.update(list => list.map(s => {
            if (s.id === storyId) return updated;
            if (s.status === 'VOTING') return { ...s, status: 'PENDING' as const };
            return s;
          }));
          return of(updated);
        }
        return of(null);
      })
    );
  }

  getVotes(sessionId: string, storyId: string, participantId?: string): Observable<StoryVotesResponse | null> {
    const params: { [key: string]: string } = {};
    if (participantId) {
      params['participantId'] = participantId;
    }
    return this.http.get<StoryVotesResponse>(`${this.baseUrl}/${sessionId}/stories/${storyId}/votes`, { params }).pipe(
      tap(res => this._currentVotes.set(res)),
      catchError(() => of(null))
    );
  }

  submitVote(sessionId: string, storyId: string, participantId: string, voteValue: string): Observable<StoryVotesResponse | null> {
    const dto: SubmitVoteDto = { participantId, voteValue };
    return this.http.post<StoryVotesResponse>(`${this.baseUrl}/${sessionId}/stories/${storyId}/votes`, dto).pipe(
      tap(res => this._currentVotes.set(res)),
      catchError(() => of(null))
    );
  }

  revealVotes(sessionId: string, storyId: string): Observable<StoryVotesResponse | null> {
    return this.http.post<StoryVotesResponse>(`${this.baseUrl}/${sessionId}/stories/${storyId}/reveal`, {}).pipe(
      tap(res => {
        this._currentVotes.set(res);
        this._stories.update(list => list.map(s => s.id === storyId ? { ...s, votesRevealed: true } : s));
        if (this._activeStory()?.id === storyId) {
          this._activeStory.update(s => s ? { ...s, votesRevealed: true } : null);
        }
      }),
      catchError(() => of(null))
    );
  }

  resetVotes(sessionId: string, storyId: string): Observable<StoryVotesResponse | null> {
    return this.http.post<StoryVotesResponse>(`${this.baseUrl}/${sessionId}/stories/${storyId}/reset-votes`, {}).pipe(
      tap(res => {
        this._currentVotes.set(res);
        this._stories.update(list => list.map(s => s.id === storyId ? { ...s, votesRevealed: false } : s));
        if (this._activeStory()?.id === storyId) {
          this._activeStory.update(s => s ? { ...s, votesRevealed: false } : null);
        }
      }),
      catchError(() => of(null))
    );
  }

  finalizeEstimate(sessionId: string, storyId: string, finalEstimate: string): Observable<UserStory | null> {
    const dto: FinalizeEstimateDto = { finalEstimate };
    return this.http.post<UserStory>(`${this.baseUrl}/${sessionId}/stories/${storyId}/finalize`, dto).pipe(
      tap(updated => {
        this._stories.update(list => list.map(s => s.id === storyId ? updated : s));
        if (this._activeStory()?.id === storyId) {
          this._activeStory.set(updated);
        }
      }),
      catchError(() => {
        const found = this._stories().find(s => s.id === storyId);
        if (found) {
          const updated: UserStory = { ...found, finalEstimate, status: 'ESTIMATED', votesRevealed: true };
          this._stories.update(list => list.map(s => s.id === storyId ? updated : s));
          if (this._activeStory()?.id === storyId) {
            this._activeStory.set(updated);
          }
          return of(updated);
        }
        return of(null);
      })
    );
  }

  applyStoryCreated(story: UserStory): void {
    this._stories.update(list => {
      if (list.some(s => s.id === story.id)) {
        return list;
      }
      return [...list, story];
    });
    if (!this._activeStory()) {
      this._activeStory.set(story);
    }
  }

  applyStoryUpdated(story: UserStory): void {
    this._stories.update(list => list.map(s => s.id === story.id ? story : s));
    if (this._activeStory()?.id === story.id) {
      this._activeStory.set(story);
    }
  }

  applyStoryDeleted(storyId: string): void {
    this._stories.update(list => list.filter(s => s.id !== storyId));
    if (this._activeStory()?.id === storyId) {
      const remaining = this._stories();
      this._activeStory.set(remaining.length > 0 ? remaining[0] : null);
    }
  }

  applyStorySelected(story: UserStory): void {
    this._stories.update(list => list.map(s => {
      if (s.id === story.id) {
        return story;
      }
      if (s.status === 'VOTING') {
        return { ...s, status: 'PENDING' };
      }
      return s;
    }));
    this._activeStory.set(story);
  }

  applyFinalizeEstimate(story: UserStory): void {
    this._stories.update(list => list.map(s => s.id === story.id ? story : s));
    if (this._activeStory()?.id === story.id) {
      this._activeStory.set(story);
    }
  }

  applyVotesUpdate(votes: StoryVotesResponse): void {
    this._currentVotes.set(votes);
    if (votes.storyId) {
      this._stories.update(list => list.map(s => s.id === votes.storyId ? { ...s, votesRevealed: votes.revealed } : s));
      if (this._activeStory()?.id === votes.storyId) {
        this._activeStory.update(s => s ? { ...s, votesRevealed: votes.revealed } : null);
      }
    }
  }
}
