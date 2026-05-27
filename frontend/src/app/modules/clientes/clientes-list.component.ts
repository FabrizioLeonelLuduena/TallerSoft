import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { RouterModule } from '@angular/router';
import { Observable, of } from 'rxjs';

@Component({
  selector: 'app-clientes-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatTooltipModule,
    MatDividerModule,
    RouterModule
  ],
  templateUrl: './clientes-list.component.html',
  styleUrls: ['./clientes-list.component.scss']
})
export class ClientesListComponent implements OnInit {
  searchTerm = '';
  loading = false;
  clientes: any[] = [];
  filteredClientes: any[] = [];
  currentRole$ = of('ADMIN');

  ngOnInit() {
    this.loadClientes();
  }

  loadClientes() {
    this.loading = true;
    // Load clientes from service
    setTimeout(() => {
      this.clientes = [];
      this.loading = false;
      this.filterClientes();
    }, 500);
  }

  filterClientes() {
    const term = this.searchTerm.toLowerCase();
    this.filteredClientes = this.clientes.filter(c =>
      c.nombre.toLowerCase().includes(term)
    );
  }

  getInitials(nombre: string): string {
    return nombre
      .split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  getClienteOrdenesCount(clienteId: string): number {
    return 0;
  }

  verDetalles(cliente: any) {
    // Navigate to detail
  }

  crearCliente() {
    // Navigate to create
  }

  editarCliente(cliente: any) {
    // Navigate to edit
  }

  eliminarCliente(cliente: any) {
    // Delete cliente
  }

  canCreateCliente(): boolean {
    return true;
  }
}

