import { Routes } from '@angular/router';
import { AuthGuard } from '../../core/auth/auth.guard';
import { StockListComponent } from './list/list.component';

export const stockRoutes: Routes = [
  {
    path: '',
    component: StockListComponent,
    canActivate: [AuthGuard]
  }
];
