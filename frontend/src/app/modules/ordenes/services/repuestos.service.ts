import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Repuesto {
  id: number;
  nombre: string;
  categoria: string;
  precio: number;
  stockActual: number;
  stockMinimo: number;
  critico: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class RepuestosService {
  private apiUrl = `${environment.apiUrl}/repuestos`;

  constructor(private http: HttpClient) {}

  listarRepuestos(critico?: boolean): Observable<Repuesto[]> {
    let params = new HttpParams();
    if (critico) params = params.set('critico', 'true');
    return this.http.get<Repuesto[]>(this.apiUrl, { params });
  }

  obtenerRepuesto(id: number): Observable<Repuesto> {
    return this.http.get<Repuesto>(`${this.apiUrl}/${id}`);
  }

  crearRepuesto(data: any): Observable<Repuesto> {
    return this.http.post<Repuesto>(this.apiUrl, data);
  }

  editarRepuesto(id: number, data: any): Observable<Repuesto> {
    return this.http.put<Repuesto>(`${this.apiUrl}/${id}`, data);
  }
}
