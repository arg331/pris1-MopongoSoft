import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AdminService } from './admin.service';
import { AuthService } from './auth.service';
import { Variacion7Service } from './variacion7.service';

/** Contrato con la API del Incremento 2 y del panel de administración (backend/README.md). */
describe('contrato con la API · Incremento 2', () => {
  let backend: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('P17 · POST /api/auth/registro guarda la sesión que devuelve el servidor', () => {
    const auth = TestBed.inject(AuthService);
    auth.registrar({ tipo: 'REPRESENTANTE', nombre: 'Ana', apellidos: 'Ruiz', email: 'ana@club.test',
      password: 'Segura2026!', cifEntidad: 'G00000000' }).subscribe();
    const req = backend.expectOne('/api/auth/registro');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.cifEntidad).toBe('G00000000');
    req.flush({ token: 't', expiraEn: new Date(Date.now() + 3_600_000).toISOString(), pendienteValidacion: false,
      usuario: { id: 9, nombre: 'Ana', apellidos: 'Ruiz', email: 'ana@club.test', rol: 'EXTERNO',
        rolNombre: 'Usuario externo', puedeReservar: false, representante: true } });
    expect(auth.estaAutenticado()).toBe(true);
    expect(auth.esRepresentante()).toBe(true);
    expect(auth.esExterno()).toBe(true);
  });

  it('P23 · un representante pendiente de validación no queda con la sesión iniciada', () => {
    const auth = TestBed.inject(AuthService);
    let pendiente = false;
    auth.registrar({ tipo: 'REPRESENTANTE', nombre: 'Ana', apellidos: 'Ruiz', email: 'ana@club.test',
      password: 'Segura2026!', cifEntidad: 'G00000000' }).subscribe((r) => (pendiente = r.pendienteValidacion));
    backend.expectOne('/api/auth/registro').flush({ token: null, expiraEn: null, pendienteValidacion: true,
      usuario: { id: 9, nombre: 'Ana', apellidos: 'Ruiz', email: 'ana@club.test', rol: 'EXTERNO',
        rolNombre: 'Usuario externo', puedeReservar: false, representante: true } });
    expect(pendiente).toBe(true);
    expect(auth.estaAutenticado()).toBe(false);
  });

  it('P45 · retirar el consentimiento es DELETE /api/menores/{id}', () => {
    TestBed.inject(Variacion7Service).retirarConsentimiento(3).subscribe();
    expect(backend.expectOne('/api/menores/3').request.method).toBe('DELETE');
  });

  it('P52/P56 · corrección del contacto del menor y cancelación de una escuela', () => {
    TestBed.inject(Variacion7Service).actualizarContacto(2, { telefonoTutor: '611', contactoEmergenciaNombre: 'Tía',
      contactoEmergenciaTelefono: '622' }).subscribe();
    TestBed.inject(AdminService).cancelarEscuela(5).subscribe();
    expect(backend.expectOne('/api/menores/2/contacto').request.method).toBe('PATCH');
    expect(backend.expectOne('/api/admin/escuelas/5/cancelar').request.method).toBe('PATCH');
  });

  it('P32 · suspensión de una entidad con fecha y motivo', () => {
    TestBed.inject(AdminService).suspenderEntidad(1, '2026-11-30', 'Tres ausencias').subscribe();
    expect(backend.expectOne('/api/admin/entidades/1/suspender').request.body).toEqual({ hasta: '2026-11-30', motivo: 'Tres ausencias' });
  });

  it('P21 · el representante solicita con hora de inicio y de fin', () => {
    TestBed.inject(Variacion7Service)
      .solicitarInstitucional({ idInstalacion: 5, fecha: '2026-10-30', horaInicio: '18:00', horaFin: '20:00' }).subscribe();
    const req = backend.expectOne('/api/reservas-institucionales');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ idInstalacion: 5, fecha: '2026-10-30', horaInicio: '18:00', horaFin: '20:00' });
  });

  it('P18 · inscripción (POST) y anulación (DELETE) en un evento', () => {
    const api = TestBed.inject(Variacion7Service);
    api.inscribirEnEvento(3).subscribe();
    api.anularInscripcion(3).subscribe();
    const reqs = backend.match('/api/eventos/3/inscripcion');
    expect(reqs.map((r) => r.request.method)).toEqual(['POST', 'DELETE']);
  });

  it('P19/P22 · menores e inscripción en escuelas', () => {
    const api = TestBed.inject(Variacion7Service);
    api.registrarMenor({ nombre: 'Leo', apellidos: 'Ruiz', fechaNacimiento: '2018-01-10', telefonoTutor: '600',
      contactoEmergenciaNombre: 'Abuela', contactoEmergenciaTelefono: '601', consentimientoRgpd: true }).subscribe();
    api.inscribirMenor(1, 2).subscribe();
    api.darDeBaja(1, 2).subscribe();
    expect(backend.expectOne('/api/menores').request.body.consentimientoRgpd).toBe(true);
    expect(backend.expectOne('/api/escuelas/1/inscripciones').request.body).toEqual({ idMenor: 2 });
    expect(backend.expectOne('/api/escuelas/1/inscripciones/2/baja').request.method).toBe('PATCH');
  });

  it('P21 · la administración autoriza y rechaza con motivo', () => {
    const api = TestBed.inject(AdminService);
    api.institucionales('PENDIENTE').subscribe();
    api.autorizar(4).subscribe();
    api.rechazar(4, 'Campo ocupado').subscribe();
    expect(backend.expectOne((r) => r.url === '/api/admin/reservas-institucionales'
      && r.params.get('estado') === 'PENDIENTE')).toBeTruthy();
    expect(backend.expectOne('/api/admin/reservas-institucionales/4/autorizar').request.method).toBe('PATCH');
    expect(backend.expectOne('/api/admin/reservas-institucionales/4/rechazar').request.body).toEqual({ motivo: 'Campo ocupado' });
  });

  it('F10 · la consulta general no envía filtros vacíos', () => {
    TestBed.inject(AdminService).reservas({ desde: '2026-10-27', hasta: '', idInstalacion: null, estado: null, texto: '' })
      .subscribe();
    const req = backend.expectOne((r) => r.url === '/api/admin/reservas');
    expect(req.request.params.keys()).toEqual(['desde']);
  });

  it('F06/F07/F08 · rutas de activación, bloqueos y cierres', () => {
    const api = TestBed.inject(AdminService);
    api.activarInstalacion(2, false).subscribe();
    api.activarUsuario(7, true).subscribe();
    api.eliminarBloqueo(11).subscribe();
    api.cerrar({ inicio: '2026-12-25T00:00:00', fin: '2026-12-26T00:00:00', observaciones: 'Navidad' }).subscribe();
    expect(backend.expectOne('/api/admin/instalaciones/2/desactivar').request.method).toBe('PATCH');
    expect(backend.expectOne('/api/admin/usuarios/7/activar').request.method).toBe('PATCH');
    expect(backend.expectOne('/api/admin/bloqueos/11').request.method).toBe('DELETE');
    expect(backend.expectOne('/api/admin/cierres').request.method).toBe('POST');
  });

  it('P9 · informe semanal o mensual con fecha de referencia', () => {
    TestBed.inject(AdminService).informe('MENSUAL', '2026-10-05').subscribe();
    const req = backend.expectOne((r) => r.url === '/api/admin/informes');
    expect(req.request.params.get('periodo')).toBe('MENSUAL');
    expect(req.request.params.get('fecha')).toBe('2026-10-05');
  });
});
