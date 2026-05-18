import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RepuestosService, Repuesto } from '../../ordenes/services/repuestos.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-stock-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDialogModule,
    MatSnackBarModule,
    FormsModule
  ],
  templateUrl: './list.component.html',
  styleUrls: ['./list.component.scss']
})
export class StockListComponent implements OnInit, OnDestroy {
  displayedColumns: string[] = ['nombre', 'categoria', 'precio', 'stockActual', 'stockMinimo', 'estado', 'acciones'];
  dataSource = new MatTableDataSource<Repuesto>();
  loading = true;
  solosCriticos = false;
  
  private destroy$ = new Subject<void>();

  constructor(
    private repuestosService: RepuestosService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.cargarRepuestos();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargarRepuestos(): void {
    this.loading = true;
    this.repuestosService.listarRepuestos(this.solosCriticos ? true : undefined)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (repuestos) => {
          this.dataSource.data = repuestos;
          this.loading = false;
        },
        error: () => {
          this.snackBar.open('Error al cargar repuestos', 'Cerrar', { duration: 3000 });
          this.loading = false;
        }
      });
  }

  onToggleCriticos(value: boolean): void {
    this.solosCriticos = value;
    this.cargarRepuestos();
  }

  crearRepuesto(): void {
    // TODO: Abrir diálogo para crear repuesto
  }

  editarRepuesto(repuesto: Repuesto): void {
    // TODO: Abrir diálogo para editar repuesto
  }

  getEstadoColor(critico: boolean): string {
    return critico ? 'warn' : 'primary';
  }

  getEstadoLabel(critico: boolean): string {
    return critico ? 'CRÍTICO' : 'OK';
  }
}
