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

  viewMode: 'cobros' | 'historial' = 'cobros';

  cajaDiaria: CajaDiariaResponse | null = null;
  ordenesPendientes: OrdenTrabajoResponse[] = [];
  isLoading  = true;
  fechaInput = this.hoyISO();

  ngOnInit(): void {
    this.cargarTodo();
  }

  switchView(mode: 'cobros' | 'historial'): void {
    this.viewMode = mode;
  }

  cargarTodo(): void {
    this.isLoading = true;
    forkJoin({
      caja:       this.cobrosService.getCajaDiaria(this.fechaInput),
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

  onFechaChange(): void {
    this.cobrosService.getCajaDiaria(this.fechaInput)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => { this.cajaDiaria = data; },
        error: () => this.snackBar.open('Error al cargar la caja diaria', 'Cerrar', { duration: 3000 })
      });
  }

  // ── Helpers ──────────────────────────────────────────────

  formatCurrency(amount: number): string {
    return '$' + amount.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  padOrderId(id: number): string {
    return '#' + String(id).padStart(4, '0');
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

  formatFechaDisplay(): string {
    try {
      const [y, m, d] = this.fechaInput.split('-');
      return `${d}/${m}/${y}`;
    } catch { return this.fechaInput; }
  }

  private hoyISO(): string {
    return new Date().toISOString().split('T')[0];
  }
}
