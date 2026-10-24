import { registerLocaleData } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import localeEs from '@angular/common/locales/es';
import { LOCALE_ID, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { Disponibilidad } from '../../core/modelos';
import { AuthService } from '../../core/auth.service';
import { InstalacionesService } from '../../core/instalaciones.service';
import { Usuario } from '../../core/modelos';
import { ReservasService } from '../../core/reservas.service';
import { ALUMNA, EXTERNO, PADEL_1, TENIS_1, TIPOS, disponibilidadPadel, reserva } from '../../core/test-datos';
import { InstalacionesPage } from './instalaciones';

registerLocaleData(localeEs);

const HOY = '2026-10-23';

describe('InstalacionesPage (F02 + F03 + F04)', () => {
  let fixture: ComponentFixture<InstalacionesPage>;
  let html: HTMLElement;
  const api = { tipos: vi.fn(), catalogo: vi.fn(), disponibilidad: vi.fn() };
  const reservas = { crear: vi.fn() };
  const usuario = signal<Usuario | null>(ALUMNA);

  const pulsar = async (el: Element | null) => {
    expect(el).toBeTruthy();
    (el as HTMLElement).click();
    await fixture.whenStable();
  };
  const botonPorTexto = (texto: string) =>
    Array.from(html.querySelectorAll('button')).find((b) => b.textContent?.includes(texto)) ?? null;
  const franjas = () => Array.from(html.querySelectorAll<HTMLButtonElement>('button.franja'));

  beforeEach(async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 9, 23, 10, 0));
    Object.values(api).forEach((f) => f.mockReset());
    reservas.crear.mockReset();
    usuario.set(ALUMNA);
    api.tipos.mockReturnValue(of(TIPOS));
    api.catalogo.mockReturnValue(of([PADEL_1, TENIS_1]));
    api.disponibilidad.mockImplementation((_id: number, fecha: string) => of(disponibilidadPadel(fecha)));

    TestBed.configureTestingModule({
      imports: [InstalacionesPage],
      providers: [
        provideRouter([]),
        provideNativeDateAdapter(),
        { provide: LOCALE_ID, useValue: 'es' },
        { provide: InstalacionesService, useValue: api },
        { provide: ReservasService, useValue: reservas },
        { provide: AuthService, useValue: { usuario } },
      ],
    });
    fixture = TestBed.createComponent(InstalacionesPage);
    html = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => vi.useRealTimers());

  it('muestra el catálogo con tipo, ubicación y aforo', () => {
    expect(api.catalogo).toHaveBeenCalledWith(null);
    const tarjetas = html.querySelectorAll('button.instalacion');
    expect(tarjetas.length).toBe(2);
    expect(tarjetas[0].textContent).toContain('Pista de pádel 1');
    expect(tarjetas[0].textContent).toContain('Zona exterior norte');
    expect(tarjetas[0].textContent).toContain('Aforo 4');
  });

  it('filtra el catálogo por tipo', async () => {
    api.catalogo.mockReturnValue(of([TENIS_1]));
    await pulsar(Array.from(html.querySelectorAll('button.pastilla')).find((b) => b.textContent?.includes('Tenis')) ?? null);
    expect(api.catalogo).toHaveBeenLastCalledWith(2);
    expect(html.querySelectorAll('button.instalacion').length).toBe(1);
    expect(html.querySelector('button.pastilla.activa')?.textContent).toContain('Tenis');
  });

  it('al elegir una instalación muestra las franjas de hoy con su estado', async () => {
    await pulsar(html.querySelector('button.instalacion'));

    expect(api.disponibilidad).toHaveBeenCalledWith(1, HOY);
    const lista = franjas();
    expect(lista.map((b) => b.textContent?.trim().slice(0, 11))).toEqual([
      '08:00–09:30', '09:30–11:00', '11:00–12:30', '12:30–14:00',
    ]);
    expect(lista.map((b) => b.disabled)).toEqual([true, true, false, true]);
    expect(lista[3].textContent).toContain('docencia');
    expect(html.textContent).toContain('1 franja libre');
  });

  it('reserva una franja libre en dos toques y refresca la disponibilidad', async () => {
    reservas.crear.mockReturnValue(of(reserva({ id: 9, horaInicio: '11:00:00', horaFin: '12:30:00' })));
    await pulsar(html.querySelector('button.instalacion'));

    await pulsar(franjas()[2]);
    expect(html.querySelector('.confirmar')?.textContent).toContain('11:00–12:30');
    await pulsar(botonPorTexto('Confirmar reserva'));

    expect(reservas.crear).toHaveBeenCalledWith({ idInstalacion: 1, fecha: HOY, horaInicio: '11:00' });
    expect(html.querySelector('.aviso')?.textContent).toContain('Reserva confirmada');
    expect(html.querySelector('.confirmar')).toBeNull();
    expect(api.disponibilidad).toHaveBeenCalledTimes(2);
  });

  it('las franjas no libres no se pueden elegir', async () => {
    await pulsar(html.querySelector('button.instalacion'));
    franjas()[1].click(); // reservada (deshabilitada)
    await fixture.whenStable();
    expect(html.querySelector('.confirmar')).toBeNull();
  });

  it('si otra persona se adelanta, explica el conflicto y vuelve a cargar', async () => {
    reservas.crear.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { detail: 'La franja acaba de ser reservada por otra persona', codigo: 'FRANJA_OCUPADA' },
    })));
    await pulsar(html.querySelector('button.instalacion'));
    await pulsar(franjas()[2]);
    await pulsar(botonPorTexto('Confirmar reserva'));

    expect(html.querySelector('[role=alert]')?.textContent).toContain('acaba de ser reservada');
    expect(api.disponibilidad).toHaveBeenCalledTimes(2);
  });

  it('cambia de día con las flechas y no permite ir a días pasados', async () => {
    await pulsar(html.querySelector('button.instalacion'));
    const anterior = html.querySelector<HTMLButtonElement>('button[aria-label="Día anterior"]')!;
    expect(anterior.disabled).toBe(true);

    await pulsar(html.querySelector('button[aria-label="Día siguiente"]'));
    expect(api.disponibilidad).toHaveBeenLastCalledWith(1, '2026-10-24');
  });

  it('P2: las franjas a menos de 3 horas aparecen fuera de plazo y no se pueden elegir', async () => {
    const d = disponibilidadPadel(HOY);
    d.franjas[2] = { ...d.franjas[2], estado: 'FUERA_DE_PLAZO' };
    api.disponibilidad.mockReturnValue(of(d));
    await pulsar(html.querySelector('button.instalacion'));

    expect(franjas()[2].disabled).toBe(true);
    expect(franjas()[2].textContent).toContain('Fuera de plazo');
    expect(html.querySelector('.reglas')?.textContent).toContain('al menos 3 horas');
  });

  it('P4: un usuario externo consulta la disponibilidad pero no puede reservar', async () => {
    usuario.set(EXTERNO);
    await fixture.whenStable();
    expect(html.querySelector('.nota')?.textContent).toContain('solo para la comunidad universitaria');

    await pulsar(html.querySelector('button.instalacion'));
    expect(franjas()[2].disabled).toBe(true);
    franjas()[2].click();
    await fixture.whenStable();
    expect(html.querySelector('.confirmar')).toBeNull();
  });

  it('si las respuestas llegan desordenadas, muestra solo las franjas del último día pedido', async () => {
    const respuestas = new Map<string, Subject<Disponibilidad>>();
    api.disponibilidad.mockImplementation((_id: number, fecha: string) => {
      const s = new Subject<Disponibilidad>();
      respuestas.set(fecha, s);
      return s;
    });
    await pulsar(html.querySelector('button.instalacion'));                    // pide el 23
    await pulsar(html.querySelector('button[aria-label="Día siguiente"]'));    // pide el 24
    await pulsar(html.querySelector('button[aria-label="Día siguiente"]'));    // pide el 25

    const dia25 = disponibilidadPadel('2026-10-25');
    dia25.franjas = [{ horaInicio: '09:00:00', horaFin: '10:30:00', estado: 'LIBRE', motivo: null }];
    respuestas.get('2026-10-25')!.next(dia25);  // llega primero la última
    respuestas.get('2026-10-24')!.next(disponibilidadPadel('2026-10-24'));  // y después una antigua
    respuestas.get('2026-10-23')!.next(disponibilidadPadel('2026-10-23'));
    await fixture.whenStable();

    expect(franjas().map((b) => b.textContent?.trim().slice(0, 11))).toEqual(['09:00–10:30']);
    expect(html.querySelector<HTMLInputElement>('input[matinput]')!.value).toContain('25');
    expect(html.querySelector('mat-progress-bar')).toBeNull();
  });

  it('un error de una consulta antigua no borra las franjas del día actual', async () => {
    const antigua = new Subject<Disponibilidad>();
    api.disponibilidad
      .mockReturnValueOnce(antigua)
      .mockImplementation((_id: number, fecha: string) => of(disponibilidadPadel(fecha)));
    await pulsar(html.querySelector('button.instalacion'));
    await pulsar(html.querySelector('button[aria-label="Día siguiente"]'));

    antigua.error(new HttpErrorResponse({ status: 500 }));
    await fixture.whenStable();

    expect(franjas().length).toBe(4);
    expect(html.querySelector('[role=alert]')).toBeNull();
  });

  it('avisa cuando la instalación no abre ese día', async () => {
    api.disponibilidad.mockReturnValue(of({ ...disponibilidadPadel(HOY), franjas: [] }));
    await pulsar(html.querySelector('button.instalacion'));
    expect(html.textContent).toContain('no abre este día');
  });
});
