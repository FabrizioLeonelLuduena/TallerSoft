import { Routes } from '@angular/router';
import { AuthGuard } from '@core/auth/auth.guard';
import { RoleGuard } from '@core/auth/role.guard';
import { LayoutComponent } from './shared/layout/layout.component';
import { DashboardComponent } from './modules/dashboard/dashboard.component';
import { OrdenesListComponent } from './modules/ordenes/ordenes-list.component';
import { InventarioComponent } from './modules/inventario/inventario.component';
import { ReportesComponent } from './modules/reportes/reportes.component';
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
        loadChildren: () => import('./modules/clientes/clientes.routes').then(m => m.clientesRoutes),
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
        loadChildren: () => import('./modules/usuarios/usuarios.routes').then(m => m.USUARIOS_ROUTES),
        canActivate: [AuthGuard]
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
