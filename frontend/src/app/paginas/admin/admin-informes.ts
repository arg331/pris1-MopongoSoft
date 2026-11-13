import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AdminService } from '../../core/admin.service';
import { mensajeDeError } from '../../core/errores';
import { aFechaIso, fechaLegible } from '../../core/fechas';
import { Informe } from '../../core/modelos';

/**
 * P9 · Informes del Servicio de Deportes (semanal o mensual): ocupación por instalación y por franja,
 * uso por perfil, cancelaciones y no presentados. Definiciones de cálculo: ❓P51.
 */
@Component({
  selector: 'app-admin-informes',
  imports: [FormsModule, MatButtonModule, MatButtonToggleModule, MatFormFieldModule, MatInputModule, MatProgressBarModule],
  template: `
    <div class="fila-acciones" style="justify-content:flex-start">
      <mat-button-toggle-group [(ngModel)]="periodo" (ngModelChange)="cargar()" aria-label="Periodo">
        <mat-button-toggle value="SEMANAL">Semanal</mat-button-toggle>
        <mat-button-toggle value="MENSUAL">Mensual</mat-button-toggle>
      </mat-button-toggle-group>
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Fecha de referencia</mat-label>
        <input matInput type="date" [(ngModel)]="fecha" (change)="cargar()" />
      </mat-form-field>
    </div>

    @if (error(); as texto) { <p class="aviso error" role="alert">{{ texto }}</p> }
    @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }

    @if (informe(); as i) {
      <p class="subtitulo" style="margin-top:12px">Del {{ fechaLegible(i.desde) }} al {{ fechaLegible(i.hasta) }}</p>

      <h2 class="seccion">Ocupación por instalación</h2>
      <div class="tabla-envoltorio">
        <table class="tabla">
          <thead><tr><th>Instalación</th><th>Horas abiertas</th><th>Bloqueadas</th><th>Reservadas</th><th>Ocupación</th></tr></thead>
          <tbody>
            @for (o of i.ocupacionPorInstalacion; track o.idInstalacion) {
              <tr>
                <td>{{ o.instalacion }}<br /><span class="meta">{{ o.tipo }}</span></td>
                <td>{{ o.horasApertura }}</td><td>{{ o.horasBloqueadas }}</td><td>{{ o.horasReservadas }}</td>
                <td>
                  <div class="barra-porcentaje" [attr.aria-label]="o.porcentaje + ' %'"><span [style.width.%]="o.porcentaje"></span></div>
                  <span class="meta">{{ o.porcentaje }} %</span>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <h2 class="seccion">Reservas por franja horaria</h2>
      @for (f of i.ocupacionPorFranja; track f.hora) {
        <div class="fila-acciones" style="justify-content:flex-start">
          <span style="width:52px">{{ f.hora }}:00</span>
          <div class="barra-porcentaje" style="flex:1"><span [style.width.%]="100 * f.reservas / maxFranja()"></span></div>
          <span style="width:32px;text-align:right">{{ f.reservas }}</span>
        </div>
      } @empty {
        <p class="vacio">Sin reservas en el periodo.</p>
      }

      <h2 class="seccion">Uso por perfil</h2>
      <div class="tabla-envoltorio">
        <table class="tabla">
          <thead><tr><th>Perfil</th><th>Reservas</th><th>Usuarios distintos</th><th>Horas</th></tr></thead>
          <tbody>
            @for (u of i.usoPorPerfil; track u.rol) {
              <tr><td>{{ u.rol }}</td><td>{{ u.reservas }}</td><td>{{ u.usuarios }}</td><td>{{ u.horas }}</td></tr>
            } @empty {
              <tr><td colspan="4" class="vacio">Sin uso en el periodo.</td></tr>
            }
          </tbody>
        </table>
      </div>

      <h2 class="seccion">Cancelaciones y no presentados</h2>
      <div class="rejilla-form">
        <div class="tarjeta">
          <h3>{{ i.cancelaciones.canceladas }} cancelaciones · {{ i.cancelaciones.rechazadas }} institucionales rechazadas</h3>
          @for (c of i.cancelaciones.porInstalacion; track c.instalacion) {
            <p class="meta">{{ c.instalacion }}: {{ c.cantidad }}</p>
          }
        </div>
        <div class="tarjeta">
          <h3>{{ i.noPresentados.total }} no presentados</h3>
          @for (n of i.noPresentados.porUsuario; track n.idUsuario) {
            <p class="meta">{{ n.usuario }} ({{ n.rol }}): {{ n.veces }}</p>
          }
        </div>
      </div>
    }
  `,
})
export class AdminInformesPage implements OnInit {
  private readonly api = inject(AdminService);

  protected readonly fechaLegible = fechaLegible;
  protected periodo: 'SEMANAL' | 'MENSUAL' = 'SEMANAL';
  protected fecha = aFechaIso(new Date());
  protected readonly informe = signal<Informe | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly maxFranja = computed(() =>
    Math.max(1, ...(this.informe()?.ocupacionPorFranja.map((f) => f.reservas) ?? [1])));

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.api.informe(this.periodo, this.fecha || undefined).subscribe({
      next: (i) => {
        this.informe.set(i);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.cargando.set(false);
        this.error.set(mensajeDeError(err));
      },
    });
  }
}
