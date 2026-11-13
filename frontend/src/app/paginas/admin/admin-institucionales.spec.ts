import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AdminService } from '../../core/admin.service';
import { ReservaInstitucional } from '../../core/modelos';
import { AdminInstitucionalesPage } from './admin-institucionales';

const pendiente: ReservaInstitucional = {
  id: 4, idInstalacion: 5, instalacion: 'Campo de fútbol 7', fecha: '2099-10-29', horaInicio: '19:00:00',
  horaFin: '21:00:00', estado: 'PENDIENTE', idEntidad: 1, entidad: 'Club Deportivo Demo Almería',
  representante: 'Pablo Romero Sáez', emailRepresentante: 'externo.demo@correo.test', autorizador: null,
  fechaCreacion: '2026-10-20T10:00:00', fechaResolucion: null, motivoRechazo: null,
};

describe('AdminInstitucionalesPage (P21)', () => {
  let fixture: ComponentFixture<AdminInstitucionalesPage>;
  let html: HTMLElement;
  const api = { institucionales: vi.fn(), autorizar: vi.fn(), rechazar: vi.fn() };

  const boton = (texto: string) =>
    Array.from(html.querySelectorAll('button')).find((b) => b.textContent?.trim() === texto) ?? null;

  beforeEach(async () => {
    api.institucionales.mockReset().mockReturnValue(of([pendiente]));
    api.autorizar.mockReset().mockReturnValue(of({}));
    api.rechazar.mockReset().mockReturnValue(of({}));
    TestBed.configureTestingModule({ imports: [AdminInstitucionalesPage], providers: [{ provide: AdminService, useValue: api }] });
    fixture = TestBed.createComponent(AdminInstitucionalesPage);
    html = fixture.nativeElement;
    await fixture.whenStable();
  });

  it('lista las pendientes con su entidad y representante', () => {
    expect(api.institucionales).toHaveBeenCalledWith('PENDIENTE');
    expect(html.textContent).toContain('Club Deportivo Demo Almería · Campo de fútbol 7');
    expect(html.textContent).toContain('Pablo Romero Sáez');
  });

  it('autoriza una solicitud', async () => {
    boton('Autorizar')!.click();
    await fixture.whenStable();
    expect(api.autorizar).toHaveBeenCalledWith(4);
    expect(html.querySelector('[role=status]')?.textContent).toContain('autorizada');
  });

  it('para rechazar exige escribir un motivo', async () => {
    boton('Rechazar…')!.click();
    await fixture.whenStable();
    const rechazar = boton('Rechazar')!;
    expect(rechazar.disabled).toBe(true);
    const input = html.querySelector('input') as HTMLInputElement;
    input.value = 'Campo reservado para la liga';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(rechazar.disabled).toBe(false);
    rechazar.click();
    await fixture.whenStable();
    expect(api.rechazar).toHaveBeenCalledWith(4, 'Campo reservado para la liga');
  });

  it('una solicitud cuya fecha ya ha pasado no se puede autorizar', async () => {
    api.institucionales.mockReturnValue(of([{ ...pendiente, id: 9, fecha: '2020-01-01' }]));
    (fixture.componentInstance as unknown as { cargar: () => void }).cargar();
    await fixture.whenStable();
    expect(boton('Autorizar')).toBeNull();
    expect(html.textContent).toContain('La fecha ya ha pasado');
  });
});
