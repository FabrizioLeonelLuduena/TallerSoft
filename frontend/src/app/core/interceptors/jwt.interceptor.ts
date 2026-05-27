import { inject } from '@angular/core';
import {
  HttpRequest,
  HttpHandlerFn,
  HttpEvent,
  HttpInterceptorFn,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';
import { Router } from '@angular/router';

/**
 * JWT Interceptor Function (Angular 17+)
 * Attaches Authorization header with JWT token to all HTTP requests
 * Handles 401 responses by logging out and redirecting to login
 */
export const jwtInterceptor: HttpInterceptorFn = (
  request: HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  // Debug logging
  console.log('[jwtInterceptor] Processing request:', request.url);
  console.log('[jwtInterceptor] Token available:', !!token);

  // Attach token to request if available
  if (token) {
    console.log('[jwtInterceptor] Token found, adding Authorization header');
    request = request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    console.log('[jwtInterceptor] Authorization header added:', request.headers.get('Authorization')?.substring(0, 30) + '...');
  } else {
    console.warn('[jwtInterceptor] No token found in sessionStorage');
  }

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      console.error('[jwtInterceptor] HTTP Error:', error.status, error.message);
      
      // Handle 401 Unauthorized
      if (error.status === 401) {
        console.log('[jwtInterceptor] 401 Unauthorized - logging out');
        authService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
