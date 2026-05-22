import { Routes } from '@angular/router';
import { authGuard } from './shared/auth.guard';
import { onboardingGuard } from './shared/onboarding.guard';
import { roleGuard } from './shared/role.guard';
import { planGuard } from './shared/plan.guard';

// Application routes. Lazy-load components to reduce the initial bundle size.
export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./auth/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./auth/register.component').then((m) => m.RegisterComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard, onboardingGuard],
    loadComponent: () => import('./dashboard/dashboard.component').then((m) => m.DashboardComponent)
  },
  {
    path: 'owner',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['OWNER'] },
    loadComponent: () => import('./owner/owner.component').then((m) => m.OwnerComponent)
  },
  {
    path: 'demo-accounts',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['OWNER'] },
    loadComponent: () => import('./demo/demo-accounts.component').then((m) => m.DemoAccountsComponent)
  },
  {
    path: 'products',
    canActivate: [authGuard, onboardingGuard],
    loadComponent: () => import('./products/products.component').then((m) => m.ProductsComponent)
  },
  {
    path: 'stock',
    canActivate: [authGuard, onboardingGuard],
    loadComponent: () => import('./stock/stock.component').then((m) => m.StockComponent)
  },
  {
    path: 'sales',
    canActivate: [authGuard, onboardingGuard],
    loadComponent: () => import('./sales/sales.component').then((m) => m.SalesComponent)
  },
  {
    path: 'procurement',
    canActivate: [authGuard, onboardingGuard, roleGuard, planGuard],
    data: { roles: ['ADMIN'], plans: ['PRO'] },
    loadComponent: () => import('./procurement/procurement.component').then((m) => m.ProcurementComponent)
  },
  {
    path: 'approvals',
    canActivate: [authGuard, onboardingGuard, roleGuard, planGuard],
    data: { roles: ['ADMIN'], plans: ['PRO'] },
    loadComponent: () => import('./procurement/approval-center.component').then((m) => m.ApprovalCenterComponent)
  },
  {
    path: 'notifications',
    canActivate: [authGuard, onboardingGuard, roleGuard, planGuard],
    data: { roles: ['ADMIN'], plans: ['PRO'] },
    loadComponent: () => import('./notifications/notification-center.component').then((m) => m.NotificationCenterComponent)
  },
  {
    path: 'security-audit',
    canActivate: [authGuard, onboardingGuard, roleGuard, planGuard],
    data: { roles: ['ADMIN'], plans: ['PRO'] },
    loadComponent: () => import('./admin/security-audit.component').then((m) => m.SecurityAuditComponent)
  },
  {
    path: 'system-health',
    canActivate: [authGuard, onboardingGuard, roleGuard, planGuard],
    data: { roles: ['ADMIN'], plans: ['PRO'] },
    loadComponent: () => import('./admin/system-health.component').then((m) => m.SystemHealthComponent)
  },
  {
    path: 'model-registry',
    canActivate: [authGuard, onboardingGuard, roleGuard, planGuard],
    data: { roles: ['ADMIN'], plans: ['PRO'] },
    loadComponent: () => import('./admin/model-registry.component').then((m) => m.ModelRegistryComponent)
  },
  {
    path: 'executive-timeline',
    canActivate: [authGuard, onboardingGuard, roleGuard, planGuard],
    data: { roles: ['ADMIN'], plans: ['PRO'] },
    loadComponent: () => import('./admin/executive-timeline.component').then((m) => m.ExecutiveTimelineComponent)
  },
  {
    path: 'daily-report',
    canActivate: [authGuard, onboardingGuard, roleGuard, planGuard],
    data: { roles: ['ADMIN'], plans: ['PRO'] },
    loadComponent: () => import('./admin/daily-report.component').then((m) => m.DailyReportComponent)
  },
  {
    path: 'users',
    canActivate: [authGuard, onboardingGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () => import('./users/organisation-users.component').then((m) => m.OrganisationUsersComponent)
  },
  {
    path: 'onboarding',
    canActivate: [authGuard],
    loadComponent: () => import('./auth/onboarding.component').then((m) => m.OnboardingComponent)
  },
  {
    path: 'organisation-settings',
    canActivate: [authGuard, onboardingGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () => import('./auth/onboarding.component').then((m) => m.OnboardingComponent)
  },
  {
    path: 'billing',
    canActivate: [authGuard, onboardingGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () => import('./billing/billing.component').then((m) => m.BillingComponent)
  },
  {
    path: 'support',
    canActivate: [authGuard, onboardingGuard],
    loadComponent: () => import('./support/support.component').then((m) => m.SupportComponent)
  },
  {
    path: 'suppliers/:id/360',
    canActivate: [authGuard, onboardingGuard, roleGuard, planGuard],
    data: { roles: ['ADMIN'], plans: ['PRO'] },
    loadComponent: () => import('./procurement/supplier-360.component').then((m) => m.Supplier360Component)
  },
  {
    path: 'forbidden',
    canActivate: [authGuard],
    loadComponent: () => import('./shared/forbidden.component').then((m) => m.ForbiddenComponent)
  },
  {
    path: 'prediction',
    canActivate: [authGuard, onboardingGuard, planGuard],
    data: { plans: ['PRO'] },
    loadComponent: () => import('./ai-reports/prediction.component').then((m) => m.PredictionComponent)
  },
  {
    path: 'ai-dashboard',
    canActivate: [authGuard, onboardingGuard, planGuard],
    data: { plans: ['PRO'] },
    loadComponent: () => import('./ai-reports/ai-dashboard.component').then((m) => m.AiDashboardComponent)
  },
  {
    path: 'stock-health',
    canActivate: [authGuard, onboardingGuard, planGuard],
    data: { plans: ['PRO'] },
    loadComponent: () => import('./ai-reports/stock-health.component').then((m) => m.StockHealthComponent)
  },
  {
    path: 'copilot',
    canActivate: [authGuard, onboardingGuard, planGuard],
    data: { plans: ['PRO'] },
    loadComponent: () => import('./ai-reports/copilot.component').then((m) => m.CopilotComponent)
  },
  {
    path: 'alerts',
    canActivate: [authGuard, onboardingGuard],
    loadComponent: () => import('./ai-reports/alerts.component').then((m) => m.AlertsComponent)
  },
  {
    path: 'reports',
    canActivate: [authGuard, onboardingGuard],
    loadComponent: () => import('./ai-reports/reports.component').then((m) => m.ReportsComponent)
  },
  { path: '**', redirectTo: '' }
];
