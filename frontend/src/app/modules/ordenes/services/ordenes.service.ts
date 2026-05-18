import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface OrdenTrabajo {
  id: number;
  equipoId: number;
  clienteNombre: string;
  clienteId: number;
  tecnicoId: number;
  tecnicoNombre: string;
  fallaReportada: string;
  diagnostico: string;
  estado: string;
  prioridad: string;
  presupuesto: number;
  createdAt: string;
  updatedAt: string;
  repuestos: OrdenRepuesto[];
}

export interface OrdenRepuesto {
  id: number;
  repuestoId: number;
  nombreRepuesto: string;
  cantidad: number;
  precioUnit: number;
  total: number;
}

@Injectable({
  providedIn: 'root'
})
export class OrdenesService {
  private apiUrl = `${environment.apiUrl}/ordenes`;

  constructor(private http: HttpClient) {}

  listarOrdenes(filtros?: any): Observable<OrdenTrabajo[]> {
    let params = new HttpParams();
    if (filtros) {
      if (filtros.estado) params = params.set('estado', filtros.estado);
      if (filtros.tecnicoId) params = params.set('tecnicoId', filtros.tecnicoId);
      if (filtros.desde) params = params.set('desde', filtros.desde);
      if (filtros.hasta) params = params.set('hasta', filtros.hasta);
    }
    return this.http.get<OrdenTrabajo[]>(this.apiUrl, { params });
  }

  listarOrdenesActivas(): Observable<OrdenTrabajo[]> {
    return this.http.get<OrdenTrabajo[]>(`${this.apiUrl}/activas`);
  }

  obtenerOrden(id: number): Observable<OrdenTrabajo> {
    return this.http.get<OrdenTrabajo>(`${this.apiUrl}/${id}`);
  }

  crearOrden(data: any): Observable<OrdenTrabajo> {
    return this.http.post<OrdenTrabajo>(this.apiUrl, data);
  }

  cambiarEstado(id: number, nuevoEstado: string): Observable<OrdenTrabajo> {
    return this.http.put<OrdenTrabajo>(`${this.apiUrl}/${id}/estado`, { nuevoEstado });
  }

  agregarDiagnostico(id: number, diagnostico: string): Observable<OrdenTrabajo> {
    return this.http.put<OrdenTrabajo>(`${this.apiUrl}/${id}/diagnostico`, { diagnostico });
  }

  agregarRepuesto(ordenId: number, data: any): Observable<OrdenTrabajo> {
    return this.http.post<OrdenTrabajo>(`${this.apiUrl}/${ordenId}/repuestos`, data);
  }

  listarRepuestosDeOrden(ordenId: number): Observable<OrdenTrabajo> {
    return this.http.get<OrdenTrabajo>(`${this.apiUrl}/${ordenId}/repuestos`);
  }
}
