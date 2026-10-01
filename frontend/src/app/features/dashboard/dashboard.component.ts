import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
  protected readonly sessions = [
    { name: 'Sprint 12 - Application mobile', meta: '8 user stories • il y a 2 jours', status: 'Terminée', letter: 'A' },
    { name: 'Sprint 11 - Refactorisation', meta: '5 user stories • il y a 1 semaine', status: 'En cours', letter: 'R' },
    { name: 'Sprint 10 - Nouvelles fonctionnalités', meta: '12 user stories • il y a 2 semaines', status: 'Terminée', letter: 'N' }
  ];
}
