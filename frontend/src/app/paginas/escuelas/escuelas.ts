import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { mensajeDeError } from '../../core/errores';
import { DIAS_SEMANA, aFechaIso, fechaLegible, horaCorta } from '../../core/fechas';
import { Escuela, Menor } from '../../core/modelos';
import { Variacion7Service } from '../../core/variacion7.service';

/**
 * P17, P19, P20, P22 · Escuelas infantil-juveniles para tutores: alta de menores con el consentimiento
 * RGPD del tutor, inscripción (edad a la fecha de inscripción y plazas, comprobadas por la BD) y baja.
 */
@Component({
  selector: 'app-escuelas',
  imports: [FormsModule, ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatIconModule,
    MatInputModule, MatProgressBarModule, MatSelectModule],
  template: `
    <section class="pagina-media" aria-labelledby="titulo-escuelas">
      <h1 id="titulo-escuelas" class="titulo-pagina">Escuelas deportivas</h1>
      <p class="subtitulo">Inscribe a los menores a tu cargo en las escuelas infantil-juveniles de la temporada.</p>

      @if (aviso(); as a) {
        <p class="aviso" [class.error]="a.error" [attr.role]="a.error ? 'alert' : 'status'">{{ a.texto }}</p>
      }
      @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }

      <h2 class="seccion">Menores a mi cargo</h2>
      @for (m of menores(); track m.id) {
        <article class="tarjeta" [attr.aria-label]="m.nombre + ' ' + m.apellidos">
          <h3>{{ m.nombre }} {{ m.apellidos }}</h3>
          <p class="meta">Nacimiento: {{ fechaLegible(m.fechaNacimiento) }} · consentimiento RGPD dado el
            {{ fechaLegible(m.fechaConsentimiento) }}</p>
          @if (editando() === m.id) {
            <div class="rejilla-form" style="margin-top:8px">
              <mat-form-field appearance="outline"><mat-label>Teléfono del tutor</mat-label>
                <input matInput [(ngModel)]="contacto.telefonoTutor" [ngModelOptions]="{ standalone: true }" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Contacto de emergencia</mat-label>
                <input matInput [(ngModel)]="contacto.contactoEmergenciaNombre" [ngModelOptions]="{ standalone: true }" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Teléfono de emergencia</mat-label>
                <input matInput [(ngModel)]="contacto.contactoEmergenciaTelefono" [ngModelOptions]="{ standalone: true }" /></mat-form-field>
            </div>
            <div class="fila-acciones">
              <button mat-button type="button" (click)="editando.set(null)">Cancelar</button>
              <button mat-flat-button type="button" (click)="guardarContacto(m)">Guardar contacto</button>
            </div>
          } @else {
            <p class="meta">Teléfono del tutor: {{ m.telefonoTutor }} · emergencias: {{ m.contactoEmergenciaNombre }}
              ({{ m.contactoEmergenciaTelefono }})
              <button mat-button type="button" (click)="editarContacto(m)">Corregir</button></p>
          }
          @for (i of m.inscripciones; track i.idEscuela) {
            <div class="fila-acciones" style="justify-content:flex-start">
              <span>{{ i.escuela }} ({{ i.temporada }})</span>
              <span class="chip" [class.ok]="i.estado === 'ACTIVA'">{{ i.estado === 'ACTIVA' ? 'Inscrito' : 'Baja' }}</span>
              @if (i.estado === 'ACTIVA') {
                <button mat-button type="button" (click)="baja(i.idEscuela, m)">Dar de baja</button>
              }
            </div>
          } @empty {
            <p class="meta">Sin inscripciones.</p>
          }
          <div class="fila-acciones">
            @if (retirando() === m.id) {
              <span class="meta">Se eliminarán los datos de {{ m.nombre }} y sus inscripciones.</span>
              <button mat-button type="button" (click)="retirando.set(null)">No</button>
              <button mat-flat-button type="button" (click)="retirar(m)">Sí, retirar</button>
            } @else {
              <button mat-button type="button" (click)="retirando.set(m.id)">Retirar consentimiento</button>
            }
          </div>
        </article>
      } @empty {
        @if (!cargando()) { <p class="vacio">Aún no has registrado a ningún menor.</p> }
      }

      <details class="tarjeta" [open]="menores().length === 0">
        <summary><strong>Registrar a un menor</strong></summary>
        <form [formGroup]="formMenor" (ngSubmit)="registrarMenor()" class="rejilla-form" style="margin-top:12px" novalidate>
          <mat-form-field appearance="outline">
            <mat-label>Nombre</mat-label>
            <input matInput formControlName="nombre" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Apellidos</mat-label>
            <input matInput formControlName="apellidos" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Fecha de nacimiento</mat-label>
            <input matInput type="date" formControlName="fechaNacimiento" [max]="hoy" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Teléfono del tutor</mat-label>
            <input matInput type="tel" formControlName="telefonoTutor" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Contacto de emergencia</mat-label>
            <input matInput formControlName="contactoEmergenciaNombre" placeholder="Nombre y relación" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Teléfono de emergencia</mat-label>
            <input matInput type="tel" formControlName="contactoEmergenciaTelefono" />
          </mat-form-field>
          <mat-checkbox class="completo" formControlName="consentimientoRgpd">
            Como tutor/a legal, doy mi consentimiento para que el Servicio de Deportes trate los datos del menor
            con el fin de gestionar su inscripción en las escuelas deportivas (RGPD). Puedo retirarlo en cualquier
            momento: se eliminarán sus datos y sus inscripciones.
          </mat-checkbox>
          <p class="meta completo">Texto provisional: la Universidad de Almería facilitará el texto legal definitivo
            en la implantación. No se recogen datos médicos.</p>
          <div class="completo fila-acciones">
            <button mat-flat-button type="submit" [disabled]="enviando()">Registrar menor</button>
          </div>
        </form>
      </details>

      <h2 class="seccion">Escuelas de la temporada</h2>
      @for (e of escuelas(); track e.id) {
        <article class="tarjeta" [attr.aria-label]="e.nombre">
          <h3>{{ e.nombre }}</h3>
          <p class="meta">{{ e.instalacion }} · {{ horario(e) }} · del {{ fechaLegible(e.fechaInicio) }} al
            {{ fechaLegible(e.fechaFin) }}</p>
          <div class="fila-acciones">
            <span class="chip">De {{ e.edadMin }} a {{ e.edadMax }} años</span>
            <span class="chip" [class.ok]="e.plazasLibres > 0" [class.mal]="e.plazasLibres === 0">
              {{ e.plazasLibres > 0 ? e.plazasLibres + ' plazas libres' : 'Sin plazas' }}
            </span>
            <span style="flex:1"></span>
            @if (menores().length > 0) {
              <mat-form-field appearance="outline" subscriptSizing="dynamic" style="width:200px">
                <mat-label>Menor</mat-label>
                <mat-select [(ngModel)]="eleccion[e.id]" [ngModelOptions]="{ standalone: true }">
                  @for (m of menores(); track m.id) { <mat-option [value]="m.id">{{ m.nombre }}</mat-option> }
                </mat-select>
              </mat-form-field>
              <button mat-flat-button type="button" (click)="inscribir(e)"
                      [disabled]="!eleccion[e.id] || enviando() || e.plazasLibres === 0">Inscribir</button>
            }
          </div>
        </article>
      } @empty {
        @if (!cargando()) { <p class="vacio">No hay escuelas abiertas en esta temporada.</p> }
      }
    </section>
  `,
})
export class EscuelasPage implements OnInit {
  private readonly api = inject(Variacion7Service);

  protected readonly fechaLegible = fechaLegible;
  protected readonly hoy = aFechaIso(new Date());
  protected readonly escuelas = signal<Escuela[]>([]);
  protected readonly menores = signal<Menor[]>([]);
  protected readonly cargando = signal(true);
  protected readonly enviando = signal(false);
  protected readonly aviso = signal<{ texto: string; error: boolean } | null>(null);
  protected readonly retirando = signal<number | null>(null);
  protected readonly editando = signal<number | null>(null);
  protected contacto = { telefonoTutor: '', contactoEmergenciaNombre: '', contactoEmergenciaTelefono: '' };
  /** Menor elegido en cada escuela. */
  protected readonly eleccion: Record<number, number | undefined> = {};

  protected readonly formMenor = inject(FormBuilder).nonNullable.group({
    nombre: ['', Validators.required],
    apellidos: ['', Validators.required],
    fechaNacimiento: ['', Validators.required],
    telefonoTutor: ['', Validators.required],
    contactoEmergenciaNombre: ['', Validators.required],
    contactoEmergenciaTelefono: ['', Validators.required],
    consentimientoRgpd: [false, Validators.requiredTrue],
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected horario(e: Escuela): string {
    return e.horario
      .map((s) => `${DIAS_SEMANA[s.diaSemana].toLowerCase()} ${horaCorta(s.horaInicio)}–${horaCorta(s.horaFin)}`)
      .join(', ');
  }

  protected registrarMenor(): void {
    if (this.formMenor.invalid) {
      this.formMenor.markAllAsTouched();
      this.aviso.set({
        texto: this.formMenor.controls.consentimientoRgpd.value
          ? 'Completa los datos del menor.'
          : 'Sin el consentimiento del tutor no podemos registrar los datos del menor.',
        error: true,
      });
      return;
    }
    this.enviando.set(true);
    const v = this.formMenor.getRawValue();
    this.api.registrarMenor({ ...v, nombre: v.nombre.trim(), apellidos: v.apellidos.trim(),
      telefonoTutor: v.telefonoTutor.trim(), contactoEmergenciaNombre: v.contactoEmergenciaNombre.trim(),
      contactoEmergenciaTelefono: v.contactoEmergenciaTelefono.trim() }).subscribe({
      next: (m) => {
        this.enviando.set(false);
        this.formMenor.reset();
        this.aviso.set({ texto: `${m.nombre} registrado. Ya puedes inscribirlo en una escuela.`, error: false });
        this.cargar();
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected inscribir(e: Escuela): void {
    const idMenor = this.eleccion[e.id];
    if (!idMenor) {
      return;
    }
    this.enviando.set(true);
    this.api.inscribirMenor(e.id, idMenor).subscribe({
      next: (m) => {
        this.enviando.set(false);
        this.aviso.set({ texto: `${m.nombre} inscrito en ${e.nombre}.`, error: false });
        this.cargar();
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected editarContacto(m: Menor): void {
    this.contacto = { telefonoTutor: m.telefonoTutor, contactoEmergenciaNombre: m.contactoEmergenciaNombre,
      contactoEmergenciaTelefono: m.contactoEmergenciaTelefono };
    this.editando.set(m.id);
  }

  protected guardarContacto(m: Menor): void {
    this.api.actualizarContacto(m.id, this.contacto).subscribe({
      next: () => {
        this.editando.set(null);
        this.aviso.set({ texto: `Datos de contacto de ${m.nombre} actualizados.`, error: false });
        this.cargar();
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  /** P45: se eliminan los datos del menor y sus inscripciones. */
  protected retirar(m: Menor): void {
    this.retirando.set(null);
    this.api.retirarConsentimiento(m.id).subscribe({
      next: () => {
        this.aviso.set({ texto: `Consentimiento retirado: se han eliminado los datos de ${m.nombre}.`, error: false });
        this.cargar();
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected baja(idEscuela: number, m: Menor): void {
    this.enviando.set(true);
    this.api.darDeBaja(idEscuela, m.id).subscribe({
      next: () => {
        this.enviando.set(false);
        this.aviso.set({ texto: `${m.nombre} dado de baja. La plaza queda libre.`, error: false });
        this.cargar();
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  private fallo(err: unknown): void {
    this.enviando.set(false);
    this.aviso.set({ texto: mensajeDeError(err), error: true });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.escuelas().subscribe({
      next: (l) => this.escuelas.set(l),
      error: (err: unknown) => this.fallo(err),
    });
    this.api.menores().subscribe({
      next: (l) => {
        this.menores.set(l);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.cargando.set(false);
        this.fallo(err);
      },
    });
  }
}
