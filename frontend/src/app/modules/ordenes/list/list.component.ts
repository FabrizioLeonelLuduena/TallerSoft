import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrdenesService, OrdenTrabajo } from '../services/ordenes.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './list.component.html',
  styleUrls: ['./list.component.scss']
})
export class ListComponent implements OnInit, OnDestroy {
  displayedColumns: string[] = ['id', 'cliente', 'estado', 'prioridad', 'tecnico', 'presupuesto', 'acciones'];
  dataSource = new MatTableDataSource<OrdenTrabajo>();
  loading = true;
  
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  
  private destroy$ = new Subject<void>();

  constructor(private ordenesService: OrdenesService) {}

  ngOnInit(): void {
    this.cargarOrdenes();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargarOrdenes(): void {
    this.loading = true;
    this.ordenesService.listarOrdenes()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (ordenes) => {
          this.dataSource.data = ordenes;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      });
  }

  getEstadoColor(estado: string): string {
    switch (estado) {
      case 'PENDIENTE': return 'warn';
      case 'EN_PROCESO': return 'accent';
      case 'LISTO': return 'primary';
      case 'ENTREGADO': return '';
      default: return '';
    }
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
