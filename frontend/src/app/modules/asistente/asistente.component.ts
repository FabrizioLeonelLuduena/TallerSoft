import { Component, OnDestroy, AfterViewChecked, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { AnalyticsService } from '@core/services/analytics.service';

interface Mensaje {
  rol: 'usuario' | 'asistente';
  texto: string;
  timestamp: Date;
}

@Component({
  selector: 'app-asistente',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './asistente.component.html',
  styleUrls: ['./asistente.component.scss'],
})
export class AsistenteComponent implements AfterViewChecked, OnDestroy {
  @ViewChild('mensajesContainer') mensajesContainer!: ElementRef<HTMLDivElement>;

  mensajes: Mensaje[] = [
    {
      rol: 'asistente',
      texto: '¡Hola! Soy el asistente de TallerSoft. Podés preguntarme sobre órdenes, stock, ingresos o el rendimiento del equipo técnico.',
      timestamp: new Date(),
    },
  ];

  preguntaActual = '';
  cargando = false;
  inputFocused = false;
  private scrollPendiente = false;
  private destroy$ = new Subject<void>();

  sugerencias = [
    '¿Cuántas órdenes están pendientes?',
    '¿Cuál fue el ingreso de hoy?',
    '¿Qué repuestos están en stock crítico?',
    '¿Cuál es el rendimiento del equipo técnico?',
  ];

  constructor(private analytics: AnalyticsService) {}

  ngAfterViewChecked(): void {
    if (this.scrollPendiente) {
      this.scrollAlFinal();
      this.scrollPendiente = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  enviar(): void {
    const pregunta = this.preguntaActual.trim();
    if (!pregunta || this.cargando) return;

    this.mensajes.push({ rol: 'usuario', texto: pregunta, timestamp: new Date() });
    this.preguntaActual = '';
    this.cargando = true;
    this.scrollPendiente = true;

    this.analytics
      .consultarAsistente(pregunta)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: res => {
          this.mensajes.push({ rol: 'asistente', texto: res.respuesta, timestamp: new Date() });
          this.cargando = false;
          this.scrollPendiente = true;
        },
        error: () => {
          this.mensajes.push({
            rol: 'asistente',
            texto: 'Ocurrió un error al consultar el asistente. Verificá que el servicio de analítica esté activo.',
            timestamp: new Date(),
          });
          this.cargando = false;
          this.scrollPendiente = true;
        },
      });
  }

  usarSugerencia(texto: string): void {
    this.preguntaActual = texto;
    this.enviar();
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.enviar();
    }
  }

  private scrollAlFinal(): void {
    if (this.mensajesContainer) {
      const el = this.mensajesContainer.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }
}
