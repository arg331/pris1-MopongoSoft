import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AdminService } from '../../core/admin.service';
import { mensajeDeError } from '../../core/errores';
import { fechaLegible, horaCorta } from '../../core/fechas';
import { EstadoReserva, ReservaInstitucional } from '../../core/modelos';

/** P21 · Bandeja de autorización de reservas institucionales (autorizar o rechazar con motivo). */
@Component({
  selector: 'app-admin-institucionales',
  imports: [FormsModule, MatButtonModule, MatButtonToggleModule, MatFormFieldModule, MatInputModule, MatProgressBarModule],
  template: `
    <mat-button-toggle-group [ngModel]="filtro()" (ngModelChange)="filtro.set($event); cargar()" aria-label="Estado">
      <mat-button-toggle value="PENDIENTE">Pendientes</mat-button-toggle>
      <mat-button-toggle [value]="null">Todas</mat-button-toggle>
    </mat-button-toggle-group>

    @if (aviso(); as a) {
      <p class="aviso" [class.error]="a.error" [attr.role]="a.error ? 'alert' : 'status'">{{ a.texto }}</p>
    }
    @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }

    <div style="margin-top:12px">
      @for (r of lista(); track r.id) {
        <article class="tarjeta" [attr.aria-label]="'Solicitud de ' + r.entidad">
          <div class="fila-acciones" style="justify-content:space-between">
            <div>
              <h3>{{ r.entidad }} · {{ r.instalacion }}</h3>
              <p class="meta">{{ fechaLegible(r.fecha) }}, {{ horaCorta(r.horaInicio) }}–{{ horaCorta(r.horaFin) }}
                · solicitada por {{ r.representante }} ({{ r.emailRepresentante }})</p>
              @if (r.autorizador) {
                <p class="meta">Resuelta por {{ r.autorizador }}@if (r.motivoRechazo) {: {{ r.motivoRechazo }}}</p>
              }
            </div>
            <span class="chip" [class.pendiente]="r.estado === 'PENDIENTE'" [class.ok]="r.estado === 'CONFIRMADA'"
                  [class.mal]="r.estado === 'RECHAZADA' || r.estado === 'CANCELADA'">{{ r.estado }}</span>
          </div>
          @if (r.estado === 'PENDIENTE') {
            @if (rechazando() === r.id) {
              <div class="fila-acciones">
                <mat-form-field appearance="outline" subscriptSizing="dynamic" style="flex:1;min-width:220px">
                  <mat-label>Motivo del rechazo</mat-label>
                  <input matInput [(ngModel)]="motivo" />
                </mat-form-field>
                <button mat-button type="button" (click)="rechazando.set(null)">Volver</button>
                <button mat-flat-button type="button" (click)="rechazar(r)" [disabled]="!motivo.trim()">Rechazar</button>
              </div>
            } @else {
              <div class="fila-acciones">
                @if (iniciada(r)) {
                  <span class="meta">La fecha ya ha pasado: no se puede autorizar (plazo de autorización pendiente, ❓P26)</span>
                }
                <button mat-stroked-button type="button" (click)="rechazando.set(r.id); motivo = ''">Rechazar…</button>
                @if (!iniciada(r)) {
                  <button mat-flat-button type="button" (click)="autorizar(r)">Autorizar</button>
                }
              </div>
            }
          }
        </article>
      } @empty {
        @if (!cargando()) { <p class="vacio">No hay solicitudes {{ filtro() ? 'pendientes' : '' }}.</p> }
      }
    </div>
  `,
})
export class AdminInstitucionalesPage implements OnInit {
  private readonly api = inject(AdminService);

  protected readonly fechaLegible = fechaLegible;
  protected readonly horaCorta = horaCorta;
  protected readonly filtro = signal<EstadoReserva | null>('PENDIENTE');
  protected readonly lista = signal<ReservaInstitucional[]>([]);
  protected readonly cargando = signal(false);
  protected readonly rechazando = signal<number | null>(null);
  protected readonly aviso = signal<{ texto: string; error: boolean } | null>(null);
  protected motivo = '';

  ngOnInit(): void {
    this.cargar();
  }

  /** El backend no autoriza una reserva que ya ha empezado (RESERVA_INICIADA). */
  protected iniciada(r: ReservaInstitucional): boolean {
    return new Date(`${r.fecha}T${r.horaInicio}`).getTime() <= Date.now();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.api.institucionales(this.filtro()).subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected autorizar(r: ReservaInstitucional): void {
    this.api.autorizar(r.id).subscribe({
      next: () => this.hecho(`Reserva de ${r.entidad} autorizada.`),
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected rechazar(r: ReservaInstitucional): void {
    this.api.rechazar(r.id, this.motivo.trim()).subscribe({
      next: () => this.hecho(`Reserva de ${r.entidad} rechazada; la franja queda libre.`),
      error: (err: unknown) => this.fallo(err),
    });
  }

  private hecho(texto: string): void {
    this.rechazando.set(null);
    this.aviso.set({ texto, error: false });
    this.cargar();
  }

  private fallo(err: unknown): void {
    this.cargando.set(false);
    this.aviso.set({ texto: mensajeDeError(err), error: true });
  }
}
