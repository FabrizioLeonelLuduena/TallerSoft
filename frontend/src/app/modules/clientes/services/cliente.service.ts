import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { environment } from '@environments/environment';

export interface ClienteRequest {
  nombre: string;
  telefono?: string | null;
  email?: string | null;
  direccion?: string | null;
}

export interface ClienteResponse {
  id: number;
  nombre: string;
  telefono: string | null;
  email: string | null;
  direccion: string | null;
  activo: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private api = `${environment.apiUrl}/api/clientes`;

  constructor(private http: HttpClient) {}

  listarClientes(nombre?: string): Observable<ClienteResponse[]> {
    console.log('[ClienteService] listarClientes() called with nombre:', nombre);
    let params = new HttpParams();
    if (nombre) {
      params = params.set('nombre', nombre);
    }

    const url = `${this.api}`;
    console.log('[ClienteService] Making request to:', url, 'with params:', params.toString());
    
    return this.http.get<ClienteResponse[]>(url, { params }).pipe(
      tap(response => console.log('[ClienteService] Response received:', response)),
      catchError(err => {
        console.error('[ClienteService] HTTP Error:', err);
        throw err;
      })
    );
  }

  obtenerCliente(id: number): Observable<ClienteResponse> {
    return this.http.get<ClienteResponse>(`${this.api}/${id}`);
  }

  crearCliente(data: ClienteRequest): Observable<ClienteResponse> {
    return this.http.post<ClienteResponse>(this.api, data);
  }

  editarCliente(id: number, data: ClienteRequest): Observable<ClienteResponse> {
    return this.http.put<ClienteResponse>(`${this.api}/${id}`, data);
  }

  eliminarCliente(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
