import {
  Component,
  OnDestroy,
  AfterViewChecked,
  ElementRef,
  ViewChild,
  HostListener,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { Router, NavigationEnd } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';

import { AnalyticsService } from '@core/services/analytics.service';

interface Mensaje {
  rol: 'usuario' | 'asistente';
  texto: string;
  timestamp: Date;
}

@Component({
  selector: 'app-chat-flotante',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './chat-flotante.component.html',
  styleUrls: ['./chat-flotante.component.scss'],
})
export class ChatFlotanteComponent implements AfterViewChecked, OnDestroy {
  @ViewChild('mensajesRef') mensajesRef!: ElementRef<HTMLDivElement>;
  @ViewChild('inputRef') inputRef!: ElementRef<HTMLTextAreaElement>;

  abierto = false;
  isAsistenteRoute = false;
  cargando = false;
  pregunta = '';
  mensajesNoLeidos = 0;
  inputFocused = false;

  mensajes: Mensaje[] = [
    {
      rol: 'asistente',
      texto: '¡Hola! ¿En qué te puedo ayudar hoy?',
      timestamp: new Date(),
    },
  ];

  sugerencias = [
    '¿Cuántas órdenes están pendientes?',
    '¿Cuál fue el ingreso de hoy?',
    '¿Qué repuestos están en stock crítico?',
  ];

  private scrollPendiente = false;
  private destroy$ = new Subject<void>();

  constructor(private analytics: AnalyticsService, private router: Router) {
    this.isAsistenteRoute = this.router.url.startsWith('/asistente');

    this.router.events
      .pipe(
        filter(e => e instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((e: any) => {
        this.isAsistenteRoute = e.url.startsWith('/asistente');
        if (this.isAsistenteRoute) this.abierto = false;
      });
  }

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

  toggleChat(): void {
    this.abierto = !this.abierto;
    if (this.abierto) {
      this.mensajesNoLeidos = 0;
      this.scrollPendiente = true;
      setTimeout(() => this.inputRef?.nativeElement.focus(), 150);
    }
  }

  cerrar(): void {
    this.abierto = false;
  }

  enviar(): void {
    const texto = this.pregunta.trim();
    if (!texto || this.cargando) return;

    this.mensajes.push({ rol: 'usuario', texto, timestamp: new Date() });
    this.pregunta = '';
    this.cargando = true;
    this.scrollPendiente = true;

    this.analytics
      .consultarAsistente(texto)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: res => {
          this.mensajes.push({ rol: 'asistente', texto: res.respuesta, timestamp: new Date() });
          this.cargando = false;
          this.scrollPendiente = true;
          if (!this.abierto) this.mensajesNoLeidos++;
        },
        error: () => {
          this.mensajes.push({
            rol: 'asistente',
            texto: 'Error al consultar el asistente. Verificá que el servicio esté activo.',
            timestamp: new Date(),
          });
          this.cargando = false;
          this.scrollPendiente = true;
        },
      });
  }

  usarSugerencia(s: string): void {
    this.pregunta = s;
    this.enviar();
  }

  onKeydown(e: KeyboardEvent): void {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      this.enviar();
    }
  }

  irAlAsistente(): void {
    this.abierto = false;
    this.router.navigate(['/asistente']);
  }

  private scrollAlFinal(): void {
    if (this.mensajesRef) {
      const el = this.mensajesRef.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.abierto) this.cerrar();
  }
}
