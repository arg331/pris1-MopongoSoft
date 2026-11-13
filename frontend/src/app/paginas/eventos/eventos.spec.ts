import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { Evento } from '../../core/modelos';
import { Variacion7Service } from '../../core/variacion7.service';
import { EventosPage } from './eventos';

const evento = (cambios: Partial<Evento> = {}): Evento => ({
  id: 1, nombre: 'Jornada de puertas abiertas', descripcion: null, inicio: '2026-12-19T09:00:00',
  fin: '2026-12-19T14:00:00', aforoMaximo: 200, estado: 'PUBLICADO', inscritos: 1, plazasLibres: 199,
  instalaciones: ['Pabellón cubierto', 'Sala multifunción A'], inscrito: false, publico: 'COMUNIDAD_Y_EXTERNOS',
  cierreInscripcion: '2099-12-18T09:00:00', ...cambios,
});

describe('EventosPage (P18)', () => {
  let fixture: ComponentFixture<EventosPage>;
  let html: HTMLElement;
  const api = { eventos: vi.fn(), inscribirEnEvento: vi.fn(), anularInscripcion: vi.fn() };
  let esAdmin = false;

  const boton = (texto: string) =>
    Array.from(html.querySelectorAll('button')).find((b) => b.textContent?.includes(texto)) ?? null;

  async function crear() {
    TestBed.configureTestingModule({
      imports: [EventosPage],
      providers: [{ provide: Variacion7Service, useValue: api }, { provide: AuthService, useValue: { esAdmin: () => esAdmin } }],
    });
    fixture = TestBed.createComponent(EventosPage);
    html = fixture.nativeElement;
    await fixture.whenStable();
  }

  beforeEach(() => {
    esAdmin = false;
    api.eventos.mockReset().mockReturnValue(of([evento()]));
    api.inscribirEnEvento.mockReset();
    api.anularInscripcion.mockReset();
  });

  it('muestra el evento, sus instalaciones y las plazas libres', async () => {
    await crear();
    expect(html.textContent).toContain('Jornada de puertas abiertas');
    expect(html.textContent).toContain('Pabellón cubierto, Sala multifunción A');
    expect(html.textContent).toContain('199 plazas libres de 200');
  });

  it('al inscribirse marca el evento como inscrito', async () => {
    api.inscribirEnEvento.mockReturnValue(of(evento({ inscrito: true, inscritos: 2, plazasLibres: 198 })));
    await crear();
    boton('Inscribirme')!.click();
    await fixture.whenStable();
    expect(api.inscribirEnEvento).toHaveBeenCalledWith(1);
    expect(html.textContent).toContain('Inscrito');
    expect(boton('Anular inscripción')).toBeTruthy();
  });

  it('muestra el error del servidor si el aforo está completo', async () => {
    api.inscribirEnEvento.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { codigo: 'AFORO_COMPLETO', detail: 'El evento ha completado su aforo' } })));
    await crear();
    boton('Inscribirme')!.click();
    await fixture.whenStable();
    expect(html.querySelector('[role=alert]')?.textContent).toContain('El evento ha completado su aforo');
  });

  it('la administración ve los eventos pero no se inscribe', async () => {
    esAdmin = true;
    await crear();
    expect(boton('Inscribirme')).toBeNull();
  });

  it('P35: con las inscripciones cerradas no se puede inscribir', async () => {
    api.eventos.mockReturnValue(of([evento({ cierreInscripcion: '2020-01-01T09:00:00' })]));
    await crear();
    expect(boton('Inscribirme')).toBeNull();
    expect(html.textContent).toContain('Inscripciones cerradas');
  });
});
