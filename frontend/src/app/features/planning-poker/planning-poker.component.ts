import { Component, signal } from '@angular/core';

@Component({
  selector: 'app-planning-poker',
  standalone: true,
  templateUrl: './planning-poker.component.html'
})
export class PlanningPokerComponent {
  protected readonly cards = ['?', '0', '1', '2', '3', '5', '8', '13', '21', '34', '55', '∞'];
  protected readonly selected = signal<string | null>(null);
}
