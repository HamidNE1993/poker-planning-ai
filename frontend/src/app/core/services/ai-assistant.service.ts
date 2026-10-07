import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap, catchError } from 'rxjs';
import { StoryAnalysis, AnalyzeTextRequest } from '../models/ai-analysis.model';

@Injectable({
  providedIn: 'root'
})
export class AiAssistantService {
  private readonly http = inject(HttpClient);
  private readonly cache = new Map<string, StoryAnalysis>();

  private readonly _currentAnalysis = signal<StoryAnalysis | null>(null);
  readonly currentAnalysis = this._currentAnalysis.asReadonly();

  private readonly _isLoading = signal<boolean>(false);
  readonly isLoading = this._isLoading.asReadonly();

  private readonly _error = signal<string | null>(null);
  readonly error = this._error.asReadonly();

  getStoryAnalysis(storyId: string, forceRefresh: boolean = false): Observable<StoryAnalysis> {
    if (!forceRefresh && this.cache.has(storyId)) {
      const cached = this.cache.get(storyId)!;
      this._currentAnalysis.set(cached);
      this._error.set(null);
      return of(cached);
    }

    this._isLoading.set(true);
    this._error.set(null);

    return this.http.get<StoryAnalysis>(`/api/stories/${storyId}/analysis`).pipe(
      tap(analysis => {
        this.cache.set(storyId, analysis);
        this._currentAnalysis.set(analysis);
        this._isLoading.set(false);
      }),
      catchError(err => {
        this._isLoading.set(false);
        this._error.set('Impossible de charger l\'analyse IA pour cette story.');
        throw err;
      })
    );
  }

  analyzeText(request: AnalyzeTextRequest): Observable<StoryAnalysis> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.post<StoryAnalysis>('/api/ai/analyze', request).pipe(
      tap(analysis => {
        this._isLoading.set(false);
      }),
      catchError(err => {
        this._isLoading.set(false);
        this._error.set('Échec de l\'analyse du texte libre.');
        throw err;
      })
    );
  }

  clearCurrentAnalysis(): void {
    this._currentAnalysis.set(null);
    this._error.set(null);
  }
}
