import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../core/auth.service';
import { mensajeDeError } from '../../core/errores';
import { fechaHoraLegible } from '../../core/fechas';
import { Evento } from '../../core/modelos';
import { Variacion7Service } from '../../core/variacion7.service';

/** P18 · Eventos masivos publicados: inscripción y anulación hasta que empiezan (aforo controlado por la BD). */
@Component({
  selector: 'app-eventos',
  imports: [MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <section class="pagina-media" aria-labelledby="titulo-eventos">
      <h1 id="titulo-eventos" class="titulo-pagina">Eventos</h1>
      <p class="subtitulo">Jornadas y eventos deportivos organizados por el Servicio de Deportes.</p>

      @if (aviso(); as a) {
        <p class="aviso" [class.error]="a.error" [attr.role]="a.error ? 'alert' : 'status'">{{ a.texto }}</p>
      }
      @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }

      @for (e of eventos(); track e.id) {
        <article class="tarjeta" [attr.aria-label]="e.nombre">
          <h3>{{ e.nombre }}</h3>
          <p class="meta">{{ fechaHoraLegible(e.inicio) }} – {{ fechaHoraLegible(e.fin).slice(11) }}
            · {{ e.instalaciones.join(', ') }}</p>
          <p class="meta">Inscripciones hasta el {{ fechaHoraLegible(e.cierreInscripcion) }}
            · {{ e.publico === 'COMUNIDAD_UAL' ? 'Solo comunidad universitaria' : 'Abierto también a usuarios externos' }}</p>
          @if (e.descripcion) { <p>{{ e.descripcion }}</p> }
          <div class="fila-acciones">
            <span class="chip" [class.ok]="e.plazasLibres > 0" [class.mal]="e.plazasLibres === 0">
              {{ e.plazasLibres > 0 ? e.plazasLibres + ' plazas libres de ' + e.aforoMaximo : 'Aforo completo' }}
            </span>
            @if (e.inscrito) {
              <span class="chip ok">Inscrito</span>
            }
            <span style="flex:1"></span>
            @if (cerrada(e)) {
              <span class="meta">Inscripciones cerradas</span>
            } @else if (puedeInscribirse()) {
              @if (e.inscrito) {
                <button mat-stroked-button type="button" (click)="anular(e)" [disabled]="ocupado() === e.id">
                  Anular inscripción
                </button>
              } @else {
                <button mat-flat-button type="button" (click)="inscribir(e)"
                        [disabled]="ocupado() === e.id || e.plazasLibres === 0">
                  <mat-icon>how_to_reg</mat-icon> Inscribirme
                </button>
              }
            }
          </div>
        </article>
      } @empty {
        @if (!cargando()) { <p class="vacio">No hay eventos publicados ahora mismo.</p> }
      }
    </section>
  `,
})
export class EventosPage implements OnInit {
  private readonly api = inject(Variacion7Service);
  private readonly auth = inject(AuthService);

  protected readonly fechaHoraLegible = fechaHoraLegible;
  protected readonly eventos = signal<Evento[]>([]);
  protected readonly cargando = signal(true);
  protected readonly ocupado = signal<number | null>(null);
  protected readonly aviso = signal<{ texto: string; error: boolean } | null>(null);
  /** La administración organiza los eventos pero no se inscribe (P33). */
  protected readonly puedeInscribirse = () => !this.auth.esAdmin();

  ngOnInit(): void {
    this.cargar();
  }

  /** P35: inscripción y anulación hasta 24 h antes del inicio. */
  protected cerrada(e: Evento): boolean {
    return new Date(e.cierreInscripcion).getTime() <= Date.now();
  }

  protected inscribir(e: Evento): void {
    this.ejecutar(e, this.api.inscribirEnEvento(e.id), `Te has inscrito en «${e.nombre}».`);
  }

  protected anular(e: Evento): void {
    this.ejecutar(e, this.api.anularInscripcion(e.id), `Inscripción en «${e.nombre}» anulada.`);
  }

  private ejecutar(e: Evento, peticion: ReturnType<Variacion7Service['inscribirEnEvento']>, texto: string): void {
    this.ocupado.set(e.id);
    peticion.subscribe({
      next: (actualizado) => {
        this.eventos.update((l) => l.map((x) => (x.id === actualizado.id ? actualizado : x)));
        this.ocupado.set(null);
        this.aviso.set({ texto, error: false });
      },
      error: (err: unknown) => {
        this.ocupado.set(null);
        this.aviso.set({ texto: mensajeDeError(err), error: true });
        this.cargar();
      },
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.eventos().subscribe({
      next: (l) => {
        this.eventos.set(l);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.cargando.set(false);
        this.aviso.set({ texto: mensajeDeError(err), error: true });
      },
    });
  }
}
