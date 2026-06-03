import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { DragDropModule, CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { OrdenesService, OrdenTrabajoResponse } from '../services/ordenes.service';

interface KanbanColumn {
  estado: string;
  label: string;
  ordenes: OrdenTrabajoResponse[];
}

@Component({
  selector: 'app-kanban',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    DragDropModule,
    MatIconModule,
    MatButtonModule,
    MatSnackBarModule
  ],
  templateUrl: './kanban.component.html',
  styleUrls: ['./kanban.component.scss']
})
export class KanbanComponent implements OnInit {
  private ordenesService = inject(OrdenesService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  columns: KanbanColumn[] = [
    { estado: 'PENDIENTE', label: 'Pendiente', ordenes: [] },
    { estado: 'EN_PROCESO', label: 'En Proceso', ordenes: [] },
    { estado: 'LISTO', label: 'Listo', ordenes: [] },
    { estado: 'ENTREGADO', label: 'Entregado', ordenes: [] }
  ];

  connectedLists = ['PENDIENTE', 'EN_PROCESO', 'LISTO', 'ENTREGADO'];
  isLoading = true;
  allOrdenes: OrdenTrabajoResponse[] = [];

  ngOnInit() {
    // Data is provided by the parent OrdenesPrincipalComponent via setFilteredOrdenes()
    this.isLoading = false;
  }

  loadOrdenes() {
    this.isLoading = true;
    this.ordenesService.listarOrdenesActivas()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (ordenes) => {
          this.allOrdenes = ordenes;
          this.reorganizeColumns(ordenes);
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
          this.snackBar.open('Error al cargar órdenes', 'Cerrar', { duration: 3000 });
        }
      });
  }

  setFilteredOrdenes(ordenes: OrdenTrabajoResponse[]) {
    this.reorganizeColumns(ordenes);
  }

  private reorganizeColumns(ordenes: OrdenTrabajoResponse[]) {
    this.columns.forEach(col => col.ordenes = []);
    ordenes.forEach(orden => {
      const col = this.columns.find(c => c.estado === orden.estado);
      if (col) col.ordenes.push(orden);
    });
  }

  onDrop(event: CdkDragDrop<OrdenTrabajoResponse[]>) {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      return;
    }

    const orden = event.previousContainer.data[event.previousIndex];
    const nuevoEstado = this.columns[this.connectedLists.indexOf(event.container.id)].estado;
    
    const originalPrevItems = [...event.previousContainer.data];
    const originalCurrItems = [...event.container.data];

    transferArrayItem(event.previousContainer.data, event.container.data, event.previousIndex, event.currentIndex);

    this.ordenesService.cambiarEstado(orden.id, nuevoEstado)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          orden.estado = nuevoEstado as 'PENDIENTE' | 'EN_PROCESO' | 'LISTO' | 'ENTREGADO';
          this.snackBar.open('Orden actualizada', 'Cerrar', { duration: 2000 });
        },
        error: (err) => {
          event.previousContainer.data = originalPrevItems;
          event.container.data = originalCurrItems;
          const message = err.error?.message || 'Error al cambiar estado';
          this.snackBar.open(message, 'Cerrar', { duration: 3000 });
        }
      });
  }

  getRelativeTime(dateStr: string): string {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'hace un momento';
    if (diffMins < 60) return `hace ${diffMins}m`;
    if (diffHours < 24) return `hace ${diffHours}h`;
    if (diffDays === 1) return 'ayer';
    return `hace ${diffDays}d`;
  }

  getTecnicoInitials(nombre?: string | null): string {
    if (!nombre) return '?';
    return nombre.split(' ').map(n => n[0]).join('').toUpperCase();
  }

  getEstadoColor(estado: string): string {
    switch (estado) {
      case 'PENDIENTE': return 'var(--color-info)';
      case 'EN_PROCESO': return 'var(--color-accent)';
      case 'LISTO': return 'var(--color-success)';
      case 'ENTREGADO': return 'var(--color-text-muted)';
      default: return 'var(--color-text-muted)';
    }
  }

  getEstadoBgColor(estado: string): string {
    switch (estado) {
      case 'PENDIENTE': return 'rgba(59, 130, 246, 0.1)';
      case 'EN_PROCESO': return 'rgba(0, 245, 212, 0.1)';
      case 'LISTO': return 'rgba(34, 197, 94, 0.1)';
      case 'ENTREGADO': return 'rgba(156, 163, 175, 0.1)';
      default: return 'transparent';
    }
  }

  getCardBorderColor(estado: string): string {
    switch (estado) {
      case 'PENDIENTE': return 'var(--color-info)';
      case 'EN_PROCESO': return 'var(--color-accent)';
      case 'LISTO': return 'var(--color-success)';
      case 'ENTREGADO': return 'var(--color-text-muted)';
      default: return 'var(--color-border)';
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

  navigateToDetail(ordenId: number) {
    this.router.navigate(['/ordenes', ordenId]);
  }

  navigateToList() {
    this.router.navigate(['/ordenes']);
  }

  navigateToCreateNew() {
    this.router.navigate(['/ordenes/nueva']);
  }

  padOrderId(id: number): string {
    return String(id).padStart(4, '0');
  }

  trackByOrdenId(index: number, orden: OrdenTrabajoResponse): number {
    return orden.id;
  }
}
