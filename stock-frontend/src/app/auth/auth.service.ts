import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { switchMap, tap } from 'rxjs';
import { environment } from '../environment';

export interface AuthResponse {
  token: string;
  userId: number;
  organisationId: number;
  role: string;
}

export interface MeResponse {
  userId: number;
  email: string;
  organisationId: number;
  organisationName: string;
  onboardingCompleted: boolean;
  role: string;
}

export interface OrganisationProfile {
  organisationId: number;
  name: string;
  industry: string | null;
  sizeRange: string | null;
  phone: string | null;
  address: string | null;
  city: string | null;
  country: string | null;
  currency: string | null;
  onboardingCompleted: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private tokenKey = 'jwt';
  private _user = signal<MeResponse | null>(null);
  readonly user = this._user.asReadonly();

  constructor(private http: HttpClient) {
    const token = this.getToken();
    if (token) {
      this.me().subscribe({ error: () => this.logout() });
    }
  }

  private storeToken(token: string) {
    localStorage.setItem(this.tokenKey, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isLoggedIn(): boolean {
    return this._user() !== null || this.getToken() !== null;
  }

  login(email: string, password: string) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, { email, password }).pipe(
      tap((res) => this.storeToken(res.token)),
      switchMap(() => this.me())
    );
  }

  register(organisationName: string, email: string, password: string) {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/register`, { organisationName, email, password })
      .pipe(
        tap((res) => this.storeToken(res.token)),
        switchMap(() => this.me())
      );
  }

  loginWithGoogle(idToken: string) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/google`, { idToken }).pipe(
      tap((res) => this.storeToken(res.token)),
      switchMap(() => this.me())
    );
  }

  me() {
    return this.http.get<MeResponse>(`${environment.apiUrl}/auth/me`).pipe(
      tap((me) => {
        this._user.set(me);
      })
    );
  }

  organisationProfile() {
    return this.http.get<OrganisationProfile>(`${environment.apiUrl}/auth/organisation-profile`);
  }

  updateOrganisationProfile(request: {
    name: string;
    industry: string;
    sizeRange: string;
    phone?: string | null;
    address?: string | null;
    city: string;
    country: string;
    currency: string;
  }) {
    return this.http.put<OrganisationProfile>(`${environment.apiUrl}/auth/organisation-profile`, request).pipe(
      switchMap(() => this.me())
    );
  }

  logout() {
    localStorage.removeItem(this.tokenKey);
    this._user.set(null);
  }
}
