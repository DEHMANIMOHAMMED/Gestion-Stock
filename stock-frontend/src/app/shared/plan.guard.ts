import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, PlanCode } from '../auth/auth.service';
import { catchError, map, of } from 'rxjs';

export const planGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const requiredPlans = (route.data?.['plans'] ?? []) as PlanCode[];

  if (!requiredPlans.length || auth.hasRole('OWNER')) {
    return true;
  }

  const canAccessPlan = (plan?: PlanCode) => !!plan && requiredPlans.includes(plan);

  if (canAccessPlan(auth.user()?.planCode)) {
    return true;
  }

  if (!auth.getToken()) {
    return router.createUrlTree(['/login']);
  }

  return auth.me().pipe(
    map((user) => {
      if (user.role === 'OWNER' || canAccessPlan(user.planCode)) {
        return true;
      }
      return router.createUrlTree(['/billing']);
    }),
    catchError(() => of(router.createUrlTree(['/login'])))
  );
};
