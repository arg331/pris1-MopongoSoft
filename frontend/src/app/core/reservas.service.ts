import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { NuevaReserva, Reserva } from './modelos';

/** F04 · Reservar y F05 · Mis reservas / cancelación. */
@Injectable({ providedIn: 'root' })
export class ReservasService {
  private readonly http = inject(HttpClient);

  crear(reserva: NuevaReserva): Observable<Reserva> {
    return this.http.post<Reserva>('/api/reservas', reserva);
  }

  mias(): Observable<Reserva[]> {
    return this.http.get<Reserva[]>('/api/reservas/mias');
  }

  cancelar(id: number): Observable<Reserva> {
    return this.http.patch<Reserva>(`/api/reservas/${id}/cancelar`, {});
  }
}
