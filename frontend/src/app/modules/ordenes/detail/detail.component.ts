import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormsModule } from '@angular/forms';
import { OrdenesService, OrdenTrabajo } from '../services/ordenes.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatTabsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    FormsModule
  ],
  templateUrl: './detail.component.html',
  styleUrls: ['./detail.component.scss']
})
export class DetailComponent implements OnInit, OnDestroy {
  orden: OrdenTrabajo | null = null;
  loading = true;
  displayedColumns: string[] = ['nombre', 'cantidad', 'precioUnit', 'total'];
  
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private ordenesService: OrdenesService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const id = params['id'];
      this.cargarOrden(id);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargarOrden(id: number): void {
    this.loading = true;
    this.ordenesService.obtenerOrden(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (orden) => {
          this.orden = orden;
          this.loading = false;
        },
        error: () => {
          this.snackBar.open('Error al cargar la orden', 'Cerrar', { duration: 3000 });
          this.loading = false;
        }
      });
  }

  cambiarEstado(): void {
    if (!this.orden) return;
    // TODO: Implementar cambio de estado con validaciones
  }

  agregarRepuesto(): void {
    if (!this.orden) return;
    // TODO: Abrir diálogo para agregar repuesto
  }

  editarDiagnostico(): void {
    if (!this.orden) return;
    // TODO: Permitir editar diagnóstico
  }
}
