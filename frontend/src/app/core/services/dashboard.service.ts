import { Injectable, signal } from '@angular/core';
import { MetricCard } from '../models/metric.model';
import { AiInsight } from '../models/ai-insight.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly _metrics = signal<MetricCard[]>([
    {
      title: 'Sessions actives',
      value: '3',
      change: '+1 ce matin',
      trend: 'up',
      icon: 'pi-bolt',
      colorClass: 'purple',
      description: "Sessions en cours d'estimation"
    },
    {
      title: 'Stories estimées',
      value: '148',
      change: '+24 ce mois',
      trend: 'up',
      icon: 'pi-check-circle',
      colorClass: 'blue',
      description: "Stories validées par l'équipe"
    },
    {
      title: 'Accord moyen avec l\'IA',
      value: '84%',
      change: '+6% vs sprint précédent',
      trend: 'up',
      icon: 'pi-sparkles',
      colorClass: 'green',
      description: "Convergence des votes équipe & IA"
    },
    {
      title: 'Temps moyen / Story',
      value: '2.4 min',
      change: "-30s d'optimisation",
      trend: 'up',
      icon: 'pi-clock',
      colorClass: 'orange',
      description: "Grâce aux pré-analyses d'ambiguïté"
    }
  ]);

  private readonly _aiInsights = signal<AiInsight[]>([
    {
      storyTitle: 'AUTHENT-42: Authentification Biométrique WebAuthn',
      sessionName: 'Refonte Module Facturation',
      type: 'AMBIGUITY',
      title: "Critères d'acceptation incomplets détectés",
      message: "L'IA suggère de préciser la gestion du fallback OTP en cas d'échec matériel avant l'estimation finale.",
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

  readonly metrics = this._metrics.asReadonly();
  readonly aiInsights = this._aiInsights.asReadonly();
}
