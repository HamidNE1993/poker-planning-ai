import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html'
})
export class AppComponent {
  protected readonly dark = signal(false);

  protected toggleTheme(): void {
    this.dark.update(v => !v);
    document.documentElement.classList.toggle('app-dark', this.dark());
  }
}
