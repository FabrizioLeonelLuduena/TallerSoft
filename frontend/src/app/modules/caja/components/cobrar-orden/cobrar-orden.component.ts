import { Component, OnInit, OnDestroy, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { OrdenesService, OrdenTrabajoResponse } from '../../../ordenes/services/ordenes.service';
import { CobrosService, CobroResponse, MedioPago } from '../../services/cobros.service';

@Component({
  selector: 'app-cobrar-orden',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatSnackBarModule],
  templateUrl: './cobrar-orden.component.html',
  styleUrls: ['./cobrar-orden.component.scss']
})
export class CobrarOrdenComponent implements OnInit, OnDestroy {
  private route       = inject(ActivatedRoute);
  private router      = inject(Router);
  private ordenesService = inject(OrdenesService);
  private cobrosService  = inject(CobrosService);
  private snackBar    = inject(MatSnackBar);
  private destroyRef  = inject(DestroyRef);

  orden: OrdenTrabajoResponse | null = null;
  isLoading      = true;
  isSubmitting   = false;

  medioPago: MedioPago = 'EFECTIVO';
  montoRecibido: number | null = null;

  // Modal de MercadoPago
  showMpModal       = false;
  mpQrImageUrl: string | null = null;
  cobroId: number | null = null;
  isVerificandoPago = false;
  pagoAprobado      = false;

  private pollInterval: ReturnType<typeof setInterval> | null = null;
  private readonly POLL_INTERVAL_MS = 4000;

  get vuelto(): number {
    if (this.medioPago !== 'EFECTIVO' || !this.montoRecibido || !this.orden) return 0;
    return Math.max(0, this.montoRecibido - this.orden.presupuesto);
  }

  get montoInsuficiente(): boolean {
    if (this.medioPago !== 'EFECTIVO' || !this.montoRecibido || !this.orden) return false;
    return this.montoRecibido < this.orden.presupuesto;
  }

  ngOnInit(): void {
    const id = +this.route.snapshot.paramMap.get('ordenId')!;
    this.ordenesService.obtenerOrden(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (orden) => {
          this.orden = orden;
          this.isLoading = false;
          if (orden.estado !== 'LISTO') {
            this.snackBar.open('Solo se pueden cobrar órdenes en estado LISTO', 'Cerrar', { duration: 4000 });
            this.router.navigate(['/ordenes', id]);
          }
        },
        error: () => {
          this.isLoading = false;
          this.snackBar.open('Error al cargar la orden', 'Cerrar', { duration: 3000 });
          this.router.navigate(['/ordenes']);
        }
      });
  }

  seleccionarMedio(medio: MedioPago): void {
    this.medioPago = medio;
    this.montoRecibido = null;
  }

  onSubmit(): void {
    if (!this.orden || this.isSubmitting) return;
    if (this.medioPago === 'EFECTIVO' && this.montoInsuficiente) return;

    this.isSubmitting = true;
    this.cobrosService.registrarCobro({
      ordenId: this.orden.id,
      monto: this.orden.presupuesto,
      montoRecibido: this.medioPago === 'EFECTIVO' ? (this.montoRecibido ?? undefined) : undefined,
      medioPago: this.medioPago
    }).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (cobro) => {
          this.isSubmitting = false;
          if (cobro.medioPago === 'MERCADOPAGO') {
            this.cobroId      = cobro.id;
            this.mpQrImageUrl = cobro.mpQrImageUrl ?? null;
            this.pagoAprobado = false;
            this.showMpModal  = true;
            this.startPolling();
          } else {
            this.snackBar.open('Cobro registrado exitosamente', '', { duration: 3000 });
            this.router.navigate(['/ordenes', this.orden!.id]);
          }
        },
        error: (err) => {
          this.isSubmitting = false;
          this.snackBar.open(err.error?.message || 'Error al registrar el cobro', 'Cerrar', { duration: 4000 });
        }
      });
  }

  verificarPago(manual = false): void {
    if (!this.cobroId || this.isVerificandoPago || this.pagoAprobado) return;
    this.isVerificandoPago = true;
    this.cobrosService.getCobro(this.cobroId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (cobro) => {
          this.isVerificandoPago = false;
          if (cobro.estadoPago === 'APROBADO') {
            this.pagoAprobado = true;
            this.stopPolling();
            setTimeout(() => {
              this.showMpModal = false;
              this.router.navigate(['/ordenes', this.orden!.id]);
            }, 1500);
          } else if (manual) {
            this.snackBar.open('El pago aún no fue acreditado. Esperá unos segundos.', 'Cerrar', { duration: 4000 });
          }
        },
        error: () => {
          this.isVerificandoPago = false;
          if (manual) {
            this.snackBar.open('Error al verificar el pago', 'Cerrar', { duration: 3000 });
          }
        }
      });
  }

  private startPolling(): void {
    this.stopPolling();
    this.pollInterval = setInterval(() => this.verificarPago(false), this.POLL_INTERVAL_MS);
  }

  private stopPolling(): void {
    if (this.pollInterval !== null) {
      clearInterval(this.pollInterval);
      this.pollInterval = null;
    }
  }

  cerrarMpModal(): void {
    this.stopPolling();
    this.showMpModal = false;
    this.router.navigate(['/ordenes', this.orden?.id]);
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  navigateBack(): void {
    this.router.navigate(['/ordenes', this.orden?.id]);
  }

  formatCurrency(amount: number): string {
    return '$' + amount.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  padOrderId(id: number): string {
    return '#' + String(id).padStart(4, '0');
  }

  getLabelSubmit(): string {
    const labels: Record<string, string> = {
      EFECTIVO:    'Registrar cobro en efectivo',
      TARJETA:     'Confirmar cobro con tarjeta',
      MERCADOPAGO: 'Generar QR de pago'
    };
    return labels[this.medioPago];
  }
}
