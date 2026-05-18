import { Routes } from '@angular/router';
import { AuthGuard } from '@core/auth/auth.guard';
import { RoleGuard } from '@core/auth/role.guard';
import { LayoutComponent } from './shared/layout/layout.component';
import { DashboardComponent } from './modules/dashboard/dashboard.component';
import { ClientesListComponent } from './modules/clientes/clientes-list.component';
import { OrdenesListComponent } from './modules/ordenes/ordenes-list.component';
import { InventarioComponent } from './modules/inventario/inventario.component';
import { ReportesComponent } from './modules/reportes/reportes.component';
import { UsuariosComponent } from './modules/usuarios/usuarios.component';
import { UnauthorizedComponent } from './pages/unauthorized/unauthorized.component';

export const routes: Routes = [
  {
    path: 'login',
    loadChildren: () => import('./modules/auth/auth.routes').then(m => m.authRoutes)
  },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [AuthGuard],
    children: [
      {
        path: 'dashboard',
        component: DashboardComponent
      },
      {
        path: 'clientes',
        component: ClientesListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'RECEPCION'] }
      },
      {
        path: 'ordenes',
        component: OrdenesListComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'TECNICO', 'RECEPCION'] }
      },
      {
        path: 'inventario',
        component: InventarioComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'TECNICO'] }
      },
      {
        path: 'reportes',
        component: ReportesComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'usuarios',
        component: UsuariosComponent,
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'unauthorized',
        component: UnauthorizedComponent
      },
      {
        path: '',
        redirectTo: '/dashboard',
        pathMatch: 'full'
      }
    ]
  },
  {
    path: '**',
    redirectTo: '/dashboard'
  }
];
