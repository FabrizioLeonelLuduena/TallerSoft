import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatGridListModule } from '@angular/material/grid-list';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatButtonModule, MatGridListModule],
  template: `
    <div class="dashboard-container">
      <h1>Dashboard</h1>
      <p class="subtitle">Resumen general del taller</p>

      <!-- KPI Cards -->
      <div class="kpi-grid">
        <mat-card class="kpi-card">
          <div class="kpi-header">
            <mat-icon class="kpi-icon pending">pending_actions</mat-icon>
            <div>
              <p class="kpi-value">12</p>
              <p class="kpi-label">Órdenes Pendientes</p>
            </div>
          </div>
        </mat-card>

        <mat-card class="kpi-card">
          <div class="kpi-header">
            <mat-icon class="kpi-icon process">build</mat-icon>
            <div>
              <p class="kpi-value">8</p>
              <p class="kpi-label">En Proceso</p>
            </div>
          </div>
        </mat-card>

        <mat-card class="kpi-card">
          <div class="kpi-header">
            <mat-icon class="kpi-icon ready">check_circle</mat-icon>
            <div>
              <p class="kpi-value">5</p>
              <p class="kpi-label">Listas para Entregar</p>
            </div>
          </div>
        </mat-card>

        <mat-card class="kpi-card">
          <div class="kpi-header">
            <mat-icon class="kpi-icon revenue">attach_money</mat-icon>
            <div>
              <p class="kpi-value">\$4,250</p>
              <p class="kpi-label">Ingresos Hoy</p>
            </div>
          </div>
        </mat-card>
      </div>

      <!-- Main content sections -->
      <div class="dashboard-content">
        <mat-card class="section-card">
          <mat-card-header>
            <mat-card-title>Órdenes Recientes</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>No hay órdenes recientes</p>
          </mat-card-content>
        </mat-card>

        <mat-card class="section-card">
          <mat-card-header>
            <mat-card-title>Alertas</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>No hay alertas</p>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container {
      h1 {
        font-size: 28px;
        font-weight: 700;
        margin: 0 0 4px 0;
        color: #1e3c72;
      }

      .subtitle {
        font-size: 14px;
        color: #888;
        margin: 0 0 24px 0;
      }
    }

    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
      margin-bottom: 32px;

      .kpi-card {
        padding: 20px;
        cursor: pointer;
        transition: all 0.3s ease;
        border-left: 4px solid #ccc;

        &:hover {
          box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
          transform: translateY(-4px);
        }

        .kpi-header {
          display: flex;
          gap: 16px;
          align-items: flex-start;

          .kpi-icon {
            font-size: 40px;
            width: 40px;
            height: 40px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;

            &.pending {
              background-color: #ff9800;
            }

            &.process {
              background-color: #2196f3;
            }

            &.ready {
              background-color: #4caf50;
            }

            &.revenue {
              background-color: #9c27b0;
            }
          }

          .kpi-value {
            margin: 0;
            font-size: 24px;
            font-weight: 700;
            color: #1e3c72;
          }

          .kpi-label {
            margin: 4px 0 0 0;
            font-size: 13px;
            color: #666;
          }
        }
      }
    }

    .dashboard-content {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 16px;

      .section-card {
        padding: 20px;
      }
    }

    @media (max-width: 768px) {
      .kpi-grid {
        grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
        gap: 12px;
      }
    }
  `]
})
export class DashboardComponent {}

