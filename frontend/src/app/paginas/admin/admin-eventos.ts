import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { AdminService } from '../../core/admin.service';
import { mensajeDeError } from '../../core/errores';
import { desdeDatetimeLocal, fechaHoraLegible } from '../../core/fechas';
import { InstalacionesService } from '../../core/instalaciones.service';
import { Evento, InscritoEvento, Instalacion, PublicoEvento } from '../../core/modelos';

/** P18 · Eventos masivos: alta en varias instalaciones, publicación, cancelación e inscritos. */
@Component({
  selector: 'app-admin-eventos',
  imports: [FormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatIconModule, MatInputModule,
    MatProgressBarModule, MatSelectModule],
  template: `
    @if (aviso(); as a) {
      <p class="aviso" [class.error]="a.error" [attr.role]="a.error ? 'alert' : 'status'">{{ a.texto }}</p>
    }

    <details class="tarjeta">
      <summary><strong>Nuevo evento</strong></summary>
      <div class="rejilla-form" style="margin-top:12px">
        <mat-form-field appearance="outline" class="completo">
          <mat-label>Nombre</mat-label>
          <input matInput [(ngModel)]="nuevo.nombre" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="completo">
          <mat-label>Descripción</mat-label>
          <textarea matInput rows="2" [(ngModel)]="nuevo.descripcion"></textarea>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Inicio</mat-label>
          <input matInput type="datetime-local" [(ngModel)]="nuevo.inicio" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Fin</mat-label>
          <input matInput type="datetime-local" [(ngModel)]="nuevo.fin" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Quién puede inscribirse</mat-label>
          <mat-select [(ngModel)]="nuevo.publico">
            <mat-option value="COMUNIDAD_UAL">Solo comunidad universitaria</mat-option>
            <mat-option value="COMUNIDAD_Y_EXTERNOS">Comunidad universitaria y externos</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Aforo máximo</mat-label>
          <input matInput type="number" min="1" [(ngModel)]="nuevo.aforoMaximo" />
        </mat-form-field>
        <fieldset class="completo" style="border:0;padding:0">
          <legend class="meta">Instalaciones que ocupa (se bloquean y se cancelan sus reservas, P7)</legend>
          @for (i of instalaciones(); track i.id) {
            <mat-checkbox [checked]="elegidas.has(i.id)" (change)="alternar(i.id)">{{ i.nombre }}</mat-checkbox>
          }
        </fieldset>
        <div class="completo fila-acciones">
          <button mat-flat-button type="button" (click)="crear()"
                  [disabled]="!nuevo.nombre.trim() || !nuevo.inicio || !nuevo.fin || elegidas.size === 0">Crear evento</button>
        </div>
      </div>
    </details>

    @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }
    @for (e of lista(); track e.id) {
      <article class="tarjeta" [attr.aria-label]="e.nombre">
        <div class="fila-acciones" style="justify-content:space-between">
          <div>
            <h3>{{ e.nombre }}</h3>
            <p class="meta">{{ fechaHoraLegible(e.inicio) }} – {{ fechaHoraLegible(e.fin) }} · {{ e.instalaciones.join(', ') }}</p>
            <p class="meta">{{ e.inscritos }} inscritos de {{ e.aforoMaximo }} ·
              {{ e.publico === 'COMUNIDAD_UAL' ? 'solo comunidad UAL' : 'comunidad UAL y externos' }} ·
              inscripciones hasta {{ fechaHoraLegible(e.cierreInscripcion) }}</p>
          </div>
          <span class="chip" [class.ok]="e.estado === 'PUBLICADO'" [class.pendiente]="e.estado === 'PLANIFICADO'"
                [class.mal]="e.estado === 'CANCELADO'">{{ e.estado }}</span>
        </div>
        <div class="fila-acciones">
          <button mat-button type="button" (click)="verInscritos(e)">Inscritos</button>
          @if (e.estado === 'PLANIFICADO') {
            <button mat-stroked-button type="button" (click)="publicar(e)">Publicar</button>
          }
          @if (e.estado === 'PLANIFICADO' || e.estado === 'PUBLICADO') {
            <button mat-button type="button" (click)="cancelar(e)">Cancelar evento</button>
          }
        </div>
        @if (inscritosDe() === e.id) {
          <div class="tabla-envoltorio" style="margin-top:8px">
            <table class="tabla">
              <thead><tr><th>Nombre</th><th>Email</th><th>Perfil</th><th>Estado</th></tr></thead>
              <tbody>
                @for (i of inscritos(); track i.idUsuario) {
                  <tr><td>{{ i.nombre }}</td><td>{{ i.email }}</td><td>{{ i.rol }}</td><td>{{ i.estado }}</td></tr>
                } @empty {
                  <tr><td colspan="4" class="vacio">Sin inscritos.</td></tr>
                }
              </tbody>
            </table>
          </div>
        }
      </article>
    } @empty {
      @if (!cargando()) { <p class="vacio">No hay eventos.</p> }
    }
  `,
})
export class AdminEventosPage implements OnInit {
  private readonly api = inject(AdminService);
  private readonly instalacionesApi = inject(InstalacionesService);

  protected readonly fechaHoraLegible = fechaHoraLegible;
  protected readonly instalaciones = signal<Instalacion[]>([]);
  protected readonly lista = signal<Evento[]>([]);
  protected readonly inscritos = signal<InscritoEvento[]>([]);
  protected readonly inscritosDe = signal<number | null>(null);
  protected readonly cargando = signal(false);
  protected readonly aviso = signal<{ texto: string; error: boolean } | null>(null);
  protected readonly elegidas = new Set<number>();
  protected nuevo = { nombre: '', descripcion: '', inicio: '', fin: '', aforoMaximo: 100,
    publico: 'COMUNIDAD_UAL' as PublicoEvento };

  ngOnInit(): void {
    this.instalacionesApi.catalogo().subscribe({ next: (l) => this.instalaciones.set(l) });
    this.cargar();
  }

  protected alternar(id: number): void {
    if (this.elegidas.has(id)) {
      this.elegidas.delete(id);
    } else {
      this.elegidas.add(id);
    }
  }

  protected crear(): void {
    const n = this.nuevo;
    this.api
      .crearEvento({ nombre: n.nombre.trim(), descripcion: n.descripcion.trim() || null,
        inicio: desdeDatetimeLocal(n.inicio), fin: desdeDatetimeLocal(n.fin), aforoMaximo: Number(n.aforoMaximo),
        idInstalaciones: [...this.elegidas], publico: n.publico })
      .subscribe({
        next: (r) => {
          this.nuevo = { nombre: '', descripcion: '', inicio: '', fin: '', aforoMaximo: 100, publico: 'COMUNIDAD_UAL' };
          this.elegidas.clear();
          this.hecho(`Evento «${r.evento.nombre}» creado (planificado). Reservas canceladas: ${r.reservasCanceladas}. `
            + 'Publícalo para abrir las inscripciones.');
        },
        error: (err: unknown) => this.fallo(err),
      });
  }

  protected publicar(e: Evento): void {
    this.api.publicarEvento(e.id).subscribe({
      next: () => this.hecho(`«${e.nombre}» publicado: ya admite inscripciones.`),
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected cancelar(e: Evento): void {
    this.api.cancelarEvento(e.id).subscribe({
      next: (r) => this.hecho(`«${e.nombre}» cancelado: ${r.inscripcionesAnuladas} inscripciones anuladas e instalaciones liberadas.`),
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected verInscritos(e: Evento): void {
    if (this.inscritosDe() === e.id) {
      this.inscritosDe.set(null);
      return;
    }
    this.api.inscritosEvento(e.id).subscribe({
      next: (l) => {
        this.inscritos.set(l);
        this.inscritosDe.set(e.id);
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.eventos().subscribe({
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
