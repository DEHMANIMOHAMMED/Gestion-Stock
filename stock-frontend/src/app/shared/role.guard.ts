import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, Role } from '../auth/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = (route.data?.['roles'] ?? []) as Role[];

  if (!auth.canAccess(roles)) {
    router.navigateByUrl('/forbidden');
    return false;
  }

  return true;
};
