import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';
import { Disponibilidad, Instalacion, TipoInstalacion } from './modelos';

/** Resumen de un día para los badges del catálogo (se calcula con F03, sin endpoint nuevo). */
export interface ResumenDia {
  total: number;
  libres: number;
  /** Franjas reservadas por otras personas: distingue "completa" de "ya no quedan franjas". */
  reservadas: number;
  cerrada: boolean;
}

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

  /**
   * Franjas libres de cada instalación en una fecha (badges del catálogo). Si una consulta
   * falla, esa instalación simplemente no muestra badge: no bloquea el resto.
   */
  resumenDelDia(ids: number[], fecha: string): Observable<Record<number, ResumenDia>> {
    if (ids.length === 0) {
      return of({});
    }
    return forkJoin(
      ids.map((id) =>
        this.disponibilidad(id, fecha).pipe(
          map((d): [number, ResumenDia | null] => [id, {
            total: d.franjas.length,
            libres: d.franjas.filter((f) => f.estado === 'LIBRE').length,
            reservadas: d.franjas.filter((f) => f.estado === 'RESERVADA').length,
            cerrada: d.franjas.length === 0 || d.franjas.every((f) => f.motivo === 'CIERRE'),
          }]),
          catchError(() => of<[number, ResumenDia | null]>([id, null])),
        ),
      ),
    ).pipe(
      map((pares) => {
        const r: Record<number, ResumenDia> = {};
        for (const [id, resumen] of pares) {
          if (resumen) {
            r[id] = resumen;
          }
        }
        return r;
      }),
    );
  }
}
