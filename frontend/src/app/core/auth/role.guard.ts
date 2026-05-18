import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/**
 * Role Guard
 * Protects routes based on user role
 * Redirects to unauthorized page if user doesn't have required role
 * Expects route.data.roles to contain array of allowed roles
 */
@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {

  constructor(private authService: AuthService, private router: Router) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    const requiredRoles = route.data['roles'] as string[];
    const currentRole = this.authService.getCurrentRole();

    if (requiredRoles && currentRole && requiredRoles.includes(currentRole)) {
      return true;
    }

    // Redirect to unauthorized
    this.router.navigate(['/unauthorized']);
    return false;
  }
}
