import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterModule, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '@core/auth/auth.service';
import { OrdenesService, OrdenTrabajoResponse } from '../services/ordenes.service';

@Component({
  selector: 'app-ordenes-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatSelectModule,
    MatSnackBarModule,
    RouterModule
  ],
  templateUrl: './list.component.html',
  styleUrls: ['./list.component.scss']
})
export class ListComponent implements OnInit {
  private authService = inject(AuthService);
  private ordenesService = inject(OrdenesService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  ordenes: OrdenTrabajoResponse[] = [];
  filteredOrdenes: OrdenTrabajoResponse[] = [];
  isLoading = true;
  selectedEstado = '';
  selectedTecnicoId: number | null = null;
  currentRole: string | null = '';
  searchTerm = '';
  
  estados = ['PENDIENTE', 'EN_PROCESO', 'LISTO', 'ENTREGADO'];
  tecnicos: { id: number | null; nombre: string }[] = [];
  
  showDeleteOrdenConfirm = false;
  ordenToDelete: OrdenTrabajoResponse | null = null;
  isDeleting = false;

  readonly estadoOrden = { ENTREGADO: 0, LISTO: 1, EN_PROCESO: 2, PENDIENTE: 3 };

  ngOnInit() {
    this.currentRole = this.authService.getCurrentRole();
    
    if (this.currentRole === 'TECNICO') {
      this.loadMisOrdenes();
    } else {
      this.loadOrdenes();
    }
  }

  private loadOrdenes() {
    this.isLoading = true;
    this.ordenesService.listarOrdenes()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (ordenes) => {
          this.ordenes = ordenes;
          this.filteredOrdenes = [...ordenes];
          this.extractTecnicos();
          this.isLoading = false;
        },
        error: (err) => {
          this.isLoading = false;
          this.snackBar.open(
            err.error?.message || 'Error al cargar las órdenes',
            'Cerrar',
            { duration: 3000, horizontalPosition: 'right', verticalPosition: 'bottom' }
          );
        }
      });
  }

  private loadMisOrdenes() {
    this.isLoading = true;
    this.ordenesService.listarMisOrdenes()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (ordenes) => {
          this.ordenes = ordenes;
          this.filteredOrdenes = [...ordenes];
          this.isLoading = false;
        },
        error: (err) => {
          this.isLoading = false;
          this.snackBar.open(
            err.error?.message || 'Error al cargar tus órdenes',
            'Cerrar',
            { duration: 3000, horizontalPosition: 'right', verticalPosition: 'bottom' }
          );
        }
      });
  }

  private extractTecnicos() {
    const tecnicoSet = new Map<number | null, string>();
    this.ordenes.forEach(orden => {
      if (orden.tecnicoId !== null && orden.tecnicoNombre) {
        tecnicoSet.set(orden.tecnicoId, orden.tecnicoNombre);
      }
    });
    this.tecnicos = Array.from(tecnicoSet.entries()).map(([id, nombre]) => ({ id, nombre }));
  }

  filterByEstado(estado: string) {
    this.selectedEstado = estado;
    this.applyFilters();
  }

  filterByTecnico(tecnicoId: number | null) {
    this.selectedTecnicoId = tecnicoId;
    this.applyFilters();
  }

  filterBySearch() {
    this.applyFilters();
  }

  toggleFiltersPanel() {
    // Placeholder para panel de filtros si es necesario expandir
  }

  private applyFilters() {
    this.filteredOrdenes = this.ordenes.filter(orden => {
      const estadoMatch = !this.selectedEstado || orden.estado === this.selectedEstado;
      const tecnicoMatch = this.selectedTecnicoId === null || orden.tecnicoId === this.selectedTecnicoId;
      const searchMatch = !this.searchTerm || 
        orden.clienteNombre.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        orden.id.toString().includes(this.searchTerm) ||
        (orden.tecnicoNombre && orden.tecnicoNombre.toLowerCase().includes(this.searchTerm.toLowerCase()));
      return estadoMatch && tecnicoMatch && searchMatch;
    });
  }

  navigateToDetail(id: number, event?: Event) {
    if (event) event.stopPropagation();
    this.router.navigate(['/ordenes', id]);
  }

  navigateToCreate() {
    this.router.navigate(['/ordenes/nueva']);
  }

  setFilteredOrdenes(ordenes: OrdenTrabajoResponse[]) {
    this.filteredOrdenes = this.ordenesSorted_internal(ordenes);
  }

  get ordenesSorted(): OrdenTrabajoResponse[] {
    return this.ordenesSorted_internal(this.filteredOrdenes);
  }

  private ordenesSorted_internal(ordenes: OrdenTrabajoResponse[]): OrdenTrabajoResponse[] {
    return [...ordenes].sort((a, b) => {
      const estadoDiff = (this.estadoOrden[a.estado as keyof typeof this.estadoOrden] || 999) - 
                         (this.estadoOrden[b.estado as keyof typeof this.estadoOrden] || 999);
      if (estadoDiff !== 0) return estadoDiff;
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });
  }

  formatCurrency(amount: number): string {
    return '$' + amount.toLocaleString('es-AR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    });
  }

  getRelativeTime(dateStr: string): string {
    try {
      const diff = Date.now() - new Date(dateStr).getTime();
      const hours = Math.floor(diff / 3600000);
      if (hours < 1) return 'hace un momento';
      if (hours < 24) return `hace ${hours}h`;
      const days = Math.floor(hours / 24);
      if (days === 1) return 'ayer';
      return `hace ${days} días`;
    } catch {
      return 'N/A';
    }
  }

  padOrderId(id: number): string {
    return '#' + String(id).padStart(4, '0');
  }

  getEstadoBgColor(estado: string): string {
    switch (estado) {
      case 'PENDIENTE': return 'rgba(59, 130, 246, 0.15)';
      case 'EN_PROCESO': return 'rgba(249, 115, 22, 0.15)';
      case 'LISTO': return 'rgba(34, 197, 94, 0.15)';
      case 'ENTREGADO': return 'rgba(75, 85, 99, 0.2)';
      default: return 'transparent';
    }
  }

  getEstadoBorderColor(estado: string): string {
    switch (estado) {
      case 'PENDIENTE': return 'var(--color-info)';
      case 'EN_PROCESO': return 'var(--color-accent)';
      case 'LISTO': return 'var(--color-success)';
      case 'ENTREGADO': return 'var(--color-text-muted)';
      default: return 'transparent';
    }
  }

  getPrioridadColor(prioridad: string): string {
    switch (prioridad) {
      case 'ALTA': return 'var(--color-danger)';
      case 'NORMAL': return 'var(--color-warning)';
      case 'BAJA': return 'var(--color-text-muted)';
      default: return 'var(--color-text-muted)';
    }
  }

  canCreateOrden(): boolean {
    return this.currentRole === 'ADMIN' || this.currentRole === 'RECEPCION';
  }

  isTecnicoRole(): boolean {
    return this.currentRole === 'TECNICO';
  }

  getEstadoColor(estado: string): string {
    switch (estado) {
      case 'PENDIENTE': return 'var(--color-info)';
      case 'EN_PROCESO': return 'var(--color-accent)';
      case 'LISTO': return 'var(--color-success)';
      case 'ENTREGADO': return 'var(--color-text-muted)';
      default: return 'transparent';
    }
  }

  getPriorityColor(prioridad: string): string {
    switch (prioridad) {
      case 'ALTA': return 'var(--color-danger)';
      case 'NORMAL': return 'var(--color-warning)';
      case 'BAJA': return 'var(--color-text-muted)';
      default: return 'var(--color-text-muted)';
    }
  }

  showDeleteOrdenModal(orden: OrdenTrabajoResponse, event: Event) {
    event.stopPropagation();
    this.ordenToDelete = orden;
    this.showDeleteOrdenConfirm = true;
  }

  closeDeleteOrdenModal() {
    this.showDeleteOrdenConfirm = false;
    this.ordenToDelete = null;
  }

  confirmDeleteOrden() {
    if (!this.ordenToDelete) return;
    this.isDeleting = true;
    this.ordenesService.eliminarOrden(this.ordenToDelete.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.snackBar.open('Orden eliminada exitosamente', 'Cerrar', { duration: 2000 });
          this.loadOrdenes();
          this.closeDeleteOrdenModal();
        },
        error: (err) => {
          this.isDeleting = false;
          this.snackBar.open(
            err.error?.message || 'Error al eliminar la orden',
            'Cerrar',
            { duration: 3000 }
          );
        }
      });
  }

  getInitials(nombre?: string | null): string {
    if (!nombre) return '?';
    return nombre.split(' ').map(n => n[0]).join('').toUpperCase();
  }

  shouldShowEstadoSeparator(index: number): boolean {
    if (index === 0) return true;
    return this.ordenesSorted[index].estado !== this.ordenesSorted[index - 1].estado;
  }

  getEstadoForSeparator(index: number): string {
    return this.ordenesSorted[index].estado;
  }

  getEstadoCount(estado: string): number {
    return this.ordenesSorted.filter(o => o.estado === estado).length;
  }
}
