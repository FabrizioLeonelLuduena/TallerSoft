import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@environments/environment';

export interface RepuestoResponse {
  id: number;
  nombre: string;
  categoria: string | null;
  precio: number;
  stockActual: number;
  stockMinimo: number;
  critico: boolean;
  createdAt?: string;
}

export interface RepuestoRequest {
  nombre: string;
  categoria: string | null;
  precio: number;
  stockActual: number;
  stockMinimo: number;
}

// Alias used by stock module components
export type Repuesto = RepuestoResponse;

@Injectable({ providedIn: 'root' })
export class RepuestosService {
  private api = `${environment.apiUrl}/api/repuestos`;

  constructor(private http: HttpClient) {}

  listarRepuestos(critico?: boolean): Observable<RepuestoResponse[]> {
    let params = new HttpParams();
    if (critico === true) {
      params = params.set('critico', 'true');
    }
    return this.http.get<RepuestoResponse[]>(this.api, { params });
  }

  buscarRepuestos(nombre: string): Observable<RepuestoResponse[]> {
    let params = new HttpParams();
    if (nombre) {
      params = params.set('nombre', nombre);
    }
    return this.http.get<RepuestoResponse[]>(this.api, { params });
  }

  obtenerRepuesto(id: number): Observable<RepuestoResponse> {
    return this.http.get<RepuestoResponse>(`${this.api}/${id}`);
  }

  crearRepuesto(request: RepuestoRequest): Observable<RepuestoResponse> {
    return this.http.post<RepuestoResponse>(this.api, request);
  }

  editarRepuesto(id: number, request: RepuestoRequest): Observable<RepuestoResponse> {
    return this.http.put<RepuestoResponse>(`${this.api}/${id}`, request);
  }
}
