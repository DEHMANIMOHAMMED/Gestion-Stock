import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../environment';

export interface SupportMessage {
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
  replies: SupportReply[];
}

export interface SupportReply {
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

export interface SupportMessageRequest {
  subject: string;
  message: string;
  attachmentName?: string | null;
  attachmentContentType?: string | null;
  attachmentData?: string | null;
}

@Injectable({ providedIn: 'root' })
export class SupportService {
  constructor(private http: HttpClient) {}

  messages() {
    return this.http.get<SupportMessage[]>(`${environment.apiUrl}/support/messages`);
  }

  create(request: SupportMessageRequest) {
    return this.http.post<SupportMessage>(`${environment.apiUrl}/support/messages`, request);
  }

  reply(id: number, request: Omit<SupportMessageRequest, 'subject'>) {
    return this.http.post<SupportMessage>(`${environment.apiUrl}/support/messages/${id}/replies`, request);
  }
}
