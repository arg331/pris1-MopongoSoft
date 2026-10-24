import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { InstalacionesService } from './instalaciones.service';
import { ReservasService } from './reservas.service';

/** Comprueba que el frontend respeta el contrato de la API (backend/README.md). */
describe('contrato con la API', () => {
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('F02 · GET /api/instalaciones con y sin filtro de tipo', () => {
    const api = TestBed.inject(InstalacionesService);
    api.catalogo().subscribe();
    api.catalogo(1).subscribe();
    const sinFiltro = backend.expectOne((r) => r.url === '/api/instalaciones' && !r.params.has('tipo'));
    const conFiltro = backend.expectOne((r) => r.url === '/api/instalaciones' && r.params.get('tipo') === '1');
    expect(sinFiltro.request.method).toBe('GET');
    expect(conFiltro.request.method).toBe('GET');
  });

  it('F03 · GET /api/instalaciones/{id}/disponibilidad?fecha=yyyy-MM-dd', () => {
    TestBed.inject(InstalacionesService).disponibilidad(3, '2026-10-27').subscribe();
    const req = backend.expectOne((r) => r.url === '/api/instalaciones/3/disponibilidad');
    expect(req.request.params.get('fecha')).toBe('2026-10-27');
  });

  it('F04 · POST /api/reservas solo con instalación, fecha y hora de inicio', () => {
    TestBed.inject(ReservasService).crear({ idInstalacion: 1, fecha: '2026-10-27', horaInicio: '11:00' }).subscribe();
    const req = backend.expectOne('/api/reservas');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ idInstalacion: 1, fecha: '2026-10-27', horaInicio: '11:00' });
  });

  it('F05 · GET /api/reservas/mias y PATCH /api/reservas/{id}/cancelar', () => {
    const api = TestBed.inject(ReservasService);
    api.mias().subscribe();
    api.cancelar(7).subscribe();
    expect(backend.expectOne('/api/reservas/mias').request.method).toBe('GET');
    expect(backend.expectOne('/api/reservas/7/cancelar').request.method).toBe('PATCH');
  });
});
