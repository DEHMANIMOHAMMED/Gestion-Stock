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
    role: 'ADMIN'
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
    service.register('Demo Org', 'admin@example.com', 'Password123!').subscribe();

    const registerRequest = http.expectOne(`${environment.apiUrl}/auth/register`);
    expect(registerRequest.request.method).toBe('POST');
    expect(registerRequest.request.body).toEqual({
      organisationName: 'Demo Org',
      email: 'admin@example.com',
      password: 'Password123!'
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
    expect(googleRequest.request.body).toEqual({
      idToken: 'google-id-token'
    });
    googleRequest.flush(authResponse);

    const meRequest = http.expectOne(`${environment.apiUrl}/auth/me`);
    meRequest.flush(meResponse);

    expect(localStorage.getItem('jwt')).toBe('jwt-token');
    expect(service.user()).toEqual(meResponse);
  });
});
