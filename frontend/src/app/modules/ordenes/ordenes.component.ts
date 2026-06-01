import { Component, OnInit, inject, DestroyRef, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '@core/auth/auth.service';
import { OrdenesService, OrdenTrabajoResponse } from './services/ordenes.service';
import { KanbanComponent } from './kanban/kanban.component';
import { ListComponent } from './list/list.component';

@Component({
  selector: 'app-ordenes',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatIconModule,
    MatButtonModule,
    MatSnackBarModule,
    KanbanComponent,
    ListComponent
  ],
  templateUrl: './ordenes.component.html',
  styleUrls: ['./ordenes.component.scss']
})
export class OrdenesPrincipalComponent implements OnInit {
  private authService = inject(AuthService);
  private ordenesService = inject(OrdenesService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  @ViewChild('kanbanChild') kanbanChild!: KanbanComponent;
  @ViewChild('listaChild') listaChild!: ListComponent;

  viewMode: 'kanban' | 'lista' = 'kanban';
  isLoading = true;
  searchTerm = '';
  selectedTecnicoId: number | null = null;
  selectedPrioridad: string = '';
  
  ordenes: OrdenTrabajoResponse[] = [];
  tecnicos: { id: number | null; nombre: string }[] = [];
  prioridades = ['BAJA', 'NORMAL', 'ALTA'];
  currentRole: string | null = '';

  ngOnInit() {
    this.currentRole = this.authService.getCurrentRole();
    this.loadOrdenes();
  }

  private loadOrdenes() {
    this.isLoading = true;
    const loadObservable = this.currentRole === 'TECNICO' 
      ? this.ordenesService.listarMisOrdenes()
      : this.ordenesService.listarOrdenes();

    loadObservable
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (ordenes) => {
          this.ordenes = ordenes;
          this.extractTecnicos();
          this.isLoading = false;
          this.applyFilters();
        },
        error: (err) => {
          this.isLoading = false;
          this.snackBar.open(
            err.error?.message || 'Error al cargar las órdenes',
            'Cerrar',
            { duration: 3000 }
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

  switchViewMode(mode: 'kanban' | 'lista') {
    this.viewMode = mode;
  }

  applyFilters() {
    const filtered = this.ordenes.filter(orden => {
      const tecnicoMatch = !this.selectedTecnicoId || orden.tecnicoId === this.selectedTecnicoId;
      const prioridadMatch = !this.selectedPrioridad || orden.prioridad === this.selectedPrioridad;
      const searchMatch = !this.searchTerm || 
        orden.clienteNombre.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        orden.id.toString().includes(this.searchTerm) ||
        orden.fallaReportada.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        (orden.tecnicoNombre && orden.tecnicoNombre.toLowerCase().includes(this.searchTerm.toLowerCase()));
      return tecnicoMatch && prioridadMatch && searchMatch;
    });

    if (this.kanbanChild) {
      this.kanbanChild.setFilteredOrdenes(filtered);
    }
    if (this.listaChild) {
      this.listaChild.setFilteredOrdenes(filtered);
    }
  }

  navigateToCreate() {
    this.router.navigate(['/ordenes/nueva']);
  }

  canCreateOrden(): boolean {
    return this.currentRole === 'ADMIN' || this.currentRole === 'RECEPCION';
  }
}
