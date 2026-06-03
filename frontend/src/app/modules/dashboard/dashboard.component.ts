import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { Subject, forkJoin } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import {
  AnalyticsService,
  ResumenOrdenes,
  RendimientoTecnico,
  RepuestoCritico,
  ResumenCajaDiario,
  EvolucionMensual,
} from '@core/services/analytics.service';

export interface BarItem {
  mes: string;
  valor: number;
  porcentaje: number;
  label: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit, OnDestroy {
  loading = true;
  error = false;

  resumenOrdenes: ResumenOrdenes | null = null;
  cajaDiaria: ResumenCajaDiario | null = null;
  stockCritico: RepuestoCritico[] = [];
  tecnicosRendimiento: RendimientoTecnico[] = [];
  barras: BarItem[] = [];

  private destroy$ = new Subject<void>();

  constructor(private analytics: AnalyticsService) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private cargarDatos(): void {
    this.loading = true;
    this.error = false;

    forkJoin({
      ordenes:   this.analytics.getResumenOrdenes(),
      caja:      this.analytics.getResumenCajaDiario(),
      stock:     this.analytics.getStockCritico(),
      tecnicos:  this.analytics.getRendimientoTecnicos(),
      evolucion: this.analytics.getEvolucionMensual(6),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ ordenes, caja, stock, tecnicos, evolucion }) => {
          this.resumenOrdenes      = ordenes;
          this.cajaDiaria          = caja;
          this.stockCritico        = stock;
          this.tecnicosRendimiento = tecnicos;
          this.barras              = this.buildBarras(evolucion);
          this.loading = false;
        },
        error: () => {
          this.error   = true;
          this.loading = false;
        },
      });
  }

  private buildBarras(evolucion: EvolucionMensual[]): BarItem[] {
    if (!evolucion.length) return [];
    const max = Math.max(...evolucion.map(e => e.total_ingresos), 1);
    return evolucion.map(e => ({
      mes:        e.mes,
      valor:      e.total_ingresos,
      porcentaje: Math.round((e.total_ingresos / max) * 100),
      label:      `$${e.total_ingresos.toLocaleString('es-AR', { maximumFractionDigits: 0 })}`,
    }));
  }

  reintentar(): void {
    this.cargarDatos();
  }

  getInitials(nombre: string): string {
    return nombre.split(' ').slice(0, 2).map(n => n[0]).join('').toUpperCase();
  }
}
