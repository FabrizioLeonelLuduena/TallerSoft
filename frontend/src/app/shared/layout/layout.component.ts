import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '@core/auth/auth.service';
import { CurrentUser } from '@core/auth/auth.service';
import { filter } from 'rxjs/operators';

interface NavItem {
  label: string;
  icon: string;
  route?: string;
  children?: NavItem[];
  roles?: string[];
}

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatListModule,
    MatMenuModule,
    MatDividerModule,
    MatTooltipModule,
  ],
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss']
})
export class LayoutComponent implements OnInit {

  currentUser: CurrentUser | null = null;
  sidenavOpen = true;
  sidebarCollapsed = false;
  isMobile = false;
  sidenavMode: 'side' | 'over' = 'side';
  currentModuleName = 'Dashboard';

  navItems: NavItem[] = [
    {
      label: 'Dashboard',
      icon: 'dashboard',
      route: '/dashboard',
      roles: ['ADMIN', 'TECNICO', 'RECEPCION']
    },
    {
      label: 'Clientes',
      icon: 'people',
      route: '/clientes',
      roles: ['ADMIN', 'RECEPCION']
    },
    {
      label: 'Órdenes de Trabajo',
      icon: 'build',
      route: '/ordenes',
      roles: ['ADMIN', 'TECNICO', 'RECEPCION']
    },
    {
      label: 'Inventario',
      icon: 'inventory_2',
      route: '/inventario',
      roles: ['ADMIN', 'TECNICO']
    },
    {
      label: 'Reportes',
      icon: 'bar_chart',
      route: '/reportes',
      roles: ['ADMIN']
    },
    {
      label: 'Usuarios',
      icon: 'admin_panel_settings',
      route: '/usuarios',
      roles: ['ADMIN']
    },
  ];

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
    this.checkScreenSize();
    window.addEventListener('resize', () => this.checkScreenSize());
    
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.updateModuleName(event.url);
    });
  }

  checkScreenSize(): void {
    this.isMobile = window.innerWidth <= 768;
    this.sidenavMode = this.isMobile ? 'over' : 'side';
    if (this.isMobile) {
      this.sidenavOpen = false;
    } else {
      this.sidenavOpen = true;
    }
  }

  getVisibleNavItems(): NavItem[] {
    if (!this.currentUser) return [];
    return this.navItems.filter(item =>
      !item.roles || item.roles.includes(this.currentUser!.rol)
    );
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  toggleSidenav(): void {
    this.sidenavOpen = !this.sidenavOpen;
  }

  getRolLabel(): string {
    const rolMap: { [key: string]: string } = {
      'ADMIN': 'Administrador',
      'TECNICO': 'Técnico',
      'RECEPCION': 'Recepción'
    };
    return rolMap[this.currentUser?.rol || 'TECNICO'];
  }

  getUserInitials(): string {
    if (!this.currentUser?.email) return 'U';
    const parts = this.currentUser.email.split('@')[0].split('.');
    return parts.map(part => part.charAt(0).toUpperCase()).join('').substring(0, 2);
  }

  getUserName(): string {
    if (!this.currentUser?.email) return 'User';
    return this.currentUser.email.split('@')[0];
  }

  getCurrentModuleName(): string {
    return this.currentModuleName;
  }

  updateModuleName(url: string): void {
    const moduleMap: { [key: string]: string } = {
      '/dashboard': 'Dashboard',
      '/ordenes': 'Órdenes',
      '/clientes': 'Clientes',
      '/inventario': 'Inventario',
      '/stock': 'Stock',
      '/caja': 'Caja',
      '/reportes': 'Reportes',
      '/usuarios': 'Usuarios',
      '/asistente': 'Asistente IA'
    };

    for (const [path, name] of Object.entries(moduleMap)) {
      if (url.startsWith(path)) {
        this.currentModuleName = name;
        return;
      }
    }
    this.currentModuleName = 'Dashboard';
  }

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }
}
