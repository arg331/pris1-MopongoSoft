import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Disponibilidad, Instalacion, TipoInstalacion } from './modelos';

/** F02 · Catálogo y F03 · Disponibilidad. */
@Injectable({ providedIn: 'root' })
export class InstalacionesService {
  private readonly http = inject(HttpClient);

  tipos(): Observable<TipoInstalacion[]> {
    return this.http.get<TipoInstalacion[]>('/api/tipos-instalacion');
  }

  catalogo(idTipo?: number | null): Observable<Instalacion[]> {
    const params = idTipo != null ? new HttpParams().set('tipo', idTipo) : undefined;
    return this.http.get<Instalacion[]>('/api/instalaciones', { params });
  }

  disponibilidad(idInstalacion: number, fecha: string): Observable<Disponibilidad> {
    return this.http.get<Disponibilidad>(`/api/instalaciones/${idInstalacion}/disponibilidad`, {
      params: new HttpParams().set('fecha', fecha),
    });
  }
}
