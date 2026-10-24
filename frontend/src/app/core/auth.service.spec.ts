import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService, CLAVE_SESION } from './auth.service';
import { Usuario } from './modelos';
import { ALUMNA, respuestaLogin } from './test-datos';

const dentroDeUnaHora = () => new Date(Date.now() + 3_600_000);
const haceUnMinuto = () => new Date(Date.now() - 60_000);

function guardarSesion(expiraEn: Date, usuario: Usuario = ALUMNA): void {
  localStorage.setItem(CLAVE_SESION, JSON.stringify(respuestaLogin(expiraEn, usuario)));
}

describe('AuthService', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('login envía el email sin espacios, guarda la sesión y expone el usuario', () => {
    const auth = TestBed.inject(AuthService);
    let recibido: Usuario | undefined;

    auth.login('  alumna.demo@ual.es ', 'Sadual2026!').subscribe((u) => (recibido = u));
    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'alumna.demo@ual.es', password: 'Sadual2026!' });
    req.flush(respuestaLogin(dentroDeUnaHora()));

    expect(recibido?.rol).toBe('ALUMNADO');
    expect(auth.usuario()?.nombre).toBe('Lucía');
    expect(auth.token).toBe('token-de-prueba');
    expect(auth.estaAutenticado()).toBe(true);
    expect(JSON.parse(localStorage.getItem(CLAVE_SESION)!).token).toBe('token-de-prueba');
  });

  it('un login fallido no deja sesión', () => {
    const auth = TestBed.inject(AuthService);
    auth.login('alumna.demo@ual.es', 'mal').subscribe({ error: () => undefined });
    http.expectOne('/api/auth/login').flush({ detail: 'Email o contraseña incorrectos' }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.estaAutenticado()).toBe(false);
    expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
  });

  it('restaura una sesión guardada que no ha caducado (recarga de página)', () => {
    guardarSesion(dentroDeUnaHora());
    const auth = TestBed.inject(AuthService);
    expect(auth.estaAutenticado()).toBe(true);
    expect(auth.usuario()?.email).toBe('alumna.demo@ual.es');
  });

  it('descarta una sesión caducada', () => {
    guardarSesion(haceUnMinuto());
    const auth = TestBed.inject(AuthService);
    expect(auth.token).toBeNull();
    expect(auth.usuario()).toBeNull();
    expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
  });

  it('ignora datos corruptos en el almacenamiento', () => {
    localStorage.setItem(CLAVE_SESION, '{no es json');
    expect(TestBed.inject(AuthService).estaAutenticado()).toBe(false);
  });

  it('logout avisa al servidor, borra la sesión y vuelve al login', () => {
    guardarSesion(dentroDeUnaHora());
    const auth = TestBed.inject(AuthService);
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    auth.logout();
    http.expectOne('/api/auth/logout').flush(null);

    expect(auth.estaAutenticado()).toBe(false);
    expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
    expect(navegar).toHaveBeenCalledWith(['/login']);
  });

  it('esAdmin solo es cierto para el perfil ADMIN', () => {
    guardarSesion(dentroDeUnaHora(), { ...ALUMNA, rol: 'ADMIN' });
    expect(TestBed.inject(AuthService).esAdmin()).toBe(true);
  });
});
