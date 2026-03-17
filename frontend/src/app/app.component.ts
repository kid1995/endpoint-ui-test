import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthStore } from './core/auth/auth.store';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, FormsModule],
  template: `
    <header class="app-header">
      <h1>Service Visualizer</h1>
      <div class="auth-section">
        @if (authStore.isAuthenticated()) {
          <span class="badge badge-success">Authenticated</span>
          <button class="btn btn-danger" (click)="authStore.clearToken()">Logout</button>
        } @else {
          <input
            type="password"
            placeholder="Bearer Token"
            [ngModel]="tokenInput"
            (ngModelChange)="tokenInput = $event"
            (keyup.enter)="login()"
          />
          <button class="btn btn-primary" (click)="login()">Login</button>
        }
      </div>
    </header>
    <router-outlet />
  `,
  styles: [`
    .app-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 24px;
      background: var(--bg-secondary);
      border-bottom: 1px solid var(--border-color);
    }
    h1 { font-size: 18px; font-weight: 600; }
    .auth-section {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .auth-section input { width: 280px; }
  `],
})
export class AppComponent {
  readonly authStore = inject(AuthStore);
  tokenInput = '';

  login(): void {
    if (this.tokenInput.trim()) {
      this.authStore.setToken(this.tokenInput.trim());
      this.tokenInput = '';
    }
  }
}
