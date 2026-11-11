import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { adminGuard, authGuard, invitadoGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('guards de sesión', () => {
  let autenticado: boolean;
  let admin: boolean;

  beforeEach(() => {
    autenticado = false;
    admin = false;
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: { estaAutenticado: () => autenticado, esAdmin: () => admin } }],
    });
  });

  const ejecutar = (guard: typeof authGuard, url = '/mis-reservas') =>
    TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    );

  it('authGuard manda al login recordando la ruta pedida', () => {
    const resultado = ejecutar(authGuard) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(resultado)).toBe('/login?volver=%2Fmis-reservas');
  });

  it('authGuard deja pasar con sesión', () => {
    autenticado = true;
    expect(ejecutar(authGuard)).toBe(true);
  });

  it('invitadoGuard saca del login a quien ya ha iniciado sesión', () => {
    autenticado = true;
    const resultado = ejecutar(invitadoGuard) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(resultado)).toBe('/instalaciones');
  });

  it('adminGuard solo deja entrar al perfil de administración', () => {
    autenticado = true;
    const resultado = ejecutar(adminGuard, '/admin') as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(resultado)).toBe('/instalaciones');
    admin = true;
    expect(ejecutar(adminGuard, '/admin')).toBe(true);
  });
});
