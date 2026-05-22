import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { switchMap, tap } from 'rxjs';
import { environment } from '../environment';

export type Role = 'OWNER' | 'ADMIN' | 'USER';
export type PlanCode = 'TRIAL' | 'STARTER' | 'PRO';

export interface AuthResponse {
  token: string;
  userId: number;
  organisationId: number;
  role: Role;
}

export interface MeResponse {
  userId: number;
  email: string;
  organisationId: number;
  organisationName: string;
  onboardingCompleted: boolean;
  role: Role;
  planCode: PlanCode;
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
  logoUrl: string | null;
  taxId: string | null;
  website: string | null;
  stockAlertEmail: string | null;
  defaultLeadTimeDays: number | null;
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

  hasRole(role: Role): boolean {
    return this._user()?.role === role;
  }

  hasAnyRole(roles: Role[]): boolean {
    const role = this.currentRole();
    return !!role && roles.includes(role);
  }

  hasPlan(plan: PlanCode): boolean {
    return this._user()?.planCode === plan;
  }

  isProPlan(): boolean {
    const plan = this._user()?.planCode;
    return plan === 'PRO' || this.hasRole('OWNER');
  }

  canUseProFeatures(): boolean {
    return this.hasRole('OWNER') || this._user()?.planCode === 'PRO';
  }

  canAccess(requiredRoles: Role[]): boolean {
    return requiredRoles.length === 0 || this.hasAnyRole(requiredRoles);
  }

  currentRole(): Role | null {
    const userRole = this._user()?.role;
    if (userRole) {
      return userRole;
    }
    return this.roleFromToken();
  }

  landingPath(): string {
    const user = this._user();
    if (user?.role === 'OWNER') {
      return '/owner';
    }
    return user?.onboardingCompleted ? '/dashboard' : '/onboarding';
  }

  login(email: string, password: string) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, { email, password }).pipe(
      tap((res) => this.storeToken(res.token)),
      switchMap(() => this.me())
    );
  }

  register(organisationName: string, email: string, password: string, planCode: 'STARTER' | 'PRO') {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/register`, { organisationName, email, password, planCode })
      .pipe(
        tap((res) => this.storeToken(res.token)),
        switchMap(() => this.me())
      );
  }

  loginWithGoogle(idToken: string, planCode?: 'STARTER' | 'PRO') {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/google`, { idToken, planCode }).pipe(
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
    logoUrl?: string | null;
    taxId?: string | null;
    website?: string | null;
    stockAlertEmail?: string | null;
    defaultLeadTimeDays?: number | null;
  }) {
    return this.http.put<OrganisationProfile>(`${environment.apiUrl}/auth/organisation-profile`, request).pipe(
      switchMap(() => this.me())
    );
  }

  changePassword(currentPassword: string, newPassword: string) {
    return this.http.post<void>(`${environment.apiUrl}/auth/change-password`, { currentPassword, newPassword });
  }

  logout() {
    localStorage.removeItem(this.tokenKey);
    this._user.set(null);
  }

  private roleFromToken(): Role | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }
    try {
      const payload = token.split('.')[1];
      const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/');
      const decoded = JSON.parse(atob(normalizedPayload));
      return decoded.role ?? null;
    } catch {
      return null;
    }
  }
}
