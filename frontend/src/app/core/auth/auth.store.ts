import { Injectable, signal, computed } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly _token = signal<string | null>(
    typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('bearer_token') : null
  );

  readonly token = this._token.asReadonly();
  readonly isAuthenticated = computed(() => this._token() !== null);

  setToken(token: string): void {
    sessionStorage.setItem('bearer_token', token);
    this._token.set(token);
  }

  clearToken(): void {
    sessionStorage.removeItem('bearer_token');
    this._token.set(null);
  }
}
