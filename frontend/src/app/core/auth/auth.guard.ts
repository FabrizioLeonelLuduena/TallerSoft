import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/**
 * Auth Guard
 * Protects routes that require authentication
 * Redirects to login if user is not authenticated
 */
@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private authService: AuthService, private router: Router) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    if (this.authService.isLoggedIn()) {
      return true;
    }

    // Redirect to login
    this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }
}
