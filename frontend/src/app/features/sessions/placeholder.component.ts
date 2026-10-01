import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-placeholder',
  standalone: true,
  template: `
    <div class="placeholder-container">
      <div class="page-heading">
        <span class="eyebrow">Module en cours</span>
        <h1>{{ title }}</h1>
        <p>Cet écran est prêt pour le développement de la logique métier et l'intégration API.</p>
      </div>

      <div class="empty-panel">
        <i class="pi pi-sparkles"></i>
        <h2>{{ title }}</h2>
        <p>Le design system et les services d'architecture sont en place.</p>
      </div>
    </div>
  `,
  styles: [`
    .placeholder-container {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }
    .page-heading {
      .eyebrow {
        font-size: 0.8rem;
        font-weight: 700;
        color: var(--primary);
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }
      h1 {
        margin: 0.25rem 0 0.5rem;
        font-size: 1.75rem;
        font-weight: 700;
      }
      p {
        color: var(--muted);
        margin: 0;
      }
    }
    .empty-panel {
      background: var(--surface);
      border: 1px dashed var(--border);
      border-radius: 1rem;
      padding: 4rem 2rem;
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 1rem;

      i {
        font-size: 2.5rem;
        color: var(--primary);
      }
      h2 {
        margin: 0;
        font-size: 1.25rem;
      }
      p {
        color: var(--muted);
        margin: 0;
        max-width: 420px;
      }
    }
  `]
})
export class PlaceholderComponent {
  private readonly route = inject(ActivatedRoute);
  protected readonly title = (this.route.snapshot.data['title'] as string) || 'Poker Planning AI';
}
