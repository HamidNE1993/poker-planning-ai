import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { PlaceholderComponent } from './features/sessions/placeholder.component';
import { PlanningPokerComponent } from './features/planning-poker/planning-poker.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'sessions', component: PlaceholderComponent, data: { title: 'Sessions' } },
  { path: 'stories', component: PlaceholderComponent, data: { title: 'User Stories' } },
  { path: 'planning-poker', component: PlanningPokerComponent },
  { path: 'history', component: PlaceholderComponent, data: { title: 'Historique des estimations' } },
  { path: 'analytics', component: PlaceholderComponent, data: { title: 'Analytics' } },
  { path: 'settings', component: PlaceholderComponent, data: { title: 'Paramètres' } },
  { path: '**', redirectTo: '' }
];
