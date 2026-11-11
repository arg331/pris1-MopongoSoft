import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { AdminService } from '../../core/admin.service';
import { mensajeDeError } from '../../core/errores';
import { aFechaIso, fechaLegible, horaCorta, sumarDias } from '../../core/fechas';
import { InstalacionesService } from '../../core/instalaciones.service';
import { EstadoReserva, Instalacion, ReservaAdmin } from '../../core/modelos';

const ESTADOS: EstadoReserva[] = ['PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'NO_PRESENTADO', 'FINALIZADA', 'RECHAZADA'];

/** F10 · Consulta general de reservas con filtros. F12 · marcar «no presentado» (P3) y cancelar. */
@Component({
  selector: 'app-admin-reservas',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatProgressBarModule, MatSelectModule],
  template: `
    <div class="rejilla-form">
      <mat-form-field appearance="outline">
        <mat-label>Desde</mat-label>
        <input matInput type="date" [(ngModel)]="desde" />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Hasta</mat-label>
        <input matInput type="date" [(ngModel)]="hasta" />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Instalación</mat-label>
        <mat-select [(ngModel)]="idInstalacion">
          <mat-option [value]="null">Todas</mat-option>
          @for (i of instalaciones(); track i.id) { <mat-option [value]="i.id">{{ i.nombre }}</mat-option> }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Estado</mat-label>
        <mat-select [(ngModel)]="estado">
          <mat-option [value]="null">Todos</mat-option>
          @for (e of estados; track e) { <mat-option [value]="e">{{ e }}</mat-option> }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Usuario o entidad</mat-label>
        <input matInput [(ngModel)]="texto" (keyup.enter)="buscar()" />
      </mat-form-field>
      <div class="fila-acciones" style="padding-top:8px">
        <button mat-flat-button type="button" (click)="buscar()">Buscar</button>
      </div>
    </div>

    @if (aviso(); as a) {
      <p class="aviso" [class.error]="a.error" [attr.role]="a.error ? 'alert' : 'status'">{{ a.texto }}</p>
    }
    @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }

    <p class="meta">{{ reservas().length }} reservas</p>
    <div class="tabla-envoltorio">
      <table class="tabla">
        <thead>
          <tr><th>Fecha</th><th>Hora</th><th>Instalación</th><th>Usuario</th><th>Perfil</th><th>Estado</th><th></th></tr>
        </thead>
        <tbody>
          @for (r of reservas(); track r.id) {
            <tr>
              <td>{{ fechaLegible(r.fecha) }}</td>
              <td>{{ horaCorta(r.horaInicio) }}–{{ horaCorta(r.horaFin) }}</td>
              <td>{{ r.instalacion }}</td>
              <td>{{ r.usuario }}@if (r.entidad) { <br /><span class="meta">{{ r.entidad }}</span> }</td>
              <td>{{ r.rol }}</td>
              <td><span class="chip {{ clase(r.estado) }}">{{ r.estado }}</span></td>
              <td class="acciones">
                @if (r.estado === 'CONFIRMADA' && empezada(r)) {
                  <button mat-button type="button" (click)="noPresentado(r)">No presentado</button>
                }
                @if ((r.estado === 'CONFIRMADA' || r.estado === 'PENDIENTE') && !empezada(r)) {
                  <button mat-button type="button" (click)="cancelar(r)">Cancelar</button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="7" class="vacio">No hay reservas con esos filtros.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class AdminReservasPage implements OnInit {
  private readonly api = inject(AdminService);
  private readonly instalacionesApi = inject(InstalacionesService);

  protected readonly fechaLegible = fechaLegible;
  protected readonly horaCorta = horaCorta;
  protected readonly estados = ESTADOS;

  protected desde = aFechaIso(sumarDias(new Date(), -7));
  protected hasta = aFechaIso(sumarDias(new Date(), 30));
  protected idInstalacion: number | null = null;
  protected estado: EstadoReserva | null = null;
  protected texto = '';

  protected readonly instalaciones = signal<Instalacion[]>([]);
  protected readonly reservas = signal<ReservaAdmin[]>([]);
  protected readonly cargando = signal(false);
  protected readonly aviso = signal<{ texto: string; error: boolean } | null>(null);

  ngOnInit(): void {
    this.instalacionesApi.catalogo().subscribe({ next: (l) => this.instalaciones.set(l) });
    this.buscar();
  }

  protected clase(estado: EstadoReserva): string {
    return estado === 'CONFIRMADA' || estado === 'FINALIZADA' ? 'ok'
      : estado === 'PENDIENTE' ? 'pendiente' : 'mal';
  }

  protected empezada(r: ReservaAdmin): boolean {
    return new Date(`${r.fecha}T${r.horaInicio}`).getTime() <= Date.now();
  }

  protected buscar(): void {
    this.cargando.set(true);
    this.api
      .reservas({ desde: this.desde, hasta: this.hasta, idInstalacion: this.idInstalacion, estado: this.estado,
        texto: this.texto.trim() })
      .subscribe({
        next: (l) => {
          this.reservas.set(l);
          this.cargando.set(false);
        },
        error: (err: unknown) => this.fallo(err),
      });
  }

  protected noPresentado(r: ReservaAdmin): void {
    this.api.noPresentado(r.id).subscribe({
      next: () => {
        this.aviso.set({ texto: `${r.usuario} marcado como no presentado; no podrá reservar hasta el domingo (P3).`, error: false });
        this.buscar();
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected cancelar(r: ReservaAdmin): void {
    this.api.cancelarReserva(r.id).subscribe({
      next: () => {
        this.aviso.set({ texto: 'Reserva cancelada.', error: false });
        this.buscar();
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  private fallo(err: unknown): void {
    this.cargando.set(false);
    this.aviso.set({ texto: mensajeDeError(err), error: true });
  }
}
