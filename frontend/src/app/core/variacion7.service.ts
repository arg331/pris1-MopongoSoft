import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Escuela, Evento, Menor, NuevaReservaInstitucional, NuevoMenor, Reserva } from './modelos';

/**
 * Variación 7 vista por los usuarios (Incremento 2): reservas institucionales (P21), eventos
 * masivos (P18) y escuelas con sus menores (P19, P20, P22). Contrato: backend/README.md.
 */
@Injectable({ providedIn: 'root' })
export class Variacion7Service {
  private readonly http = inject(HttpClient);

  // ---------------------------------------------------------------- P21 · institucionales

  solicitarInstitucional(peticion: NuevaReservaInstitucional): Observable<Reserva> {
    return this.http.post<Reserva>('/api/reservas-institucionales', peticion);
  }

  // ---------------------------------------------------------------- P18 · eventos

  eventos(): Observable<Evento[]> {
    return this.http.get<Evento[]>('/api/eventos');
  }

  misEventos(): Observable<Evento[]> {
    return this.http.get<Evento[]>('/api/eventos/mios');
  }

  inscribirEnEvento(id: number): Observable<Evento> {
    return this.http.post<Evento>(`/api/eventos/${id}/inscripcion`, {});
  }

  anularInscripcion(id: number): Observable<Evento> {
    return this.http.delete<Evento>(`/api/eventos/${id}/inscripcion`);
  }

  // ---------------------------------------------------------------- P19, P20, P22 · escuelas

  escuelas(): Observable<Escuela[]> {
    return this.http.get<Escuela[]>('/api/escuelas');
  }

  menores(): Observable<Menor[]> {
    return this.http.get<Menor[]>('/api/menores');
  }

  registrarMenor(menor: NuevoMenor): Observable<Menor> {
    return this.http.post<Menor>('/api/menores', menor);
  }

  /** ❓P52: el tutor corrige el teléfono y el contacto de emergencia del menor. */
  actualizarContacto(idMenor: number, contacto: Pick<NuevoMenor, 'telefonoTutor' | 'contactoEmergenciaNombre'
    | 'contactoEmergenciaTelefono'>): Observable<Menor> {
    return this.http.patch<Menor>(`/api/menores/${idMenor}/contacto`, contacto);
  }

  /** P45: retirar el consentimiento elimina los datos del menor y sus inscripciones. */
  retirarConsentimiento(idMenor: number): Observable<void> {
    return this.http.delete<void>(`/api/menores/${idMenor}`);
  }

  inscribirMenor(idEscuela: number, idMenor: number): Observable<Menor> {
    return this.http.post<Menor>(`/api/escuelas/${idEscuela}/inscripciones`, { idMenor });
  }

  darDeBaja(idEscuela: number, idMenor: number): Observable<Menor> {
    return this.http.patch<Menor>(`/api/escuelas/${idEscuela}/inscripciones/${idMenor}/baja`, {});
  }
}
