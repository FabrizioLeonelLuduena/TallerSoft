import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';
import { Router } from '@angular/router';

/**
 * JWT Interceptor
 * Attaches Authorization header with JWT token to all HTTP requests
 * Handles 401 responses by logging out and redirecting to login
 */
@Injectable()
export class JwtInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService, private router: Router) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken();

    // Debug logging
    console.log('[JwtInterceptor] Processing request:', request.url);
    console.log('[JwtInterceptor] Token available:', !!token);

    // Attach token to request if available
    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      console.log('[JwtInterceptor] Authorization header added');
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('[JwtInterceptor] HTTP Error:', error.status, error.message);
        
        // Handle 401 Unauthorized
        if (error.status === 401) {
          this.authService.logout();
          this.router.navigate(['/login']);
        }
        return throwError(() => error);
      })
    );
  }
}
