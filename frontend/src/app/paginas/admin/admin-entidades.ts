import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../core/admin.service';
import { mensajeDeError } from '../../core/errores';
import { fechaLegible } from '../../core/fechas';
import { EntidadAdmin, EntidadForm } from '../../core/modelos';

/**
 * P25 · Entidades con convenio (las da de alta la administración; varios representantes por entidad).
 * P32 · Ausencias a reservas institucionales y suspensión temporal, a decisión de la administración
 * a partir de 3 ausencias durante el convenio.
 */
@Component({
  selector: 'app-admin-entidades',
  imports: [FormsModule, RouterLink, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatIconModule,
    MatInputModule, MatProgressBarModule],
  template: `
    @if (aviso(); as a) {
      <p class="aviso" [class.error]="a.error" [attr.role]="a.error ? 'alert' : 'status'">{{ a.texto }}</p>
    }

    @if (form(); as f) {
      <div class="tarjeta">
        <h3>{{ editando() ? 'Editar entidad' : 'Nueva entidad con convenio' }}</h3>
        <div class="rejilla-form" style="margin-top:8px">
          <mat-form-field appearance="outline"><mat-label>Nombre</mat-label><input matInput [(ngModel)]="f.nombre" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>CIF</mat-label><input matInput [(ngModel)]="f.cif" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Email de contacto</mat-label><input matInput [(ngModel)]="f.emailContacto" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Teléfono</mat-label><input matInput [(ngModel)]="f.telefono" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Inicio del convenio</mat-label><input matInput type="date" [(ngModel)]="f.fechaInicio" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Fin del convenio (opcional)</mat-label><input matInput type="date" [(ngModel)]="f.fechaFin" /></mat-form-field>
          <mat-checkbox [(ngModel)]="f.activa">Convenio activo</mat-checkbox>
        </div>
        <div class="fila-acciones">
          <button mat-button type="button" (click)="form.set(null)">Cancelar</button>
          <button mat-flat-button type="button" (click)="guardar(f)" [disabled]="!f.nombre.trim() || !f.cif.trim() || !f.fechaInicio">Guardar</button>
        </div>
      </div>
    } @else {
      <div class="fila-acciones" style="margin-bottom:12px">
        <button mat-flat-button type="button" (click)="nueva()"><mat-icon>add</mat-icon> Nueva entidad</button>
      </div>
    }

    @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }
    @for (e of lista(); track e.id) {
      <article class="tarjeta" [attr.aria-label]="e.nombre">
        <div class="fila-acciones" style="justify-content:space-between">
          <div>
            <h3>{{ e.nombre }} · {{ e.cif }}</h3>
            <p class="meta">Convenio del {{ fechaLegible(e.fechaInicio) }}{{ e.fechaFin ? ' al ' + fechaLegible(e.fechaFin) : ' (sin fecha de fin)' }}
              · {{ e.emailContacto ?? 'sin email' }}</p>
            <p class="meta">{{ e.representantes }} representante(s)
              @if (e.representantesPendientes > 0) {
                · <a routerLink="../usuarios">{{ e.representantesPendientes }} pendiente(s) de validar</a>
              }
              · {{ e.ausencias }} ausencia(s) en reservas institucionales</p>
            @if (e.suspendidaHasta) {
              <p class="meta">Reservas suspendidas hasta el {{ fechaLegible(e.suspendidaHasta) }}: {{ e.motivoSuspension }}</p>
            }
          </div>
          <div class="fila-acciones">
            <span class="chip" [class.ok]="e.activa && !e.suspendidaHasta" [class.mal]="!e.activa || e.suspendidaHasta">
              {{ !e.activa ? 'Convenio inactivo' : e.suspendidaHasta ? 'Suspendida' : 'Activa' }}
            </span>
            @if (e.puedeSuspenderse && !e.suspendidaHasta) { <span class="chip pendiente">3 o más ausencias</span> }
          </div>
        </div>
        <div class="fila-acciones">
          <button mat-button type="button" (click)="editar(e)">Editar</button>
          @if (e.suspendidaHasta) {
            <button mat-button type="button" (click)="levantar(e)">Levantar suspensión</button>
          } @else if (e.puedeSuspenderse) {
            @if (suspendiendo() === e.id) {
              <mat-form-field appearance="outline" subscriptSizing="dynamic" style="width:170px">
                <mat-label>Hasta</mat-label><input matInput type="date" [(ngModel)]="hasta" />
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic" style="flex:1;min-width:200px">
                <mat-label>Motivo</mat-label><input matInput [(ngModel)]="motivo" />
              </mat-form-field>
              <button mat-button type="button" (click)="suspendiendo.set(null)">Volver</button>
              <button mat-flat-button type="button" (click)="suspender(e)" [disabled]="!hasta || !motivo.trim()">Suspender</button>
            } @else {
              <button mat-stroked-button type="button" (click)="suspendiendo.set(e.id)">Suspender reservas…</button>
            }
          }
        </div>
      </article>
    } @empty {
      @if (!cargando()) { <p class="vacio">No hay entidades con convenio.</p> }
    }
  `,
})
export class AdminEntidadesPage implements OnInit {
  private readonly api = inject(AdminService);

  protected readonly fechaLegible = fechaLegible;
  protected readonly lista = signal<EntidadAdmin[]>([]);
  protected readonly form = signal<EntidadForm | null>(null);
  protected readonly editando = signal<number | null>(null);
  protected readonly suspendiendo = signal<number | null>(null);
  protected readonly cargando = signal(false);
  protected readonly aviso = signal<{ texto: string; error: boolean } | null>(null);
  protected hasta = '';
  protected motivo = '';

  ngOnInit(): void {
    this.cargar();
  }

  protected nueva(): void {
    this.editando.set(null);
    this.form.set({ nombre: '', cif: '', emailContacto: '', telefono: '', fechaInicio: '', fechaFin: '', activa: true });
  }

  protected editar(e: EntidadAdmin): void {
    this.editando.set(e.id);
    this.form.set({ nombre: e.nombre, cif: e.cif, emailContacto: e.emailContacto, telefono: e.telefono,
      fechaInicio: e.fechaInicio, fechaFin: e.fechaFin, activa: e.activa });
  }

  protected guardar(f: EntidadForm): void {
    const datos: EntidadForm = { ...f, fechaFin: f.fechaFin || null };
    const id = this.editando();
    (id ? this.api.editarEntidad(id, datos) : this.api.crearEntidad(datos)).subscribe({
      next: (e) => {
        this.form.set(null);
        this.hecho(`${e.nombre} guardada.`);
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected suspender(e: EntidadAdmin): void {
    this.api.suspenderEntidad(e.id, this.hasta, this.motivo.trim()).subscribe({
      next: (n) => {
        this.suspendiendo.set(null);
        this.hecho(`Reservas institucionales de ${n.nombre} suspendidas hasta el ${fechaLegible(n.suspendidaHasta!)}.`);
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected levantar(e: EntidadAdmin): void {
    this.api.levantarSuspension(e.id).subscribe({
      next: () => this.hecho(`${e.nombre} puede volver a solicitar reservas institucionales.`),
      error: (err: unknown) => this.fallo(err),
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.entidades().subscribe({
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
