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
import { DIAS_CORTOS, DIAS_SEMANA, horaCorta } from '../../core/fechas';
import { InstalacionesService } from '../../core/instalaciones.service';
import { Horario, InstalacionAdmin, InstalacionForm, TipoInstalacion } from '../../core/modelos';

interface Formulario extends InstalacionForm {
  horario: Horario[];
}

/** P11 · Horario general: lunes a viernes 08:00–22:00 y sábados 09:00–14:00. */
function horarioGeneral(): Horario[] {
  return [1, 2, 3, 4, 5].map((d) => ({ diaSemana: d, horaApertura: '08:00', horaCierre: '22:00' }))
    .concat({ diaSemana: 6, horaApertura: '09:00', horaCierre: '14:00' });
}

/** F06 · Alta, edición (incluida la duración de franja, P12, y el horario) y baja de instalaciones. */
@Component({
  selector: 'app-admin-instalaciones',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressBarModule,
    MatSelectModule],
  template: `
    @if (aviso(); as a) {
      <p class="aviso" [class.error]="a.error" [attr.role]="a.error ? 'alert' : 'status'">{{ a.texto }}</p>
    }

    @if (form(); as f) {
      <div class="tarjeta">
        <h3>{{ editando() ? 'Editar instalación' : 'Nueva instalación' }}</h3>
        <div class="rejilla-form" style="margin-top:8px">
          <mat-form-field appearance="outline">
            <mat-label>Nombre</mat-label>
            <input matInput [(ngModel)]="f.nombre" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Tipo</mat-label>
            <mat-select [(ngModel)]="f.idTipo">
              @for (t of tipos(); track t.id) { <mat-option [value]="t.id">{{ t.nombre }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Ubicación</mat-label>
            <input matInput [(ngModel)]="f.ubicacion" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Aforo</mat-label>
            <input matInput type="number" min="1" [(ngModel)]="f.aforo" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Duración de la franja (min)</mat-label>
            <input matInput type="number" min="15" max="240" step="15" [(ngModel)]="f.duracionFranjaMin" />
            <mat-hint>No cambia las reservas ya hechas (P12)</mat-hint>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Descripción</mat-label>
            <input matInput [(ngModel)]="f.descripcion" />
          </mat-form-field>
        </div>
        <p class="meta" style="margin-top:12px"><strong>Horario semanal</strong></p>
        @for (h of f.horario; track $index) {
          <div class="fila-acciones" style="justify-content:flex-start">
            <mat-form-field appearance="outline" subscriptSizing="dynamic" style="width:150px">
              <mat-select [(ngModel)]="h.diaSemana" aria-label="Día">
                @for (d of dias; track d) { <mat-option [value]="d">{{ diasSemana[d] }}</mat-option> }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic" style="width:130px">
              <input matInput type="time" [(ngModel)]="h.horaApertura" aria-label="Apertura" />
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic" style="width:130px">
              <input matInput type="time" [(ngModel)]="h.horaCierre" aria-label="Cierre" />
            </mat-form-field>
            <button mat-icon-button type="button" (click)="f.horario.splice($index, 1)" aria-label="Quitar tramo">
              <mat-icon>delete</mat-icon>
            </button>
          </div>
        }
        <div class="fila-acciones" style="justify-content:space-between;margin-top:8px">
          <button mat-button type="button" (click)="f.horario.push({ diaSemana: 1, horaApertura: '08:00', horaCierre: '22:00' })">
            <mat-icon>add</mat-icon> Añadir tramo
          </button>
          <div class="fila-acciones">
            <button mat-button type="button" (click)="form.set(null)">Cancelar</button>
            <button mat-flat-button type="button" (click)="guardar(f)" [disabled]="enviando()">Guardar</button>
          </div>
        </div>
      </div>
    } @else {
      <div class="fila-acciones" style="margin-bottom:12px">
        <button mat-flat-button type="button" (click)="nueva()"><mat-icon>add</mat-icon> Nueva instalación</button>
      </div>
    }

    @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }
    <div class="tabla-envoltorio">
      <table class="tabla">
        <thead><tr><th>Instalación</th><th>Tipo</th><th>Aforo</th><th>Franja</th><th>Horario</th><th>Estado</th><th></th></tr></thead>
        <tbody>
          @for (i of lista(); track i.id) {
            <tr>
              <td>{{ i.nombre }}<br /><span class="meta">{{ i.ubicacion }}</span></td>
              <td>{{ i.tipo }}</td>
              <td>{{ i.aforo }}</td>
              <td>{{ i.duracionFranjaMin }} min</td>
              <td class="meta">{{ resumen(i.horario) }}</td>
              <td>
                <span class="chip" [class.ok]="i.activa" [class.mal]="!i.activa">{{ i.activa ? 'Activa' : 'De baja' }}</span>
                @if (i.reservasFuturas > 0) { <br /><span class="meta">{{ i.reservasFuturas }} reservas futuras</span> }
              </td>
              <td class="acciones">
                <button mat-button type="button" (click)="editar(i)">Editar</button>
                <button mat-button type="button" (click)="cambiarEstado(i)">{{ i.activa ? 'Dar de baja' : 'Reactivar' }}</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class AdminInstalacionesPage implements OnInit {
  private readonly api = inject(AdminService);
  private readonly instalacionesApi = inject(InstalacionesService);

  protected readonly dias = [1, 2, 3, 4, 5, 6, 7];
  protected readonly diasSemana = DIAS_SEMANA;
  protected readonly lista = signal<InstalacionAdmin[]>([]);
  protected readonly tipos = signal<TipoInstalacion[]>([]);
  protected readonly form = signal<Formulario | null>(null);
  protected readonly editando = signal<number | null>(null);
  protected readonly cargando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly aviso = signal<{ texto: string; error: boolean } | null>(null);

  ngOnInit(): void {
    this.instalacionesApi.tipos().subscribe({ next: (t) => this.tipos.set(t) });
    this.cargar();
  }

  protected resumen(horario: Horario[]): string {
    return horario.map((h) => `${DIAS_CORTOS[h.diaSemana]} ${horaCorta(h.horaApertura)}–${horaCorta(h.horaCierre)}`).join(' · ');
  }

  protected nueva(): void {
    this.editando.set(null);
    this.form.set({ idTipo: this.tipos()[0]?.id ?? 1, nombre: '', ubicacion: '', aforo: 4, duracionFranjaMin: 60,
      descripcion: '', horario: horarioGeneral() });
  }

  protected editar(i: InstalacionAdmin): void {
    this.editando.set(i.id);
    this.form.set({ idTipo: i.idTipo, nombre: i.nombre, ubicacion: i.ubicacion, aforo: i.aforo,
      duracionFranjaMin: i.duracionFranjaMin, descripcion: i.descripcion,
      horario: i.horario.map((h) => ({ ...h, horaApertura: horaCorta(h.horaApertura), horaCierre: horaCorta(h.horaCierre) })) });
  }

  protected guardar(f: Formulario): void {
    this.enviando.set(true);
    const datos: InstalacionForm = { ...f, aforo: Number(f.aforo), duracionFranjaMin: Number(f.duracionFranjaMin) };
    const id = this.editando();
    (id ? this.api.editarInstalacion(id, datos) : this.api.crearInstalacion(datos)).subscribe({
      next: (i) => {
        this.enviando.set(false);
        this.form.set(null);
        this.aviso.set({ texto: `${i.nombre} guardada.`, error: false });
        this.cargar();
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected cambiarEstado(i: InstalacionAdmin): void {
    this.api.activarInstalacion(i.id, !i.activa).subscribe({
      next: (n) => {
        this.aviso.set({ texto: `${n.nombre} ${n.activa ? 'reactivada' : 'dada de baja'}.`, error: false });
        this.cargar();
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.instalaciones().subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  private fallo(err: unknown): void {
    this.cargando.set(false);
    this.enviando.set(false);
    this.aviso.set({ texto: mensajeDeError(err), error: true });
  }
}
