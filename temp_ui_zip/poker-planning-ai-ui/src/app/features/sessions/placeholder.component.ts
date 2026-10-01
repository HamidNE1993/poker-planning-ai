import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-placeholder',
  standalone: true,
  template: `<div class="page-heading"><div><span class="eyebrow">POKER PLANNING AI</span><h1>{{title}}</h1><p>Écran prêt à être branché au backend.</p></div></div>
  <div class="panel empty"><i class="pi pi-sparkles"></i><h2>{{title}}</h2><p>Le design system est installé. Implémente maintenant le cas d'usage métier de cet écran.</p></div>`
})
export class PlaceholderComponent {
  readonly title = this.route.snapshot.data['title'] as string;
  constructor(private readonly route: ActivatedRoute) {}
}
