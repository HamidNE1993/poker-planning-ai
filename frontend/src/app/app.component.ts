import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
@Component({selector:'app-root',standalone:true,template:`<main><h1>Poker Planning AI</h1><p>Java 25 · Spring Boot 4.1 · Angular 22</p><button (click)="check()">Check backend</button><pre>{{ result() }}</pre></main>`,styles:[`main{font-family:system-ui;max-width:900px;margin:4rem auto;padding:0 1rem}button{padding:.7rem 1rem}pre{margin-top:1rem}`]})
export class AppComponent {
  private readonly http = inject(HttpClient); readonly result = signal('');
  check(): void { this.http.get('/api/health').subscribe({next:v=>this.result.set(JSON.stringify(v,null,2)),error:e=>this.result.set(`Backend unavailable: ${e.message}`)}); }
}
