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
import { aFechaIso, desdeDatetimeLocal, fechaHoraLegible, sumarDias } from '../../core/fechas';
import { InstalacionesService } from '../../core/instalaciones.service';
import { Bloqueo, Instalacion, NuevoBloqueo } from '../../core/modelos';

/** F08 · Bloqueos por instalación (P7: cancelan las reservas que pisan) y cierres generales (P11). */
@Component({
  selector: 'app-admin-bloqueos',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressBarModule,
    MatSelectModule],
  template: `
    @if (aviso(); as a) {
      <p class="aviso" [class.error]="a.error" [attr.role]="a.error ? 'alert' : 'status'">{{ a.texto }}</p>
    }

    <details class="tarjeta">
      <summary><strong>Bloquear una instalación</strong> (docencia, mantenimiento, competición…)</summary>
      <div class="rejilla-form" style="margin-top:12px">
        <mat-form-field appearance="outline">
          <mat-label>Instalación</mat-label>
          <mat-select [(ngModel)]="nuevo.idInstalacion">
            @for (i of instalaciones(); track i.id) { <mat-option [value]="i.id">{{ i.nombre }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Motivo</mat-label>
          <mat-select [(ngModel)]="nuevo.motivo">
            @for (m of motivos; track m) { <mat-option [value]="m">{{ m }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Inicio</mat-label>
          <input matInput type="datetime-local" [(ngModel)]="nuevo.inicio" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Fin</mat-label>
          <input matInput type="datetime-local" [(ngModel)]="nuevo.fin" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="completo">
          <mat-label>Observaciones</mat-label>
          <input matInput [(ngModel)]="nuevo.observaciones" />
        </mat-form-field>
        <p class="meta completo">Las reservas que coincidan con el bloqueo se cancelarán (P7).</p>
        <div class="completo fila-acciones">
          <button mat-flat-button type="button" (click)="bloquear()"
                  [disabled]="!nuevo.idInstalacion || !nuevo.inicio || !nuevo.fin">Bloquear</button>
        </div>
      </div>
    </details>

    <details class="tarjeta">
      <summary><strong>Cerrar todas las instalaciones</strong> (festivo, vacaciones, cierre extraordinario)</summary>
      <div class="rejilla-form" style="margin-top:12px">
        <mat-form-field appearance="outline">
          <mat-label>Desde</mat-label>
          <input matInput type="datetime-local" [(ngModel)]="cierre.inicio" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Hasta</mat-label>
          <input matInput type="datetime-local" [(ngModel)]="cierre.fin" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="completo">
          <mat-label>Motivo del cierre</mat-label>
          <input matInput [(ngModel)]="cierre.observaciones" placeholder="Festivo: Día de Andalucía" />
        </mat-form-field>
        <div class="completo fila-acciones">
          <button mat-flat-button type="button" (click)="cerrar()"
                  [disabled]="!cierre.inicio || !cierre.fin || !cierre.observaciones.trim()">Cerrar instalaciones</button>
        </div>
      </div>
    </details>

    <div class="rejilla-form">
      <mat-form-field appearance="outline">
        <mat-label>Desde</mat-label>
        <input matInput type="date" [(ngModel)]="desde" (change)="cargar()" />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Hasta</mat-label>
        <input matInput type="date" [(ngModel)]="hasta" (change)="cargar()" />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Instalación</mat-label>
        <mat-select [(ngModel)]="filtroInstalacion" (ngModelChange)="cargar()">
          <mat-option [value]="null">Todas</mat-option>
          @for (i of instalaciones(); track i.id) { <mat-option [value]="i.id">{{ i.nombre }}</mat-option> }
        </mat-select>
      </mat-form-field>
    </div>

    @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }
    <div class="tabla-envoltorio">
      <table class="tabla">
        <thead><tr><th>Instalación</th><th>Inicio</th><th>Fin</th><th>Motivo</th><th>Observaciones</th><th></th></tr></thead>
        <tbody>
          @for (b of lista(); track b.id) {
            <tr>
              <td>{{ b.instalacion }}</td>
              <td>{{ fechaHoraLegible(b.inicio) }}</td>
              <td>{{ fechaHoraLegible(b.fin) }}</td>
              <td><span class="chip">{{ b.motivo }}</span></td>
              <td class="meta">{{ b.observaciones }}</td>
              <td class="acciones">
                @if (b.gestionadoPor) {
                  <span class="meta">Desde {{ b.gestionadoPor === 'EVENTO' ? 'Eventos' : 'Escuelas' }}</span>
                } @else {
                  <button mat-icon-button type="button" (click)="eliminar(b)" aria-label="Eliminar bloqueo"><mat-icon>delete</mat-icon></button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="6" class="vacio">No hay bloqueos en ese periodo.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class AdminBloqueosPage implements OnInit {
  private readonly api = inject(AdminService);
  private readonly instalacionesApi = inject(InstalacionesService);

  protected readonly fechaHoraLegible = fechaHoraLegible;
  protected readonly motivos: NuevoBloqueo['motivo'][] = ['DOCENCIA', 'MANTENIMIENTO', 'COMPETICION', 'CIERRE', 'OTRO'];
  protected readonly instalaciones = signal<Instalacion[]>([]);
  protected readonly lista = signal<Bloqueo[]>([]);
  protected readonly cargando = signal(false);
  protected readonly aviso = signal<{ texto: string; error: boolean } | null>(null);

  protected desde = aFechaIso(new Date());
  protected hasta = aFechaIso(sumarDias(new Date(), 60));
  protected filtroInstalacion: number | null = null;
  protected nuevo = { idInstalacion: null as number | null, motivo: 'MANTENIMIENTO' as NuevoBloqueo['motivo'],
    inicio: '', fin: '', observaciones: '' };
  protected cierre = { inicio: '', fin: '', observaciones: '' };

  ngOnInit(): void {
    this.instalacionesApi.catalogo().subscribe({ next: (l) => this.instalaciones.set(l) });
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.api.bloqueos(this.filtroInstalacion, this.desde, this.hasta).subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected bloquear(): void {
    const n = this.nuevo;
    this.api
      .crearBloqueo({ idInstalacion: n.idInstalacion!, motivo: n.motivo, inicio: desdeDatetimeLocal(n.inicio),
        fin: desdeDatetimeLocal(n.fin), observaciones: n.observaciones.trim() || null })
      .subscribe({
        next: (r) => this.hecho(`Bloqueo creado. Reservas canceladas: ${r.reservasCanceladas}.`),
        error: (err: unknown) => this.fallo(err),
      });
  }

  protected cerrar(): void {
    const c = this.cierre;
    this.api
      .cerrar({ inicio: desdeDatetimeLocal(c.inicio), fin: desdeDatetimeLocal(c.fin), observaciones: c.observaciones.trim() })
      .subscribe({
        next: (r) => this.hecho(`Cierre aplicado: ${r.bloqueosCreados} bloqueos creados y ${r.reservasCanceladas} reservas canceladas.`),
        error: (err: unknown) => this.fallo(err),
      });
  }

  protected eliminar(b: Bloqueo): void {
    this.api.eliminarBloqueo(b.id).subscribe({
      next: () => this.hecho(`Bloqueo eliminado: ${b.instalacion} vuelve a estar disponible en ese periodo.`),
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
