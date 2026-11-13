import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { mensajeDeError } from '../../core/errores';
import { aFechaIso, fechaLegible, horaCorta } from '../../core/fechas';
import { InstalacionesService } from '../../core/instalaciones.service';
import { Disponibilidad, Franja, Instalacion, Reserva } from '../../core/modelos';
import { ReservasService } from '../../core/reservas.service';
import { Variacion7Service } from '../../core/variacion7.service';

const ESTADO: Record<string, { texto: string; clase: string }> = {
  PENDIENTE: { texto: 'Pendiente de autorizar', clase: 'pendiente' },
  CONFIRMADA: { texto: 'Autorizada', clase: 'ok' },
  RECHAZADA: { texto: 'Rechazada', clase: 'mal' },
  CANCELADA: { texto: 'Cancelada', clase: 'mal' },
  FINALIZADA: { texto: 'Finalizada', clase: '' },
  NO_PRESENTADO: { texto: 'No presentada', clase: 'mal' },
};

/**
 * P21 · Reservas institucionales del representante de una entidad con convenio. Puede abarcar varias
 * franjas seguidas; nace pendiente (ya ocupa la franja) hasta que la administración la autoriza o rechaza.
 */
@Component({
  selector: 'app-institucional',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressBarModule,
    MatSelectModule],
  template: `
    <section class="pagina-media" aria-labelledby="titulo-institucional">
      <h1 id="titulo-institucional" class="titulo-pagina">Reservas institucionales</h1>
      <p class="subtitulo">Solicita instalaciones en nombre de tu entidad con al menos 48 horas de antelación y hasta
        4 horas seguidas por reserva. La administración autorizará cada solicitud; mientras tanto la franja queda reservada.</p>

      @if (aviso(); as a) {
        <p class="aviso" [class.error]="a.error" [attr.role]="a.error ? 'alert' : 'status'">{{ a.texto }}</p>
      }

      <div class="tarjeta">
        <div class="rejilla-form">
          <mat-form-field appearance="outline">
            <mat-label>Instalación</mat-label>
            <mat-select [ngModel]="idInstalacion()" (ngModelChange)="idInstalacion.set($event); consultar()">
              @for (i of instalaciones(); track i.id) { <mat-option [value]="i.id">{{ i.nombre }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Fecha</mat-label>
            <input matInput type="date" [min]="hoy" [ngModel]="fecha()" (ngModelChange)="fecha.set($event); consultar()" />
          </mat-form-field>
        </div>
        @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }
        @if (disponibilidad(); as d) {
          @if (d.franjas.length === 0) {
            <p class="vacio">La instalación no abre este día.</p>
          } @else {
            <p class="meta">Pulsa la primera y la última franja del periodo que necesitas.</p>
            <div class="fila-acciones" style="justify-content:flex-start" role="group" aria-label="Franjas">
              @for (f of d.franjas; track f.horaInicio; let i = $index) {
                <button type="button"
                        [class]="'chip ' + (enRango(i) ? 'ok' : f.estado === 'LIBRE' ? '' : 'mal')"
                        style="border:0;cursor:pointer;padding:6px 12px"
                        [disabled]="f.estado !== 'LIBRE'" (click)="elegir(i)"
                        [attr.aria-pressed]="enRango(i)">
                  {{ horaCorta(f.horaInicio) }}–{{ horaCorta(f.horaFin) }}
                </button>
              }
            </div>
            @if (rango(); as r) {
              <div class="fila-acciones" style="margin-top:12px">
                <span>{{ fechaLegible(fecha()) }}, de {{ horaCorta(r.inicio.horaInicio) }} a {{ horaCorta(r.fin.horaFin) }}</span>
                <button mat-flat-button type="button" (click)="solicitar()" [disabled]="enviando()">
                  Solicitar reserva
                </button>
              </div>
            }
          }
        }
      </div>

      <h2 class="seccion">Mis solicitudes</h2>
      @for (r of solicitudes(); track r.id) {
        <article class="tarjeta fila-acciones" style="justify-content:space-between" [attr.aria-label]="'Solicitud ' + r.id">
          <div>
            <h3>{{ r.instalacion }}</h3>
            <p class="meta">{{ fechaLegible(r.fecha) }} · {{ horaCorta(r.horaInicio) }}–{{ horaCorta(r.horaFin) }}</p>
            @if (r.motivoRechazo) { <p class="meta">Motivo del rechazo: {{ r.motivoRechazo }}</p> }
          </div>
          <div class="fila-acciones">
            <span class="chip {{ estado(r).clase }}">{{ estado(r).texto }}</span>
            @if (r.cancelable) {
              <button mat-button type="button" (click)="cancelar(r)">Cancelar</button>
            }
          </div>
        </article>
      } @empty {
        <p class="vacio">Todavía no has hecho solicitudes.</p>
      }
    </section>
  `,
})
export class InstitucionalPage implements OnInit {
  private readonly instalacionesApi = inject(InstalacionesService);
  private readonly reservasApi = inject(ReservasService);
  private readonly api = inject(Variacion7Service);

  protected readonly horaCorta = horaCorta;
  protected readonly fechaLegible = fechaLegible;
  protected readonly hoy = aFechaIso(new Date());

  protected readonly instalaciones = signal<Instalacion[]>([]);
  protected readonly idInstalacion = signal<number | null>(null);
  protected readonly fecha = signal(this.hoy);
  protected readonly disponibilidad = signal<Disponibilidad | null>(null);
  protected readonly cargando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly desde = signal<number | null>(null);
  protected readonly hasta = signal<number | null>(null);
  protected readonly solicitudes = signal<Reserva[]>([]);
  protected readonly aviso = signal<{ texto: string; error: boolean } | null>(null);

  protected readonly rango = computed<{ inicio: Franja; fin: Franja } | null>(() => {
    const d = this.disponibilidad();
    const a = this.desde();
    if (!d || a === null) {
      return null;
    }
    const b = this.hasta() ?? a;
    return { inicio: d.franjas[Math.min(a, b)], fin: d.franjas[Math.max(a, b)] };
  });

  ngOnInit(): void {
    this.instalacionesApi.catalogo().subscribe({
      next: (l) => this.instalaciones.set(l),
      error: (err: unknown) => this.fallo(err),
    });
    this.cargarSolicitudes();
  }

  protected estado(r: Reserva) {
    return ESTADO[r.estado] ?? { texto: r.estado, clase: '' };
  }

  protected enRango(i: number): boolean {
    const a = this.desde();
    if (a === null) {
      return false;
    }
    const b = this.hasta() ?? a;
    return i >= Math.min(a, b) && i <= Math.max(a, b);
  }

  /** Primer toque: inicio; segundo: fin. Un tercero empieza de nuevo. El periodo no puede saltar franjas ocupadas. */
  protected elegir(i: number): void {
    if (this.desde() === null || this.hasta() !== null) {
      this.desde.set(i);
      this.hasta.set(null);
      return;
    }
    const a = this.desde()!;
    const franjas = this.disponibilidad()!.franjas;
    const [ini, fin] = [Math.min(a, i), Math.max(a, i)];
    if (franjas.slice(ini, fin + 1).some((f) => f.estado !== 'LIBRE')) {
      this.aviso.set({ texto: 'El periodo no puede incluir franjas ocupadas o bloqueadas.', error: true });
      return;
    }
    // P28: como máximo 4 horas seguidas por reserva
    const minutos = (h: string) => Number(h.slice(0, 2)) * 60 + Number(h.slice(3, 5));
    if (minutos(franjas[fin].horaFin) - minutos(franjas[ini].horaInicio) > 4 * 60) {
      this.aviso.set({ texto: 'Una reserva institucional puede durar como máximo 4 horas seguidas.', error: true });
      return;
    }
    this.aviso.set(null);
    this.hasta.set(i);
  }

  protected consultar(): void {
    const id = this.idInstalacion();
    this.desde.set(null);
    this.hasta.set(null);
    if (!id || !this.fecha()) {
      return;
    }
    this.cargando.set(true);
    this.instalacionesApi.disponibilidad(id, this.fecha()).subscribe({
      next: (d) => {
        this.disponibilidad.set(d);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.cargando.set(false);
        this.fallo(err);
      },
    });
  }

  protected solicitar(): void {
    const r = this.rango();
    const id = this.idInstalacion();
    if (!r || !id) {
      return;
    }
    this.enviando.set(true);
    this.api
      .solicitarInstitucional({ idInstalacion: id, fecha: this.fecha(), horaInicio: horaCorta(r.inicio.horaInicio),
        horaFin: horaCorta(r.fin.horaFin) })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.aviso.set({ texto: 'Solicitud enviada. Queda pendiente de autorización por la administración.', error: false });
          this.consultar();
          this.cargarSolicitudes();
        },
        error: (err: unknown) => {
          this.fallo(err);
          this.consultar();
        },
      });
  }

  protected cancelar(r: Reserva): void {
    this.reservasApi.cancelar(r.id).subscribe({
      next: () => {
        this.aviso.set({ texto: 'Solicitud cancelada.', error: false });
        this.cargarSolicitudes();
        this.consultar();
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  private cargarSolicitudes(): void {
    this.reservasApi.mias().subscribe({
      next: (l) => this.solicitudes.set(l.filter((r) => r.institucional)),
      error: (err: unknown) => this.fallo(err),
    });
  }

  private fallo(err: unknown): void {
    this.enviando.set(false);
    this.aviso.set({ texto: mensajeDeError(err), error: true });
  }
}
