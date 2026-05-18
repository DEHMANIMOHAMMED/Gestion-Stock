import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environment';

export interface DemoAccount {
  organisation: string;
  scenario: string;
  adminEmail: string;
  userEmail: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class DemoService {
  constructor(private http: HttpClient) {}

  getAccounts(): Observable<DemoAccount[]> {
    return this.http.get<DemoAccount[]>(`${environment.apiUrl}/demo/accounts`);
  }
}
