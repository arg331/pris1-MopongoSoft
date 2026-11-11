import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AltaUsuario,
  Bloqueo,
  Cierre,
  CodigoRol,
  EntidadAdmin,
  EntidadForm,
  EstadoReserva,
  Escuela,
  Evento,
  FiltroReservas,
  Informe,
  InscritoEscuela,
  InscritoEvento,
  InstalacionAdmin,
  InstalacionForm,
  NuevaEscuela,
  NuevoBloqueo,
  NuevoEvento,
  Reserva,
  ReservaAdmin,
  ReservaInstitucional,
  ResultadoBloqueo,
  ResultadoEscuela,
  ResultadoEvento,
  UsuarioAdmin,
} from './modelos';

/** Quita los parámetros vacíos para no enviar «?texto=&estado=». */
function parametros(valores: Record<string, string | number | null | undefined>): HttpParams {
  let params = new HttpParams();
  for (const [clave, valor] of Object.entries(valores)) {
    if (valor !== null && valor !== undefined && valor !== '') {
      params = params.set(clave, valor);
    }
  }
  return params;
}

/**
 * Panel de administración (solo perfil ADMIN): núcleo común (F06, F07, F08, F10, F12), Variación 7
 * (P18, P19, P21) e informes (P9). Contrato: backend/README.md.
 */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  // ---------------------------------------------------------------- F06 · instalaciones

  instalaciones(): Observable<InstalacionAdmin[]> {
    return this.http.get<InstalacionAdmin[]>('/api/admin/instalaciones');
  }

  crearInstalacion(datos: InstalacionForm): Observable<InstalacionAdmin> {
    return this.http.post<InstalacionAdmin>('/api/admin/instalaciones', datos);
  }

  editarInstalacion(id: number, datos: InstalacionForm): Observable<InstalacionAdmin> {
    return this.http.put<InstalacionAdmin>(`/api/admin/instalaciones/${id}`, datos);
  }

  activarInstalacion(id: number, activa: boolean): Observable<InstalacionAdmin> {
    return this.http.patch<InstalacionAdmin>(`/api/admin/instalaciones/${id}/${activa ? 'activar' : 'desactivar'}`, {});
  }

  // ---------------------------------------------------------------- F07 · usuarios

  usuarios(rol?: string | null, texto?: string | null): Observable<UsuarioAdmin[]> {
    return this.http.get<UsuarioAdmin[]>('/api/admin/usuarios', { params: parametros({ rol, texto }) });
  }

  altaUsuario(datos: AltaUsuario): Observable<UsuarioAdmin> {
    return this.http.post<UsuarioAdmin>('/api/admin/usuarios', datos);
  }

  cambiarRol(id: number, rol: CodigoRol): Observable<UsuarioAdmin> {
    return this.http.patch<UsuarioAdmin>(`/api/admin/usuarios/${id}/rol`, { rol });
  }

  activarUsuario(id: number, activo: boolean): Observable<UsuarioAdmin> {
    return this.http.patch<UsuarioAdmin>(`/api/admin/usuarios/${id}/${activo ? 'activar' : 'desactivar'}`, {});
  }

  // ---------------------------------------------------------------- F08 · bloqueos y cierres

  bloqueos(idInstalacion?: number | null, desde?: string, hasta?: string): Observable<Bloqueo[]> {
    return this.http.get<Bloqueo[]>('/api/admin/bloqueos', { params: parametros({ idInstalacion, desde, hasta }) });
  }

  crearBloqueo(datos: NuevoBloqueo): Observable<ResultadoBloqueo> {
    return this.http.post<ResultadoBloqueo>('/api/admin/bloqueos', datos);
  }

  eliminarBloqueo(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/bloqueos/${id}`);
  }

  cerrar(datos: Cierre): Observable<ResultadoBloqueo> {
    return this.http.post<ResultadoBloqueo>('/api/admin/cierres', datos);
  }

  // ---------------------------------------------------------------- F10 · consulta general, F12

  reservas(filtro: FiltroReservas): Observable<ReservaAdmin[]> {
    return this.http.get<ReservaAdmin[]>('/api/admin/reservas', {
      params: parametros({ ...filtro, estado: filtro.estado as EstadoReserva | null }),
    });
  }

  noPresentado(id: number): Observable<Reserva> {
    return this.http.patch<Reserva>(`/api/admin/reservas/${id}/no-presentado`, {});
  }

  cancelarReserva(id: number): Observable<Reserva> {
    return this.http.patch<Reserva>(`/api/reservas/${id}/cancelar`, {});
  }

  // ---------------------------------------------------------------- P21 · institucionales

  institucionales(estado?: EstadoReserva | null): Observable<ReservaInstitucional[]> {
    return this.http.get<ReservaInstitucional[]>('/api/admin/reservas-institucionales', {
      params: parametros({ estado }),
    });
  }

  autorizar(id: number): Observable<Reserva> {
    return this.http.patch<Reserva>(`/api/admin/reservas-institucionales/${id}/autorizar`, {});
  }

  rechazar(id: number, motivo: string): Observable<Reserva> {
    return this.http.patch<Reserva>(`/api/admin/reservas-institucionales/${id}/rechazar`, { motivo });
  }

  // ---------------------------------------------------------------- P18 · eventos

  eventos(): Observable<Evento[]> {
    return this.http.get<Evento[]>('/api/admin/eventos');
  }

  crearEvento(datos: NuevoEvento): Observable<ResultadoEvento> {
    return this.http.post<ResultadoEvento>('/api/admin/eventos', datos);
  }

  publicarEvento(id: number): Observable<Evento> {
    return this.http.patch<Evento>(`/api/admin/eventos/${id}/publicar`, {});
  }

  cancelarEvento(id: number): Observable<ResultadoEvento> {
    return this.http.patch<ResultadoEvento>(`/api/admin/eventos/${id}/cancelar`, {});
  }

  inscritosEvento(id: number): Observable<InscritoEvento[]> {
    return this.http.get<InscritoEvento[]>(`/api/admin/eventos/${id}/inscritos`);
  }

  // ---------------------------------------------------------------- P19 · escuelas

  escuelas(): Observable<Escuela[]> {
    return this.http.get<Escuela[]>('/api/admin/escuelas');
  }

  crearEscuela(datos: NuevaEscuela): Observable<ResultadoEscuela> {
    return this.http.post<ResultadoEscuela>('/api/admin/escuelas', datos);
  }

  /** ❓P56: libera las sesiones futuras y da de baja a los inscritos. */
  cancelarEscuela(id: number): Observable<ResultadoEscuela> {
    return this.http.patch<ResultadoEscuela>(`/api/admin/escuelas/${id}/cancelar`, {});
  }

  inscritosEscuela(id: number): Observable<InscritoEscuela[]> {
    return this.http.get<InscritoEscuela[]>(`/api/admin/escuelas/${id}/inscritos`);
  }

  // ---------------------------------------------------------------- P25/P32 · entidades con convenio

  entidades(): Observable<EntidadAdmin[]> {
    return this.http.get<EntidadAdmin[]>('/api/admin/entidades');
  }

  crearEntidad(datos: EntidadForm): Observable<EntidadAdmin> {
    return this.http.post<EntidadAdmin>('/api/admin/entidades', datos);
  }

  editarEntidad(id: number, datos: EntidadForm): Observable<EntidadAdmin> {
    return this.http.put<EntidadAdmin>(`/api/admin/entidades/${id}`, datos);
  }

  suspenderEntidad(id: number, hasta: string, motivo: string): Observable<EntidadAdmin> {
    return this.http.patch<EntidadAdmin>(`/api/admin/entidades/${id}/suspender`, { hasta, motivo });
  }

  levantarSuspension(id: number): Observable<EntidadAdmin> {
    return this.http.patch<EntidadAdmin>(`/api/admin/entidades/${id}/levantar-suspension`, {});
  }

  // ---------------------------------------------------------------- P9 · informes

  informe(periodo: 'SEMANAL' | 'MENSUAL', fecha?: string): Observable<Informe> {
    return this.http.get<Informe>('/api/admin/informes', { params: parametros({ periodo, fecha }) });
  }
}
