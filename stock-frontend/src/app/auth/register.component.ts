import { AfterViewInit, Component, NgZone, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from './auth.service';
import { GoogleIdentityService } from './google-identity.service';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private googleIdentity = inject(GoogleIdentityService);
  private ngZone = inject(NgZone);
  private router = inject(Router);

  form = this.fb.group({
    organisationName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(180)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]]
  });

  message = '';
  loading = false;
  googleLoading = false;

  ngAfterViewInit(): void {
    this.googleIdentity.renderButton('google-register-button', 'signup_with', (idToken) => {
      this.ngZone.run(() => this.registerWithGoogle(idToken));
    }).catch(() => {
      this.ngZone.run(() => {
        this.message = 'Inscription Google indisponible pour le moment';
      });
    });
  }

  ngOnDestroy(): void {
    this.googleIdentity.cancel();
  }

  register() {
    this.message = '';
    this.form.markAllAsTouched();
    if (this.form.invalid || this.loading) {
      return;
    }

    const { organisationName, email, password } = this.form.value;
    this.loading = true;
    this.auth.register(organisationName!, email!, password!).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigateByUrl('/onboarding');
      },
      error: (err) => {
        this.loading = false;
        this.message = this.readError(err, "Echec de l'inscription");
      }
    });
  }

  fieldInvalid(field: 'organisationName' | 'email' | 'password'): boolean {
    const control = this.form.controls[field];
    return control.invalid && (control.dirty || control.touched);
  }

  private registerWithGoogle(idToken: string): void {
    if (this.googleLoading) {
      return;
    }

    this.message = '';
    this.googleLoading = true;
    this.auth.loginWithGoogle(idToken).subscribe({
      next: () => {
        this.googleLoading = false;
        this.router.navigateByUrl(this.auth.user()?.onboardingCompleted ? '/dashboard' : '/onboarding');
      },
      error: (err) => {
        this.googleLoading = false;
        this.message = this.readError(err, "Echec de l'inscription Google");
      }
    });
  }

  private readError(err: unknown, fallback: string): string {
    const rawError = (err as { error?: unknown })?.error;
    const error = typeof rawError === 'string' ? this.parseError(rawError) : rawError as {
      detail?: string;
      message?: string;
      errors?: Record<string, string>;
    };
    if (error?.detail) {
      return error.detail;
    }
    if (error?.message) {
      return error.message;
    }
    if (error?.errors) {
      return Object.values(error.errors).join(' ');
    }
    return fallback;
  }

  private parseError(rawError: string): { detail?: string; message?: string; errors?: Record<string, string> } {
    try {
      return JSON.parse(rawError);
    } catch {
      return { message: rawError };
    }
  }
}
