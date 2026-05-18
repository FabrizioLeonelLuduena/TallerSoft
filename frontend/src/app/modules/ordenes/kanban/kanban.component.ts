import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { OrdenesService, OrdenTrabajo } from '../services/ordenes.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-kanban',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSnackBarModule,
    DragDropModule
  ],
  templateUrl: './kanban.component.html',
  styleUrls: ['./kanban.component.scss']
})
export class KanbanComponent implements OnInit, OnDestroy {
  loading = true;
  
  pendientes: OrdenTrabajo[] = [];
  enProceso: OrdenTrabajo[] = [];
  listo: OrdenTrabajo[] = [];
  entregado: OrdenTrabajo[] = [];
  
  private destroy$ = new Subject<void>();
  
  statuses = ['PENDIENTE', 'EN_PROCESO', 'LISTO', 'ENTREGADO'];

  constructor(
    private ordenesService: OrdenesService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.cargarOrdenes();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargarOrdenes(): void {
    this.loading = true;
    this.ordenesService.listarOrdenesActivas()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (ordenes) => {
          this.pendientes = ordenes.filter(o => o.estado === 'PENDIENTE');
          this.enProceso = ordenes.filter(o => o.estado === 'EN_PROCESO');
          this.listo = ordenes.filter(o => o.estado === 'LISTO');
          this.entregado = ordenes.filter(o => o.estado === 'ENTREGADO');
          this.loading = false;
        },
        error: (err) => {
          this.snackBar.open('Error al cargar órdenes', 'Cerrar', { duration: 3000 });
          this.loading = false;
        }
      });
  }

  drop(event: CdkDragDrop<OrdenTrabajo[]>): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      const orden = event.previousContainer.data[event.previousIndex];
      const nuevoEstado = this.mapContainerToEstado(event.container);
      
      this.ordenesService.cambiarEstado(orden.id, nuevoEstado)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            transferArrayItem(
              event.previousContainer.data,
              event.container.data,
              event.previousIndex,
              event.currentIndex
            );
            this.snackBar.open(`Orden ${orden.id} movida a ${nuevoEstado}`, 'Cerrar', { duration: 2000 });
          },
          error: (err) => {
            if (err.status === 409) {
              this.snackBar.open(err.error.message || 'Transición inválida', 'Cerrar', { duration: 3000 });
            } else {
              this.snackBar.open('Error al actualizar orden', 'Cerrar', { duration: 3000 });
            }
            this.cargarOrdenes();
          }
        });
    }
  }

  private mapContainerToEstado(container: any): string {
    if (container.id === 'pendientes') return 'PENDIENTE';
    if (container.id === 'en-proceso') return 'EN_PROCESO';
    if (container.id === 'listo') return 'LISTO';
    if (container.id === 'entregado') return 'ENTREGADO';
    return 'PENDIENTE';
  }

  getPriorityColor(prioridad: string): string {
    switch (prioridad) {
      case 'ALTA': return 'warn';
      case 'NORMAL': return 'accent';
      case 'BAJA': return 'primary';
      default: return 'primary';
    }
  }
}
