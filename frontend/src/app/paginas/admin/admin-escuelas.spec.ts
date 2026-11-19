import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AdminService } from '../../core/admin.service';
import { InstalacionesService } from '../../core/instalaciones.service';
import { Escuela, InscritoEscuela } from '../../core/modelos';
import { AdminEscuelasPage } from './admin-escuelas';

const escuela: Escuela = {
  id: 1, nombre: 'Escuela de natación infantil', temporada: '2026-2027', fechaInicio: '2026-10-05',
  fechaFin: '2027-06-20', idInstalacion: 9, instalacion: 'Piscina 2', edadMin: 6, edadMax: 12, plazas: 12,
  inscritos: 1, plazasLibres: 11, diasHorario: null, horario: [], estado: 'ACTIVA',
};

const hugo: InscritoEscuela = {
  idMenor: 1, menor: 'Hugo Ruiz Campos', fechaNacimiento: '2017-03-15', tutor: 'Laura Campos Gil',
  emailTutor: 'tutor.demo@correo.test', telefonoTutor: '600000006',
  contactoEmergenciaNombre: 'Antonio Ruiz Gómez (abuelo)', contactoEmergenciaTelefono: '600000007',
  fechaInscripcion: '2026-10-20T18:05:00', estado: 'ACTIVA',
};

describe('AdminEscuelasPage (P20, P52)', () => {
  let fixture: ComponentFixture<AdminEscuelasPage>;
  let html: HTMLElement;
  const api = { escuelas: vi.fn(), inscritosEscuela: vi.fn() };

  const boton = (texto: string) =>
    Array.from(html.querySelectorAll('button')).find((b) => b.textContent?.trim() === texto) ?? null;

  beforeEach(async () => {
    api.escuelas.mockReset().mockReturnValue(of([escuela]));
    api.inscritosEscuela.mockReset().mockReturnValue(of([hugo]));
    TestBed.configureTestingModule({
      imports: [AdminEscuelasPage],
      providers: [
        { provide: AdminService, useValue: api },
        { provide: InstalacionesService, useValue: { catalogo: () => of([]) } },
      ],
    });
    fixture = TestBed.createComponent(AdminEscuelasPage);
    html = fixture.nativeElement;
    await fixture.whenStable();
  });

  it('P52 · muestra el contacto de emergencia de cada inscrito', async () => {
    boton('Inscritos')!.click();
    await fixture.whenStable();
    expect(api.inscritosEscuela).toHaveBeenCalledWith(1);
    const cabecera = Array.from(html.querySelectorAll('th')).map((th) => th.textContent?.trim());
    expect(cabecera).toContain('Contacto de emergencia');
    expect(html.textContent).toContain('Antonio Ruiz Gómez (abuelo)');
    expect(html.textContent).toContain('600000007');
  });
});
