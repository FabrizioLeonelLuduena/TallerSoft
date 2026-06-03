import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CobrosService, CajaDiariaResponse, CobroResponse } from '../../services/cobros.service';
import { OrdenesService, OrdenTrabajoResponse } from '../../../ordenes/services/ordenes.service';

type ViewMode = 'cobros' | 'caja' | 'historial';

@Component({
  selector: 'app-caja-diaria',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MatIconModule, MatSnackBarModule],
  templateUrl: './caja-diaria.component.html',
  styleUrls: ['./caja-diaria.component.scss']
})
export class CajaDiariaComponent implements OnInit {
  private cobrosService  = inject(CobrosService);
  private ordenesService = inject(OrdenesService);
  private snackBar       = inject(MatSnackBar);
  private destroyRef     = inject(DestroyRef);

  viewMode: ViewMode = 'cobros';

  // Caja diaria (toggle 2)
  cajaDiaria: CajaDiariaResponse | null = null;
  ordenesPendientes: OrdenTrabajoResponse[] = [];
  isLoading = true;

  // Historial (toggle 3)
  historialCajas: CajaDiariaResponse[] = [];
  isLoadingHistorial = false;
  selectedAnio: number = new Date().getFullYear();
  selectedMes: number  = new Date().getMonth() + 1;

  readonly meses = [
    'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
    'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
  ];

  readonly anios: number[] = (() => {
    const current = new Date().getFullYear();
    return Array.from({ length: 5 }, (_, i) => current - i);
  })();

  ngOnInit(): void {
    this.cargarTodo();
  }

  switchView(mode: ViewMode): void {
    this.viewMode = mode;
    if (mode === 'historial' && this.historialCajas.length === 0) {
      this.cargarHistorial();
    }
  }

  // ── Carga inicial ────────────────────────────────────────
  cargarTodo(): void {
    this.isLoading = true;
    forkJoin({
      caja:       this.cobrosService.getCajaDiaria(),
      pendientes: this.ordenesService.listarOrdenes({ estado: 'LISTO' })
    }).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ caja, pendientes }) => {
          this.cajaDiaria        = caja;
          this.ordenesPendientes = pendientes;
          this.isLoading         = false;
        },
        error: () => {
          this.isLoading = false;
          this.snackBar.open('Error al cargar los datos de caja', 'Cerrar', { duration: 3000 });
        }
      });
  }

  // ── Historial ────────────────────────────────────────────
  cargarHistorial(): void {
    this.isLoadingHistorial = true;
    this.cobrosService.getHistorialCajas(this.selectedAnio, this.selectedMes)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.historialCajas     = data;
          this.isLoadingHistorial = false;
        },
        error: () => {
          this.isLoadingHistorial = false;
          this.snackBar.open('Error al cargar el historial', 'Cerrar', { duration: 3000 });
        }
      });
  }

  onPeriodoChange(): void {
    this.cargarHistorial();
  }

  // ── Totales mensuales ─────────────────────────────────────
  getTotalMes(): number {
    return this.historialCajas.reduce((s, c) => s + (c.totalDia as any), 0);
  }

  getTotalMesEfectivo(): number {
    return this.historialCajas.reduce((s, c) => s + (c.totalEfectivo as any), 0);
  }

  getTotalMesTarjeta(): number {
    return this.historialCajas.reduce((s, c) => s + (c.totalTarjeta as any), 0);
  }

  getTotalMesMercadoPago(): number {
    return this.historialCajas.reduce((s, c) => s + (c.totalMercadoPago as any), 0);
  }

  getTotalMesCobros(): number {
    return this.historialCajas.reduce((s, c) => s + c.cantidadOrdenes, 0);
  }

  getMesLabel(): string {
    return this.meses[this.selectedMes - 1];
  }

  // ── Helpers ──────────────────────────────────────────────
  formatCurrency(amount: number): string {
    return '$' + Number(amount).toLocaleString('es-AR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }

  padOrderId(id: number): string {
    return '#' + String(id).padStart(4, '0');
  }

  formatFecha(fecha: string): string {
    try {
      const [y, m, d] = fecha.split('-');
      return `${d}/${m}/${y}`;
    } catch { return fecha; }
  }

  formatFechaHoy(): string {
    return this.formatFecha(new Date().toISOString().split('T')[0]);
  }

  getEstadoClass(cobro: CobroResponse): string {
    const map: Record<string, string> = {
      APROBADO:  'estado-aprobado',
      PENDIENTE: 'estado-pendiente',
      RECHAZADO: 'estado-rechazado'
    };
    return map[cobro.estadoPago] ?? '';
  }

  getMedioPagoIcon(medio: string): string {
    const icons: Record<string, string> = {
      EFECTIVO:    'payments',
      TARJETA:     'credit_card',
      MERCADOPAGO: 'qr_code'
    };
    return icons[medio] ?? 'attach_money';
  }

  getMedioPagoLabel(medio: string): string {
    const labels: Record<string, string> = {
      EFECTIVO:    'Efectivo',
      TARJETA:     'Tarjeta',
      MERCADOPAGO: 'MercadoPago'
    };
    return labels[medio] ?? medio;
  }
}
