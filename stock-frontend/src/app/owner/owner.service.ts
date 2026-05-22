import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../environment';

export interface OwnerOrganization {
  id: number;
  name: string;
  industry: string | null;
  sizeRange: string | null;
  city: string | null;
  country: string | null;
  status: 'ACTIVE' | 'SUSPENDED' | 'TRIAL';
  planCode: 'TRIAL' | 'STARTER' | 'PRO' | string;
  subscriptionStatus: string;
  onboardingCompleted: boolean;
  createdAt: string | null;
  usersCount: number;
  productsCount: number;
  stockMovementsCount: number;
}

export interface LegalSettings {
  id: number | null;
  companyName: string;
  legalNotice: string | null;
  privacyPolicy: string | null;
  terms: string | null;
  legalDocumentUrl: string | null;
  privacyDocumentUrl: string | null;
  termsDocumentUrl: string | null;
  updatedAt: string | null;
  updatedByEmail: string | null;
}

export interface OwnerSupportMessage {
  id: number;
  organisationId: number;
  organisationName: string;
  userId: number | null;
  senderEmail: string;
  subject: string;
  message: string;
  attachmentName: string | null;
  attachmentContentType: string | null;
  attachmentData: string | null;
  status: 'OPEN' | 'READ' | 'RESOLVED' | string;
  createdAt: string;
  readAt: string | null;
  resolvedAt: string | null;
  replies: OwnerSupportReply[];
}

export interface OwnerSupportReply {
  id: number;
  authorUserId: number | null;
  authorEmail: string;
  authorRole: string;
  message: string;
  attachmentName: string | null;
  attachmentContentType: string | null;
  attachmentData: string | null;
  createdAt: string;
}

export interface OwnerSupportUser {
  id: number;
  email: string;
  role: string;
  enabled: boolean;
  lastLoginAt: string | null;
}

export interface OwnerDashboard {
  organizationsCount: number;
  usersCount: number;
  productsCount: number;
  stockMovementsCount: number;
  activeOrganizationsCount: number;
  trialOrganizationsCount: number;
  organizations: OwnerOrganization[];
  legalSettings: LegalSettings;
}

@Injectable({ providedIn: 'root' })
export class OwnerService {
  constructor(private http: HttpClient) {}

  dashboard() {
    return this.http.get<OwnerDashboard>(`${environment.apiUrl}/owner/dashboard`);
  }

  updateLegalSettings(request: Partial<LegalSettings> & { companyName: string }) {
    return this.http.put<LegalSettings>(`${environment.apiUrl}/owner/legal-settings`, request);
  }

  supportMessages() {
    return this.http.get<OwnerSupportMessage[]>(`${environment.apiUrl}/owner/support-messages`);
  }

  markSupportMessageRead(id: number) {
    return this.http.patch<OwnerSupportMessage>(`${environment.apiUrl}/owner/support-messages/${id}/read`, {});
  }

  resolveSupportMessage(id: number) {
    return this.http.patch<OwnerSupportMessage>(`${environment.apiUrl}/owner/support-messages/${id}/resolve`, {});
  }

  replySupportMessage(id: number, request: { message: string; attachmentName?: string | null; attachmentContentType?: string | null; attachmentData?: string | null }) {
    return this.http.post<OwnerSupportMessage>(`${environment.apiUrl}/owner/support-messages/${id}/replies`, request);
  }

  organisationUsers(organisationId: number) {
    return this.http.get<OwnerSupportUser[]>(`${environment.apiUrl}/owner/organizations/${organisationId}/users`);
  }

  updateUserStatus(userId: number, enabled: boolean, reason: string) {
    return this.http.patch<OwnerSupportUser>(`${environment.apiUrl}/owner/users/${userId}/status`, { enabled, reason });
  }

  changeUserPassword(userId: number, newPassword: string, reason: string) {
    return this.http.patch<OwnerSupportUser>(`${environment.apiUrl}/owner/users/${userId}/password`, { newPassword, reason });
  }

  cancelOrganisationSubscription(organisationId: number, reason: string) {
    return this.http.patch(`${environment.apiUrl}/owner/organizations/${organisationId}/subscription/cancel`, { reason });
  }
}
