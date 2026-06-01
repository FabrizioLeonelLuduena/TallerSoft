import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { RepuestosService, Repuesto } from '../ordenes/services/repuestos.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatCheckboxModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  repuestosCriticos: Repuesto[] = [];
  loadingStock = true;

  private destroy$ = new Subject<void>();

  constructor(private repuestosService: RepuestosService) {}

  ngOnInit(): void {
    this.cargarStockCritico();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private cargarStockCritico(): void {
    this.repuestosService.listarRepuestos(true)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (repuestos) => {
          this.repuestosCriticos = repuestos;
          this.loadingStock = false;
        },
        error: () => {
          this.loadingStock = false;
        }
      });
  }
}
