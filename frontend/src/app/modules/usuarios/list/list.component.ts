import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router, RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '@core/auth/auth.service';
import { UsuarioService, UsuarioResponse } from '../services/usuario.service';
import { EditUserDialogComponent } from '../dialogs/edit-user-dialog/edit-user-dialog.component';

@Component({
  selector: 'app-usuarios-list',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatSnackBarModule,
    RouterModule,
    EditUserDialogComponent
  ],
  templateUrl: './list.component.html',
  styleUrls: ['./list.component.scss']
})
export class ListComponent implements OnInit {
  private authService = inject(AuthService);
  private usuarioService = inject(UsuarioService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  usuarios: UsuarioResponse[] = [];
  isLoading = true;
  currentRole: string | null = '';
  searchTerm = '';

  currentPage = 1;
  readonly pageSize = 10;

  get filteredUsuarios(): UsuarioResponse[] {
    if (!this.searchTerm.trim()) return this.usuarios;
    const term = this.searchTerm.toLowerCase();
    return this.usuarios.filter(u =>
      u.nombre.toLowerCase().includes(term) ||
      u.email.toLowerCase().includes(term)
    );
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredUsuarios.length / this.pageSize));
  }

  get pagedUsuarios(): UsuarioResponse[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredUsuarios.slice(start, start + this.pageSize);
  }

  get pageStart(): number {
    return this.filteredUsuarios.length === 0 ? 0 : (this.currentPage - 1) * this.pageSize + 1;
  }

  get pageEnd(): number {
    return Math.min(this.currentPage * this.pageSize, this.filteredUsuarios.length);
  }

  prevPage() { if (this.currentPage > 1) this.currentPage--; }
  nextPage() { if (this.currentPage < this.totalPages) this.currentPage++; }

  showDeleteUserModal = false;
  userToDelete: any = null;

  showEditDialog = false;
  usuarioToEdit: UsuarioResponse | null = null;

  ngOnInit() {
    this.currentRole = this.authService.getCurrentRole();
    if (this.currentRole !== 'ADMIN') {
      this.showAccessDenied();
      return;
    }
    this.loadUsuarios();
  }

  private loadUsuarios() {
    this.isLoading = true;
    this.usuarioService.listarUsuarios()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (usuarios) => {
          this.usuarios = usuarios;
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Error loading usuarios:', err);
          this.isLoading = false;
          this.snackBar.open(
            err.error?.message || 'Error al cargar los usuarios',
            'Cerrar',
            { duration: 3000, horizontalPosition: 'right', verticalPosition: 'bottom' }
          );
        }
      });
  }

  openCreateDialog() {
    this.router.navigate(['/usuarios/nuevo']);
  }

  openEditDialog(usuario: UsuarioResponse): void {
    this.usuarioToEdit = usuario;
    this.showEditDialog = true;
  }

  onEditDialogClosed(saved: boolean): void {
    this.showEditDialog = false;
    this.usuarioToEdit = null;
    if (saved) {
      this.loadUsuarios();
    }
  }

  getInitials(nombre: string): string {
    return nombre.charAt(0).toUpperCase();
  }

  formatDate(dateStr: string): string {
    try {
      return new Date(dateStr).toLocaleDateString('es-AR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch {
      return 'N/A';
    }
  }

  getRolBadgeClass(rol: string): string {
    const rolLower = rol.toLowerCase();
    if (rolLower === 'admin') return 'admin';
    if (rolLower === 'tecnico') return 'tecnico';
    if (rolLower === 'recepcion') return 'recepcion';
    return '';
  }

  getRolDisplayText(rol: string): string {
    const rolLower = rol.toLowerCase();
    if (rolLower === 'admin') return 'ADMIN';
    if (rolLower === 'tecnico') return 'TÉCNICO';
    if (rolLower === 'recepcion') return 'RECEPCIÓN';
    return rol;
  }

  private showAccessDenied() {
    setTimeout(() => {
      this.router.navigate(['/dashboard']);
    }, 2000);
  }

  onCreateClick(): void {
    this.router.navigate(['/usuarios/nuevo']);
  }

  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchTerm = target.value;
    this.currentPage = 1;
  }

  openDeleteUserModal(usuario: any) {
    this.userToDelete = usuario;
    this.showDeleteUserModal = true;
  }

  confirmDeleteUser() {
    if (this.userToDelete) {
      this.usuarioService.deleteUsuario(this.userToDelete.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.snackBar.open('Usuario eliminado correctamente', 'Cerrar', {
              duration: 3000,
              horizontalPosition: 'right',
              verticalPosition: 'bottom',
              panelClass: ['snackbar-success']
            });
            this.loadUsuarios();
            this.cancelDeleteUser();
          },
          error: (err) => {
            console.error('Error deleting usuario:', err);
            this.snackBar.open(
              err.error?.message || 'Error al eliminar el usuario',
              'Cerrar',
              { duration: 3000, horizontalPosition: 'right', verticalPosition: 'bottom' }
            );
          }
        });
    }
  }

  cancelDeleteUser() {
    this.showDeleteUserModal = false;
    this.userToDelete = null;
  }
}
