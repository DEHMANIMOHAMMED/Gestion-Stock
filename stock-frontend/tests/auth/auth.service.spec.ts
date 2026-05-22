import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService, AuthResponse, MeResponse } from '../../src/app/auth/auth.service';
import { environment } from '../../src/app/environment';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  const authResponse: AuthResponse = {
    token: 'jwt-token',
    userId: 7,
    organisationId: 3,
    role: 'ADMIN'
  };

  const meResponse: MeResponse = {
    userId: 7,
    email: 'admin@example.com',
    organisationId: 3,
    organisationName: 'Demo Org',
    onboardingCompleted: true,
    role: 'ADMIN',
    planCode: 'PRO'
  };

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });

    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('stores the token and loads /auth/me after login', () => {
    let user: MeResponse | undefined;

    service.login('admin@example.com', 'Password123!').subscribe((response) => {
      user = response;
    });

    const loginRequest = http.expectOne(`${environment.apiUrl}/auth/login`);
    expect(loginRequest.request.method).toBe('POST');
    expect(loginRequest.request.body).toEqual({
      email: 'admin@example.com',
      password: 'Password123!'
    });
    loginRequest.flush(authResponse);

    const meRequest = http.expectOne(`${environment.apiUrl}/auth/me`);
    expect(meRequest.request.method).toBe('GET');
    meRequest.flush(meResponse);

    expect(localStorage.getItem('jwt')).toBe('jwt-token');
    expect(user).toEqual(meResponse);
    expect(service.user()).toEqual(meResponse);
    expect(service.isLoggedIn()).toBeTrue();
  });

  it('stores the token and loads /auth/me after registration', () => {
    service.register('Demo Org', 'admin@example.com', 'Password123!', 'STARTER').subscribe();

    const registerRequest = http.expectOne(`${environment.apiUrl}/auth/register`);
    expect(registerRequest.request.method).toBe('POST');
    expect(registerRequest.request.body).toEqual({
      organisationName: 'Demo Org',
      email: 'admin@example.com',
      password: 'Password123!',
      planCode: 'STARTER'
    });
    registerRequest.flush(authResponse);

    const meRequest = http.expectOne(`${environment.apiUrl}/auth/me`);
    meRequest.flush(meResponse);

    expect(localStorage.getItem('jwt')).toBe('jwt-token');
    expect(service.user()).toEqual(meResponse);
  });

  it('stores the token and loads /auth/me after Google login', () => {
    service.loginWithGoogle('google-id-token').subscribe();

    const googleRequest = http.expectOne(`${environment.apiUrl}/auth/google`);
    expect(googleRequest.request.method).toBe('POST');
    expect(googleRequest.request.body).toEqual(jasmine.objectContaining({
      idToken: 'google-id-token'
    }));
    googleRequest.flush(authResponse);

    const meRequest = http.expectOne(`${environment.apiUrl}/auth/me`);
    meRequest.flush(meResponse);

    expect(localStorage.getItem('jwt')).toBe('jwt-token');
    expect(service.user()).toEqual(meResponse);
  });

  it('exposes role helpers and sends owners to the owner console', () => {
    service.login('owner@stockpilot.local', 'Owner@2026!').subscribe();

    const loginRequest = http.expectOne(`${environment.apiUrl}/auth/login`);
    loginRequest.flush({ ...authResponse, role: 'OWNER' });

    const meRequest = http.expectOne(`${environment.apiUrl}/auth/me`);
    meRequest.flush({
      ...meResponse,
      email: 'owner@stockpilot.local',
      role: 'OWNER',
      planCode: 'PRO',
      onboardingCompleted: true
    });

    expect(service.hasRole('OWNER')).toBeTrue();
    expect(service.hasAnyRole(['ADMIN', 'OWNER'])).toBeTrue();
    expect(service.canAccess(['OWNER'])).toBeTrue();
    expect(service.canUseProFeatures()).toBeTrue();
    expect(service.landingPath()).toBe('/owner');
  });
});
