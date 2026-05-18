import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './onboarding.component.html',
  styleUrls: ['./onboarding.component.scss']
})
export class OnboardingComponent implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    industry: ['', [Validators.required, Validators.maxLength(80)]],
    sizeRange: ['', [Validators.required, Validators.maxLength(40)]],
    phone: ['', [Validators.maxLength(60)]],
    address: ['', [Validators.maxLength(220)]],
    city: ['', [Validators.required, Validators.maxLength(120)]],
    country: ['', [Validators.required, Validators.maxLength(120)]],
    currency: ['EUR', [Validators.required, Validators.maxLength(10)]]
  });

  message = '';
  loading = true;
  saving = false;

  ngOnInit(): void {
    this.auth.organisationProfile().subscribe({
      next: (profile) => {
        this.form.patchValue({
          name: profile.name,
          industry: profile.industry || '',
          sizeRange: profile.sizeRange || '',
          phone: profile.phone || '',
          address: profile.address || '',
          city: profile.city || '',
          country: profile.country || '',
          currency: profile.currency || 'EUR'
        });
        this.loading = false;
      },
      error: () => {
        this.message = 'Impossible de charger le profil organisation.';
        this.loading = false;
      }
    });
  }

  submit(): void {
    this.message = '';
    this.form.markAllAsTouched();
    if (this.form.invalid || this.saving) {
      return;
    }

    this.saving = true;
    const value = this.form.getRawValue();
    this.auth.updateOrganisationProfile({
      name: value.name!,
      industry: value.industry!,
      sizeRange: value.sizeRange!,
      phone: value.phone || null,
      address: value.address || null,
      city: value.city!,
      country: value.country!,
      currency: value.currency!
    }).subscribe({
      next: () => {
        this.saving = false;
        this.router.navigateByUrl('/dashboard');
      },
      error: (error) => {
        this.saving = false;
        this.message = error?.error?.detail || 'Enregistrement du profil impossible.';
      }
    });
  }
}
