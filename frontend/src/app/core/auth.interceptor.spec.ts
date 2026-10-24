import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService, CLAVE_SESION } from './auth.service';
import { respuestaLogin } from './test-datos';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  function configurar(conSesion: boolean): void {
    localStorage.clear();
    if (conSesion) {
      localStorage.setItem(CLAVE_SESION, JSON.stringify(respuestaLogin(new Date(Date.now() + 3_600_000))));
    }
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  }

  afterEach(() => backend.verify());

  it('añade el token a las llamadas a la API', () => {
    configurar(true);
    http.get('/api/instalaciones').subscribe();
    expect(backend.expectOne('/api/instalaciones').request.headers.get('Authorization')).toBe('Bearer token-de-prueba');
  });

  it('no envía el token al login ni a otros dominios', () => {
    configurar(true);
    http.post('/api/auth/login', {}).subscribe();
    http.get('https://fonts.googleapis.com/css').subscribe();
    expect(backend.expectOne('/api/auth/login').request.headers.has('Authorization')).toBe(false);
    expect(backend.expectOne('https://fonts.googleapis.com/css').request.headers.has('Authorization')).toBe(false);
  });

  it('sin sesión no añade cabecera', () => {
    configurar(false);
    http.get('/api/instalaciones').subscribe();
    expect(backend.expectOne('/api/instalaciones').request.headers.has('Authorization')).toBe(false);
  });

  it('un 401 cierra la sesión y lleva al login avisando de que caducó', () => {
    configurar(true);
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    let error: unknown;

    http.get('/api/reservas/mias').subscribe({ error: (e) => (error = e) });
    backend.expectOne('/api/reservas/mias').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(error).toBeTruthy();
    expect(TestBed.inject(AuthService).estaAutenticado()).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/login'], { queryParams: { caducada: 1 } });
  });

  it('un 401 del propio login no redirige (son credenciales incorrectas)', () => {
    configurar(false);
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate');
    http.post('/api/auth/login', {}).subscribe({ error: () => undefined });
    backend.expectOne('/api/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(navegar).not.toHaveBeenCalled();
  });
});
