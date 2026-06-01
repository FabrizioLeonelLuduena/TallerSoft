import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { RepuestosService, Repuesto } from '../../ordenes/services/repuestos.service';
import { RepuestoDialogComponent } from '../dialogs/repuesto-dialog/repuesto-dialog.component';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@core/auth/auth.service';

type FilterState = 'todos' | 'criticos' | 'bajo';

@Component({
  selector: 'app-stock-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatTooltipModule,
    FormsModule,
    RepuestoDialogComponent
  ],
  templateUrl: './list.component.html',
  styleUrls: ['./list.component.scss']
})
export class StockListComponent implements OnInit, OnDestroy {
  allRepuestos: Repuesto[] = [];
  filteredStock: Repuesto[] = [];
  loading = true;
  filterState: FilterState = 'todos';
  searchTerm = '';

  showEditDialog = false;
  repuestoToEdit: Repuesto | null = null;

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private repuestosService: RepuestosService,
    private snackBar: MatSnackBar,
    private authService: AuthService,
    private router: Router
  ) {}

  get currentRole(): string | null {
    return this.authService.getCurrentRole();
  }

  onCreateClick(): void {
    this.router.navigate(['/stock/nuevo']);
  }

  ngOnInit(): void {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => this.applyFilters());

    this.cargarRepuestos();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargarRepuestos(): void {
    this.loading = true;
    this.repuestosService.listarRepuestos()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (repuestos) => {
          this.allRepuestos = repuestos;
          this.applyFilters();
          this.loading = false;
        },
        error: () => {
          this.snackBar.open('Error al cargar repuestos', 'Cerrar', { duration: 3000 });
          this.loading = false;
        }
      });
  }

  setFilter(state: FilterState): void {
    this.filterState = state;
    this.applyFilters();
  }

  filterStock(): void {
    this.searchSubject.next(this.searchTerm);
  }

  private applyFilters(): void {
    let base = this.allRepuestos;

    if (this.filterState === 'criticos') {
      base = base.filter(r => r.critico);
    } else if (this.filterState === 'bajo') {
      base = base.filter(r => r.stockActual <= r.stockMinimo);
    }

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      base = base.filter(r =>
        r.nombre.toLowerCase().includes(term) ||
        (r.categoria?.toLowerCase().includes(term) ?? false)
      );
    }

    this.filteredStock = base;
  }

  editarRepuesto(repuesto: Repuesto): void {
    this.repuestoToEdit = repuesto;
    this.showEditDialog = true;
  }

  onDialogClosed(saved: boolean): void {
    this.showEditDialog = false;
    this.repuestoToEdit = null;
    if (saved) this.cargarRepuestos();
  }

  eliminarRepuesto(_repuesto: Repuesto): void {
    this.snackBar.open('La eliminación de repuestos no está disponible en esta versión', 'Cerrar', { duration: 3000 });
  }

  // ---- Stats helpers ----

  getTotalRepuestos(): number {
    return this.allRepuestos.length;
  }

  getCriticosCount(): number {
    return this.allRepuestos.filter(r => r.critico).length;
  }

  getBajoStockCount(): number {
    return this.allRepuestos.filter(r => r.stockActual <= r.stockMinimo && !r.critico).length;
  }

  getDisponiblesCount(): number {
    return this.allRepuestos.filter(r => r.stockActual > r.stockMinimo).length;
  }

  // ---- Display helpers ----

  getStockPercentage(repuesto: Repuesto): number {
    if (!repuesto.stockMinimo) return 100;
    const ratio = repuesto.stockActual / (repuesto.stockMinimo * 2);
    return Math.min(Math.round(ratio * 100), 100);
  }

  getEstadoClass(repuesto: Repuesto): string {
    if (repuesto.critico) return 'critico';
    if (repuesto.stockActual <= repuesto.stockMinimo) return 'bajo';
    return 'disponible';
  }

  getEstadoLabel(repuesto: Repuesto): string {
    if (repuesto.critico) return 'CRÍTICO';
    if (repuesto.stockActual <= repuesto.stockMinimo) return 'BAJO';
    return 'OK';
  }
}
