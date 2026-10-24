import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { ALUMNA } from '../../core/test-datos';
import { LoginPage } from './login';

describe('LoginPage (F01)', () => {
  const login = vi.fn();

  async function crear() {
    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideRouter([]), { provide: AuthService, useValue: { login } }],
    });
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    const escribir = async (selector: string, valor: string) => {
      const input = html.querySelector<HTMLInputElement>(selector)!;
      input.value = valor;
      input.dispatchEvent(new Event('input'));
      input.dispatchEvent(new Event('blur'));
      await fixture.whenStable();
    };
    const enviar = async () => {
      html.querySelector('form')!.dispatchEvent(new Event('submit'));
      await fixture.whenStable();
    };
    return { fixture, html, escribir, enviar };
  }

  beforeEach(() => login.mockReset());

  it('no llama al servidor si el formulario es inválido y muestra los errores', async () => {
    const { html, enviar } = await crear();
    await enviar();
    expect(login).not.toHaveBeenCalled();
    expect(html.textContent).toContain('El email es obligatorio');
    expect(html.textContent).toContain('La contraseña es obligatoria');
  });

  it('valida el formato del email', async () => {
    const { html, escribir, enviar } = await crear();
    await escribir('input[type=email]', 'no-es-email');
    await escribir('input[formcontrolname=password]', 'x');
    await enviar();
    expect(login).not.toHaveBeenCalled();
    expect(html.textContent).toContain('formato válido');
  });

  it('con credenciales correctas entra y navega a instalaciones', async () => {
    login.mockReturnValue(of(ALUMNA));
    const { escribir, enviar } = await crear();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    await escribir('input[type=email]', 'alumna.demo@ual.es');
    await escribir('input[formcontrolname=password]', 'Sadual2026!');
    await enviar();

    expect(login).toHaveBeenCalledWith('alumna.demo@ual.es', 'Sadual2026!');
    expect(navegar).toHaveBeenCalledWith('/instalaciones');
  });

  it('muestra el mensaje del servidor si las credenciales no son válidas', async () => {
    login.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 401, error: { detail: 'Email o contraseña incorrectos', codigo: 'CREDENCIALES_INVALIDAS' },
    })));
    const { html, escribir, enviar } = await crear();

    await escribir('input[type=email]', 'alumna.demo@ual.es');
    await escribir('input[formcontrolname=password]', 'mala');
    await enviar();

    expect(html.querySelector('[role=alert]')?.textContent).toContain('Email o contraseña incorrectos');
    expect(html.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBe(false);
  });
});
