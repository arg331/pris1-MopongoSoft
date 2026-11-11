import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { AdminService } from '../../core/admin.service';
import { mensajeDeError } from '../../core/errores';
import { DIAS_SEMANA, fechaLegible, horaCorta } from '../../core/fechas';
import { InstalacionesService } from '../../core/instalaciones.service';
import { Escuela, InscritoEscuela, Instalacion, SesionEscuela } from '../../core/modelos';

function escuelaVacia() {
  const anio = new Date().getFullYear();
  return { idInstalacion: null as number | null, nombre: '', temporada: `${anio}-${anio + 1}`,
    fechaInicio: '', fechaFin: '', edadMin: 6, edadMax: 12, plazas: 12,
    horario: [{ diaSemana: 1, horaInicio: '17:00', horaFin: '18:00' }] as SesionEscuela[] };
}

/** P19 · Escuelas con horario semanal fijo que bloquea la instalación toda la temporada; P20 · inscritos. */
@Component({
  selector: 'app-admin-escuelas',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressBarModule,
    MatSelectModule],
  template: `
    @if (aviso(); as a) {
      <p class="aviso" [class.error]="a.error" [attr.role]="a.error ? 'alert' : 'status'">{{ a.texto }}</p>
    }

    <details class="tarjeta">
      <summary><strong>Nueva escuela</strong></summary>
      <div class="rejilla-form" style="margin-top:12px">
        <mat-form-field appearance="outline">
          <mat-label>Nombre</mat-label>
          <input matInput [(ngModel)]="nueva.nombre" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Instalación</mat-label>
          <mat-select [(ngModel)]="nueva.idInstalacion">
            @for (i of instalaciones(); track i.id) { <mat-option [value]="i.id">{{ i.nombre }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Temporada</mat-label>
          <input matInput [(ngModel)]="nueva.temporada" placeholder="2026-2027" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Inicio de temporada</mat-label>
          <input matInput type="date" [(ngModel)]="nueva.fechaInicio" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Fin de temporada</mat-label>
          <input matInput type="date" [(ngModel)]="nueva.fechaFin" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Edad mínima</mat-label>
          <input matInput type="number" min="3" max="17" [(ngModel)]="nueva.edadMin" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Edad máxima</mat-label>
          <input matInput type="number" min="3" max="17" [(ngModel)]="nueva.edadMax" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Plazas</mat-label>
          <input matInput type="number" min="1" [(ngModel)]="nueva.plazas" />
        </mat-form-field>
      </div>
      <p class="meta"><strong>Sesiones semanales</strong> (bloquean la instalación toda la temporada; las que caen en
        festivo no se generan)</p>
      @for (s of nueva.horario; track $index) {
        <div class="fila-acciones" style="justify-content:flex-start">
          <mat-form-field appearance="outline" subscriptSizing="dynamic" style="width:150px">
            <mat-select [(ngModel)]="s.diaSemana" aria-label="Día">
              @for (d of dias; track d) { <mat-option [value]="d">{{ diasSemana[d] }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic" style="width:130px">
            <input matInput type="time" [(ngModel)]="s.horaInicio" aria-label="Inicio" />
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic" style="width:130px">
            <input matInput type="time" [(ngModel)]="s.horaFin" aria-label="Fin" />
          </mat-form-field>
          <button mat-icon-button type="button" (click)="nueva.horario.splice($index, 1)" aria-label="Quitar sesión">
            <mat-icon>delete</mat-icon>
          </button>
        </div>
      }
      <div class="fila-acciones" style="justify-content:space-between">
        <button mat-button type="button" (click)="nueva.horario.push({ diaSemana: 3, horaInicio: '17:00', horaFin: '18:00' })">
          <mat-icon>add</mat-icon> Añadir sesión
        </button>
        <button mat-flat-button type="button" (click)="crear()"
                [disabled]="!nueva.nombre.trim() || !nueva.idInstalacion || !nueva.fechaInicio || !nueva.fechaFin || nueva.horario.length === 0">
          Crear escuela
        </button>
      </div>
    </details>

    @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }
    @for (e of lista(); track e.id) {
      <article class="tarjeta" [attr.aria-label]="e.nombre">
        <div class="fila-acciones" style="justify-content:space-between">
          <div>
            <h3>{{ e.nombre }} ({{ e.temporada }})</h3>
            <p class="meta">{{ e.instalacion }} · {{ horario(e) }} · {{ fechaLegible(e.fechaInicio) }} – {{ fechaLegible(e.fechaFin) }}</p>
            <p class="meta">De {{ e.edadMin }} a {{ e.edadMax }} años · {{ e.inscritos }} inscritos de {{ e.plazas }}</p>
          </div>
          <div class="fila-acciones">
            @if (e.estado === 'CANCELADA') { <span class="chip mal">Cancelada</span> }
            <button mat-button type="button" (click)="verInscritos(e)">Inscritos</button>
            @if (e.estado === 'ACTIVA') {
              @if (cancelando() === e.id) {
                <span class="meta">Se liberan sus sesiones futuras y los inscritos pasan a baja.</span>
                <button mat-button type="button" (click)="cancelando.set(null)">No</button>
                <button mat-flat-button type="button" (click)="cancelar(e)">Sí, cancelar</button>
              } @else {
                <button mat-button type="button" (click)="cancelando.set(e.id)">Cancelar escuela</button>
              }
            }
          </div>
        </div>
        @if (inscritosDe() === e.id) {
          <div class="tabla-envoltorio">
            <table class="tabla">
              <thead><tr><th>Menor</th><th>Nacimiento</th><th>Tutor</th><th>Contacto</th><th>Estado</th></tr></thead>
              <tbody>
                @for (i of inscritos(); track i.idMenor) {
                  <tr>
                    <td>{{ i.menor }}</td><td>{{ fechaLegible(i.fechaNacimiento) }}</td><td>{{ i.tutor }}</td>
                    <td>{{ i.emailTutor }}@if (i.telefonoTutor) {<br />{{ i.telefonoTutor }}}</td><td>{{ i.estado }}</td>
                  </tr>
                } @empty {
                  <tr><td colspan="5" class="vacio">Sin inscritos.</td></tr>
                }
              </tbody>
            </table>
          </div>
        }
      </article>
    } @empty {
      @if (!cargando()) { <p class="vacio">No hay escuelas.</p> }
    }
  `,
})
export class AdminEscuelasPage implements OnInit {
  private readonly api = inject(AdminService);
  private readonly instalacionesApi = inject(InstalacionesService);

  protected readonly fechaLegible = fechaLegible;
  protected readonly dias = [1, 2, 3, 4, 5, 6, 7];
  protected readonly diasSemana = DIAS_SEMANA;
  protected readonly instalaciones = signal<Instalacion[]>([]);
  protected readonly lista = signal<Escuela[]>([]);
  protected readonly inscritos = signal<InscritoEscuela[]>([]);
  protected readonly inscritosDe = signal<number | null>(null);
  protected readonly cancelando = signal<number | null>(null);
  protected readonly cargando = signal(false);
  protected readonly aviso = signal<{ texto: string; error: boolean } | null>(null);
  protected nueva = escuelaVacia();

  ngOnInit(): void {
    this.instalacionesApi.catalogo().subscribe({ next: (l) => this.instalaciones.set(l) });
    this.cargar();
  }

  protected horario(e: Escuela): string {
    return e.horario.map((s) => `${DIAS_SEMANA[s.diaSemana]} ${horaCorta(s.horaInicio)}–${horaCorta(s.horaFin)}`).join(', ');
  }

  protected crear(): void {
    const n = this.nueva;
    this.api
      .crearEscuela({ idInstalacion: n.idInstalacion!, nombre: n.nombre.trim(), temporada: n.temporada.trim(),
        fechaInicio: n.fechaInicio, fechaFin: n.fechaFin, edadMin: Number(n.edadMin), edadMax: Number(n.edadMax),
        plazas: Number(n.plazas), horario: n.horario })
      .subscribe({
        next: (r) => {
          this.nueva = escuelaVacia();
          this.hecho(`Escuela «${r.escuela.nombre}» creada: ${r.sesionesBloqueadas} sesiones bloqueadas, `
            + `${r.sesionesOmitidas} omitidas por festivos u otros bloqueos y ${r.reservasCanceladas} reservas canceladas.`);
        },
        error: (err: unknown) => this.fallo(err),
      });
  }

  protected cancelar(e: Escuela): void {
    this.cancelando.set(null);
    this.api.cancelarEscuela(e.id).subscribe({
      next: (r) => this.hecho(`«${e.nombre}» cancelada: sesiones futuras liberadas y ${r.reservasCanceladas} inscrito(s) dados de baja.`),
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected verInscritos(e: Escuela): void {
    if (this.inscritosDe() === e.id) {
      this.inscritosDe.set(null);
      return;
    }
    this.api.inscritosEscuela(e.id).subscribe({
      next: (l) => {
        this.inscritos.set(l);
        this.inscritosDe.set(e.id);
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.escuelas().subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  private hecho(texto: string): void {
    this.aviso.set({ texto, error: false });
    this.cargar();
  }

  private fallo(err: unknown): void {
    this.cargando.set(false);
    this.aviso.set({ texto: mensajeDeError(err), error: true });
  }
}
