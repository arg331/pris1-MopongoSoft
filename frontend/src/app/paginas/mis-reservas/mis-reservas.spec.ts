import { registerLocaleData } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import localeEs from '@angular/common/locales/es';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ReservasService } from '../../core/reservas.service';
import { reserva } from '../../core/test-datos';
import { MisReservasPage } from './mis-reservas';

registerLocaleData(localeEs);

describe('MisReservasPage (F05)', () => {
  let fixture: ComponentFixture<MisReservasPage>;
  let html: HTMLElement;
  const api = { mias: vi.fn(), cancelar: vi.fn() };

  const proxima = reserva({ id: 1, fecha: '2026-10-27', instalacion: 'Pista de pádel 1' });
  const otraProxima = reserva({ id: 5, fecha: '2026-10-26', instalacion: 'Pista de tenis 2', horaInicio: '18:00:00', horaFin: '19:00:00' });
  const cancelada = reserva({ id: 2, fecha: '2026-10-20', estado: 'CANCELADA', cancelable: false, instalacion: 'Pabellón cubierto' });

  const boton = (texto: string) =>
    Array.from(html.querySelectorAll('button')).find((b) => b.textContent?.trim() === texto) ?? null;
  const pulsar = async (el: HTMLElement | null) => {
    expect(el).toBeTruthy();
    el!.click();
    await fixture.whenStable();
  };

  async function crear() {
    TestBed.configureTestingModule({
      imports: [MisReservasPage],
      providers: [provideRouter([]), { provide: LOCALE_ID, useValue: 'es' }, { provide: ReservasService, useValue: api }],
    });
    fixture = TestBed.createComponent(MisReservasPage);
    html = fixture.nativeElement;
    await fixture.whenStable();
  }

  beforeEach(() => {
    api.mias.mockReset().mockReturnValue(of([proxima, cancelada, otraProxima]));
    api.cancelar.mockReset();
  });

  it('separa las próximas (ordenadas por fecha) del historial', async () => {
    await crear();
    const articulos = Array.from(html.querySelectorAll('article'));
    expect(articulos.map((a) => a.getAttribute('aria-label'))).toEqual([
      'Reserva Pista de tenis 2', 'Reserva Pista de pádel 1', 'Reserva Pabellón cubierto',
    ]);
    expect(articulos[2].classList).toContain('pasada');
    expect(articulos[2].textContent).toContain('Cancelada');
  });

  it('cancelar pide confirmación y mueve la reserva al historial', async () => {
    api.cancelar.mockReturnValue(of({ ...otraProxima, estado: 'CANCELADA', cancelable: false }));
    await crear();

    await pulsar(boton('Cancelar'));
    expect(api.cancelar).not.toHaveBeenCalled();
    await pulsar(boton('Sí, cancelar'));

    expect(api.cancelar).toHaveBeenCalledWith(5);
    expect(html.querySelector('[role=status]')?.textContent).toContain('vuelve a estar libre');
    expect(html.querySelectorAll('article.pasada').length).toBe(2);
  });

  it('se puede desistir de cancelar', async () => {
    await crear();
    await pulsar(boton('Cancelar'));
    await pulsar(boton('No'));
    expect(api.cancelar).not.toHaveBeenCalled();
    expect(boton('Sí, cancelar')).toBeNull();
  });

  it('si el servidor rechaza la cancelación muestra el motivo y recarga', async () => {
    api.cancelar.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 422, error: { detail: 'No se puede cancelar una reserva que ya ha empezado', codigo: 'RESERVA_INICIADA' },
    })));
    await crear();
    await pulsar(boton('Cancelar'));
    await pulsar(boton('Sí, cancelar'));

    expect(html.querySelector('[role=alert]')?.textContent).toContain('ya ha empezado');
    expect(api.mias).toHaveBeenCalledTimes(2);
  });

  it('tras un error, el mensaje desaparece al cancelar otra reserva con éxito', async () => {
    api.cancelar
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({
        status: 422, error: { detail: 'No se puede cancelar una reserva que ya ha empezado', codigo: 'RESERVA_INICIADA' },
      })))
      .mockReturnValueOnce(of({ ...proxima, estado: 'CANCELADA', cancelable: false }));
    await crear();

    await pulsar(boton('Cancelar'));
    await pulsar(boton('Sí, cancelar'));
    expect(html.querySelector('[role=alert]')).not.toBeNull();

    await pulsar(boton('Cancelar'));
    expect(html.querySelector('[role=alert]')).toBeNull(); // nueva acción: se limpia el error anterior
    await pulsar(boton('Sí, cancelar'));

    expect(html.querySelector('[role=alert]')).toBeNull();
    expect(html.querySelector('[role=status]')?.textContent).toContain('vuelve a estar libre');
  });

  it('sin reservas próximas lo indica', async () => {
    api.mias.mockReturnValue(of([]));
    await crear();
    expect(html.textContent).toContain('No tienes reservas próximas');
  });
});
