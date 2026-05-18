import { Routes } from '@angular/router';
import { authGuard } from './shared/auth.guard';
import { adminGuard } from './shared/admin.guard';
import { onboardingGuard } from './shared/onboarding.guard';

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
    path: 'demo-accounts',
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
    path: 'procurement',
    canActivate: [authGuard, onboardingGuard],
    loadComponent: () => import('./procurement/procurement.component').then((m) => m.ProcurementComponent)
  },
  {
    path: 'approvals',
    canActivate: [authGuard, onboardingGuard],
    loadComponent: () => import('./procurement/approval-center.component').then((m) => m.ApprovalCenterComponent)
  },
  {
    path: 'notifications',
    canActivate: [authGuard, onboardingGuard, adminGuard],
    loadComponent: () => import('./notifications/notification-center.component').then((m) => m.NotificationCenterComponent)
  },
  {
    path: 'security-audit',
    canActivate: [authGuard, onboardingGuard, adminGuard],
    loadComponent: () => import('./admin/security-audit.component').then((m) => m.SecurityAuditComponent)
  },
  {
    path: 'system-health',
    canActivate: [authGuard, onboardingGuard, adminGuard],
    loadComponent: () => import('./admin/system-health.component').then((m) => m.SystemHealthComponent)
  },
  {
    path: 'executive-timeline',
    canActivate: [authGuard, onboardingGuard, adminGuard],
    loadComponent: () => import('./admin/executive-timeline.component').then((m) => m.ExecutiveTimelineComponent)
  },
  {
    path: 'daily-report',
    canActivate: [authGuard, onboardingGuard, adminGuard],
    loadComponent: () => import('./admin/daily-report.component').then((m) => m.DailyReportComponent)
  },
  {
    path: 'onboarding',
    canActivate: [authGuard],
    loadComponent: () => import('./auth/onboarding.component').then((m) => m.OnboardingComponent)
  },
  {
    path: 'suppliers/:id/360',
    canActivate: [authGuard, onboardingGuard],
    loadComponent: () => import('./procurement/supplier-360.component').then((m) => m.Supplier360Component)
  },
  {
    path: 'prediction',
    canActivate: [authGuard, onboardingGuard],
    loadComponent: () => import('./ai-reports/prediction.component').then((m) => m.PredictionComponent)
  },
  {
    path: 'ai-dashboard',
    canActivate: [authGuard, onboardingGuard],
    loadComponent: () => import('./ai-reports/ai-dashboard.component').then((m) => m.AiDashboardComponent)
  },
  {
    path: 'stock-health',
    canActivate: [authGuard, onboardingGuard],
    loadComponent: () => import('./ai-reports/stock-health.component').then((m) => m.StockHealthComponent)
  },
  {
    path: 'copilot',
    canActivate: [authGuard, onboardingGuard],
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
