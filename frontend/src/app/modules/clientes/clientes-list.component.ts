import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-clientes-list',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatTableModule, MatButtonModule, MatIconModule],
  template: `
    <div class="clientes-container">
      <div class="header">
        <h1>Clientes</h1>
        <button mat-raised-button color="primary">
          <mat-icon>add</mat-icon>
          Nuevo Cliente
        </button>
      </div>
      <mat-card class="list-card">
        <p>Listado de clientes próximamente</p>
      </mat-card>
    </div>
  `,
  styles: [`
    .clientes-container {
      h1 { font-size: 24px; font-weight: 700; color: #1e3c72; }
      .header { display: flex; justify-content: space-between; margin-bottom: 24px; }
      .list-card { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); }
    }
  `]
})
export class ClientesListComponent {}
