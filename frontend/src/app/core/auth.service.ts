import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import { LoginResponse, Usuario } from './modelos';

export const CLAVE_SESION = 'sadual.sesion';

interface Sesion {
  token: string;
  expiraEn: string;
  usuario: Usuario;
}

/**
 * F01 · Sesión del usuario. El token JWT se guarda en localStorage para sobrevivir
 * a una recarga; se descarta en cuanto caduca o el servidor responde 401.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly sesion = signal<Sesion | null>(leerSesion());

  readonly usuario = computed(() => this.sesion()?.usuario ?? null);
  readonly esAdmin = computed(() => this.usuario()?.rol === 'ADMIN');

  /** Token vigente o null (si ha caducado, cierra la sesión local). */
  get token(): string | null {
    const s = this.sesion();
    if (!s) {
      return null;
    }
    if (haCaducado(s)) {
      this.cerrarSesionLocal();
      return null;
    }
    return s.token;
  }

  estaAutenticado(): boolean {
    return this.token !== null;
  }

  login(email: string, password: string): Observable<Usuario> {
    return this.http
      .post<LoginResponse>('/api/auth/login', { email: email.trim(), password })
      .pipe(
        tap((r) => this.guardar({ token: r.token, expiraEn: r.expiraEn, usuario: r.usuario })),
        map((r) => r.usuario),
      );
  }

  logout(): void {
    if (this.token) {
      // JWT sin estado: el aviso al servidor es informativo; si falla da igual.
      this.http.post('/api/auth/logout', {}).subscribe({ error: () => undefined });
    }
    this.cerrarSesionLocal();
    void this.router.navigate(['/login']);
  }

  cerrarSesionLocal(): void {
    this.sesion.set(null);
    try {
      localStorage.removeItem(CLAVE_SESION);
    } catch {
      /* almacenamiento no disponible: no pasa nada */
    }
  }

  private guardar(sesion: Sesion): void {
    this.sesion.set(sesion);
    try {
      localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
    } catch {
      /* modo privado: la sesión vive solo en memoria */
    }
  }
}

function haCaducado(s: Sesion): boolean {
  return new Date(s.expiraEn).getTime() <= Date.now();
}

function leerSesion(): Sesion | null {
  try {
    const crudo = localStorage.getItem(CLAVE_SESION);
    if (!crudo) {
      return null;
    }
    const s = JSON.parse(crudo) as Sesion;
    if (!s?.token || !s.usuario || haCaducado(s)) {
      localStorage.removeItem(CLAVE_SESION);
      return null;
    }
    return s;
  } catch {
    return null;
  }
}
