import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  readonly isDark = signal<boolean>(false);

  toggleTheme(): void {
    this.isDark.update(dark => !dark);
    document.documentElement.classList.toggle('app-dark', this.isDark());
  }

  setDarkTheme(dark: boolean): void {
    this.isDark.set(dark);
    document.documentElement.classList.toggle('app-dark', dark);
  }
}
