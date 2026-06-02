import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
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
  ],
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss']
})
export class LayoutComponent implements OnInit {

  currentUser: CurrentUser | null = null;
  sidebarCollapsed = false;
  isMobile = false;
  profileDropdownOpen = false;
  hasNotifications = true;

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
      label: 'Stock',
      icon: 'inventory_2',
      route: '/stock',
      roles: ['ADMIN', 'TECNICO', 'RECEPCION']
    },
    {
      label: 'Caja y Facturación',
      icon: 'point_of_sale',
      route: '/caja/diaria',
      roles: ['ADMIN', 'RECEPCION']
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
  }

  getVisibleNavItems(): NavItem[] {
    if (!this.currentUser) return [];
    return this.navItems.filter(item =>
      !item.roles || item.roles.includes(this.currentUser!.rol)
    );
  }

  logout(): void {
    this.profileDropdownOpen = false;
    this.authService.logout();
    this.router.navigate(['/login']);
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
        return;
      }
    }
  }

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  toggleProfileDropdown(): void {
    this.profileDropdownOpen = !this.profileDropdownOpen;
  }

  goToProfile(): void {
    this.profileDropdownOpen = false;
    this.router.navigate(['/profile']);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const profileElement = document.querySelector('.user-profile');
    
    if (profileElement && !profileElement.contains(target)) {
      this.profileDropdownOpen = false;
    }
  }
}
