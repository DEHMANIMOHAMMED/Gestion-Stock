import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../environment';
import { Role } from '../auth/auth.service';

export interface OrganisationUser {
  id: number;
  email: string;
  organisationId: number;
  organisationName: string;
  role: Exclude<Role, 'OWNER'>;
  enabled: boolean;
  lastLoginAt: string | null;
}

export interface CreateOrganisationUserRequest {
  email: string;
  password: string;
  role: Exclude<Role, 'OWNER'>;
}

@Injectable({ providedIn: 'root' })
export class OrganisationUserService {
  constructor(private http: HttpClient) {}

  list() {
    return this.http.get<OrganisationUser[]>(`${environment.apiUrl}/organisation-users`);
  }

  create(request: CreateOrganisationUserRequest) {
    return this.http.post<OrganisationUser>(`${environment.apiUrl}/organisation-users`, request);
  }

  updateRole(userId: number, role: Exclude<Role, 'OWNER'>) {
    return this.http.patch<OrganisationUser>(`${environment.apiUrl}/organisation-users/${userId}/role`, { role });
  }

  setEnabled(userId: number, enabled: boolean) {
    return this.http.patch<OrganisationUser>(`${environment.apiUrl}/organisation-users/${userId}/enabled?enabled=${enabled}`, {});
  }

  resetPassword(userId: number, temporaryPassword: string) {
    return this.http.post<OrganisationUser>(`${environment.apiUrl}/organisation-users/${userId}/reset-password`, { temporaryPassword });
  }
}
