import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { UsuariosService, Usuario } from '@core/services/usuarios.service';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-usuarios-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatPaginatorModule,
    MatSortModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDialogModule
  ],
  templateUrl: './list.component.html',
  styleUrls: ['./list.component.scss']
})
export class UsuariosListComponent implements OnInit {

  // Table data
  usuarios: Usuario[] = [];
  displayedColumns: string[] = ['nombre', 'email', 'rol', 'activo', 'createdAt', 'acciones'];

  // Filter form
  filterForm: FormGroup;

  // Pagination
  pageSizeOptions = [5, 10, 25, 50];
  pageSize = 10;
  pageIndex = 0;
  totalElements = 0;
  filteredUsuarios: Usuario[] = [];

  // Sorting
  sortField = 'nombre';
  sortDirection = 'asc';

  // Loading
  isLoading = false;

  roles = [
    { value: 'ADMIN', label: 'Administrador' },
    { value: 'TECNICO', label: 'Técnico' },
    { value: 'RECEPCION', label: 'Recepción' }
  ];

  constructor(
    private usuariosService: UsuariosService,
    private fb: FormBuilder,
    private dialog: MatDialog
  ) {
    this.filterForm = this.fb.group({
      busqueda: [''],
      rol: [''],
      activo: ['']
    });
  }

  ngOnInit(): void {
    this.cargarUsuarios();
    this.setupFilterListeners();
  }

  /**
   * Load users from backend
   */
  cargarUsuarios(): void {
    this.isLoading = true;
    this.usuariosService.obtenerUsuarios().subscribe({
      next: (data) => {
        this.usuarios = data;
        this.aplicarFiltros();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error al cargar usuarios:', error);
        this.isLoading = false;
      }
    });
  }

  /**
   * Setup listeners for filter form changes
   */
  setupFilterListeners(): void {
    this.filterForm.get('busqueda')?.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => {
        this.pageIndex = 0;
        this.aplicarFiltros();
      });

    this.filterForm.get('rol')?.valueChanges
      .subscribe(() => {
        this.pageIndex = 0;
        this.aplicarFiltros();
      });

    this.filterForm.get('activo')?.valueChanges
      .subscribe(() => {
        this.pageIndex = 0;
        this.aplicarFiltros();
      });
  }

  /**
   * Apply all active filters to the user list
   */
  aplicarFiltros(): void {
    const busqueda = this.filterForm.get('busqueda')?.value?.toLowerCase() || '';
    const rolFilter = this.filterForm.get('rol')?.value || '';
    const activoFilter = this.filterForm.get('activo')?.value;

    this.filteredUsuarios = this.usuarios.filter(usuario => {
      // Search filter (by name or email)
      const coincideBusqueda = busqueda === '' ||
        usuario.nombre.toLowerCase().includes(busqueda) ||
        usuario.email.toLowerCase().includes(busqueda);

      // Role filter
      const coincideRol = rolFilter === '' || usuario.rol === rolFilter;

      // Active status filter
      const coincideActivo = activoFilter === '' ||
        (activoFilter === 'activo' && usuario.activo) ||
        (activoFilter === 'inactivo' && !usuario.activo);

      return coincideBusqueda && coincideRol && coincideActivo;
    });

    // Apply sorting
    this.ordenar();

    // Update total
    this.totalElements = this.filteredUsuarios.length;
  }

  /**
   * Sort users
   */
  ordenar(): void {
    this.filteredUsuarios.sort((a, b) => {
      let aValue: any;
      let bValue: any;

      switch (this.sortField) {
        case 'nombre':
          aValue = a.nombre;
          bValue = b.nombre;
          break;
        case 'email':
          aValue = a.email;
          bValue = b.email;
          break;
        case 'rol':
          aValue = a.rol;
          bValue = b.rol;
          break;
        case 'createdAt':
          aValue = new Date(a.createdAt);
          bValue = new Date(b.createdAt);
          break;
        default:
          return 0;
      }

      if (aValue < bValue) {
        return this.sortDirection === 'asc' ? -1 : 1;
      }
      if (aValue > bValue) {
        return this.sortDirection === 'asc' ? 1 : -1;
      }
      return 0;
    });
  }

  /**
   * Handle sort change
   */
  onSortChange(sort: Sort): void {
    this.sortField = sort.active;
    this.sortDirection = sort.direction === '' ? 'asc' : sort.direction;
    this.pageIndex = 0;
    this.aplicarFiltros();
  }

  /**
   * Handle pagination change
   */
  onPageChange(event: PageEvent): void {
    this.pageSize = event.pageSize;
    this.pageIndex = event.pageIndex;
  }

  /**
   * Get paginated data to display
   */
  getDisplayedData(): Usuario[] {
    const startIndex = this.pageIndex * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    return this.filteredUsuarios.slice(startIndex, endIndex);
  }

  /**
   * Get role label in Spanish
   */
  getRoleLabel(rol: string): string {
    return this.roles.find(r => r.value === rol)?.label || rol;
  }

  /**
   * Get role color for chip
   */
  getRoleColor(rol: string): string {
    switch (rol) {
      case 'ADMIN':
        return '#d32f2f'; // Red
      case 'TECNICO':
        return '#1976d2'; // Blue
      case 'RECEPCION':
        return '#388e3c'; // Green
      default:
        return '#757575';
    }
  }

  /**
   * Clear all filters
   */
  limpiarFiltros(): void {
    this.filterForm.reset();
    this.pageIndex = 0;
    this.aplicarFiltros();
  }

  /**
   * Edit user
   */
  editarUsuario(usuario: Usuario): void {
    console.log('Editar usuario:', usuario);
    // TODO: Open edit dialog
  }

  /**
   * Delete user (soft delete)
   */
  eliminarUsuario(usuario: Usuario): void {
    if (confirm(`¿Desactiva a ${usuario.nombre}?`)) {
      this.usuariosService.desactivarUsuario(usuario.id).subscribe({
        next: () => {
          console.log('Usuario desactivado');
          this.cargarUsuarios();
        },
        error: (error) => {
          console.error('Error al desactivar usuario:', error);
        }
      });
    }
  }

  /**
   * Activate user
   */
  activarUsuario(usuario: Usuario): void {
    if (confirm(`¿Activa a ${usuario.nombre}?`)) {
      this.usuariosService.actualizarUsuario(usuario.id, { activo: true }).subscribe({
        next: () => {
          console.log('Usuario activado');
          this.cargarUsuarios();
        },
        error: (error) => {
          console.error('Error al activar usuario:', error);
        }
      });
    }
  }

  /**
   * Navigate to create new user (will implement later)
   */
  nuevoUsuario(): void {
    console.log('Crear nuevo usuario');
    // TODO: Navigate to create user form
  }
}
