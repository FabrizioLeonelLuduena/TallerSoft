import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '@environments/environment';
import { ProfileService } from '@core/services/profile.service';
import { ChatHistoryService } from '@core/services/chat-history.service';

export interface LoginResponse {
  token: string;
  userId: number;
  email: string;
  rol: string;
}

export interface CurrentUser {
  userId: number;
  email: string;
  rol: string;
}

/**
 * Authentication Service
 * Handles user login, logout, token management, and JWT storage
 * Token is stored in sessionStorage only (never localStorage)
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private currentUserSubject = new BehaviorSubject<CurrentUser | null>(this.getCurrentUser());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private profileService: ProfileService,
    private chatHistoryService: ChatHistoryService,
  ) {
    // Si ya hay sesión activa (recarga de página), inicializar los servicios
    // con el userId del token existente para que usen la clave correcta de localStorage.
    const existing = this.getCurrentUser();
    if (existing) {
      this.profileService.init(existing.userId);
      this.chatHistoryService.init(existing.userId);
    }
  }

  /**
   * Login with email and password
   */
  login(email: string, password: string): Observable<LoginResponse> {
    console.log('[AuthService] Starting login process for:', email);
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, {
      email,
      password
    }).pipe(
      map(response => {
        console.log('[AuthService] Login response received:', response);
        console.log('[AuthService] Token from response:', response.token?.substring(0, 50) + '...');
        this.profileService.init(response.userId);
        this.chatHistoryService.init(response.userId);
        this.setToken(response.token);
        console.log('[AuthService] Token saved to sessionStorage');
        console.log('[AuthService] Token in storage:', sessionStorage.getItem('token')?.substring(0, 50) + '...');
        this.currentUserSubject.next(this.getCurrentUser());
        console.log('[AuthService] Current user set:', this.getCurrentUser());
        return response;
      })
    );
  }

  /**
   * Logout and clear token
   */
  logout(): void {
    sessionStorage.removeItem('token');
    this.profileService.reset();
    this.chatHistoryService.reset();
    this.currentUserSubject.next(null);
  }

  /**
   * Store JWT token in sessionStorage (NEVER localStorage)
   */
  setToken(token: string): void {
    sessionStorage.setItem('token', token);
  }

  /**
   * Get JWT token from sessionStorage
   */
  getToken(): string | null {
    const token = sessionStorage.getItem('token');
    if (token) {
      console.log('[AuthService] Token retrieved from sessionStorage, length:', token.length);
    } else {
      console.log('[AuthService] No token found in sessionStorage');
    }
    return token;
  }

  /**
   * Check if user is logged in
   */
  isLoggedIn(): boolean {
    const loggedIn = this.getToken() !== null;
    console.log('[AuthService] isLoggedIn:', loggedIn);
    return loggedIn;
  }

  /**
   * Get current user from decoded JWT token
   */
  getCurrentUser(): CurrentUser | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }

    try {
      const payload = this.decodeToken(token);
      console.log('[AuthService] Decoded payload:', payload);
      return {
        userId: payload.userId,
        email: payload.email,
        rol: payload.rol
      };
    } catch (error) {
      console.error('Error decoding token:', error);
      return null;
    }
  }

  /**
   * Get current user role
   */
  getCurrentRole(): string | null {
    const user = this.getCurrentUser();
    return user ? user.rol : null;
  }

  /**
   * Decode JWT token (basic decoding, doesn't verify signature)
   */
  private decodeToken(token: string): any {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch (error) {
      throw new Error('Invalid token format');
    }
  }
}
