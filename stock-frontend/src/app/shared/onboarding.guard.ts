import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../auth/auth.service';

export const onboardingGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.user();

  if (user && !user.onboardingCompleted) {
    router.navigateByUrl('/onboarding');
    return false;
  }

  if (!user && auth.getToken()) {
    return auth.me().pipe(
      map((loadedUser) => {
        if (!loadedUser.onboardingCompleted) {
          router.navigateByUrl('/onboarding');
          return false;
        }
        return true;
      }),
      catchError(() => {
        auth.logout();
        router.navigateByUrl('/login');
        return of(false);
      })
    );
  }

  return true;
};
