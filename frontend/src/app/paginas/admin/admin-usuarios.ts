import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { AdminService } from '../../core/admin.service';
import { AuthService } from '../../core/auth.service';
import { mensajeDeError } from '../../core/errores';
import { AltaUsuario, CodigoRol, UsuarioAdmin } from '../../core/modelos';

/** F07 · Gestión de usuarios: alta de la comunidad UAL (P16), cambio de perfil y activación. */
@Component({
  selector: 'app-admin-usuarios',
  imports: [FormsModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule,
    MatProgressBarModule, MatSelectModule],
  template: `
    @if (aviso(); as a) {
      <p class="aviso" [class.error]="a.error" [attr.role]="a.error ? 'alert' : 'status'">{{ a.texto }}</p>
    }

    <details class="tarjeta">
      <summary><strong>Dar de alta a un miembro de la comunidad universitaria</strong></summary>
      <form [formGroup]="alta" (ngSubmit)="crear()" class="rejilla-form" style="margin-top:12px" novalidate>
        <mat-form-field appearance="outline"><mat-label>Nombre</mat-label><input matInput formControlName="nombre" /></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Apellidos</mat-label><input matInput formControlName="apellidos" /></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Email</mat-label><input matInput type="email" formControlName="email" /></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Teléfono</mat-label><input matInput formControlName="telefono" /></mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Perfil</mat-label>
          <mat-select formControlName="rol">
            @for (r of rolesAlta; track r) { <mat-option [value]="r">{{ r }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Contraseña inicial</mat-label>
          <input matInput formControlName="password" />
          <mat-hint>Al menos 8 caracteres; comunícasela al usuario</mat-hint>
        </mat-form-field>
        <div class="completo fila-acciones"><button mat-flat-button type="submit">Dar de alta</button></div>
      </form>
    </details>

    <div class="rejilla-form">
      <mat-form-field appearance="outline">
        <mat-label>Buscar</mat-label>
        <input matInput [(ngModel)]="texto" (keyup.enter)="cargar()" placeholder="Nombre o email" />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Mostrar</mat-label>
        <mat-select [(ngModel)]="soloPendientes">
          <mat-option [value]="false">Todos</mat-option>
          <mat-option [value]="true">Representantes pendientes de validar</mat-option>
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Perfil</mat-label>
        <mat-select [(ngModel)]="rol" (ngModelChange)="cargar()">
          <mat-option [value]="null">Todos</mat-option>
          @for (r of roles; track r) { <mat-option [value]="r">{{ r }}</mat-option> }
        </mat-select>
      </mat-form-field>
    </div>

    @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }
    <div class="tabla-envoltorio">
      <table class="tabla">
        <thead><tr><th>Usuario</th><th>Perfil</th><th>Entidad</th><th>Estado</th><th></th></tr></thead>
        <tbody>
          @for (u of visibles(); track u.id) {
            <tr>
              <td>{{ u.nombre }} {{ u.apellidos }}<br /><span class="meta">{{ u.email }}</span></td>
              <td>
                <mat-form-field appearance="outline" subscriptSizing="dynamic" style="width:140px">
                  <mat-select [ngModel]="u.rol" (ngModelChange)="cambiarRol(u, $event)" [disabled]="u.id === yo()"
                              [attr.aria-label]="'Perfil de ' + u.nombre">
                    @for (r of roles; track r) { <mat-option [value]="r">{{ r }}</mat-option> }
                  </mat-select>
                </mat-form-field>
              </td>
              <td>{{ u.entidad ?? '—' }}</td>
              <td>
                @if (!u.activo && u.idEntidad) {
                  <span class="chip pendiente">Pendiente de validar</span>
                } @else {
                  <span class="chip" [class.ok]="u.activo" [class.mal]="!u.activo">{{ u.activo ? 'Activo' : 'Desactivado' }}</span>
                }
              </td>
              <td class="acciones">
                @if (u.id !== yo()) {
                  <button mat-button type="button" (click)="activar(u)">
                    {{ u.activo ? 'Desactivar' : (u.idEntidad ? 'Validar y activar' : 'Activar') }}
                  </button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="vacio">No hay usuarios con esos filtros.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class AdminUsuariosPage implements OnInit {
  private readonly api = inject(AdminService);
  private readonly auth = inject(AuthService);

  protected readonly roles: CodigoRol[] = ['ALUMNADO', 'PDI', 'PAS', 'EXTERNO', 'ADMIN'];
  protected readonly rolesAlta: AltaUsuario['rol'][] = ['ALUMNADO', 'PDI', 'PAS', 'ADMIN'];
  protected readonly yo = () => this.auth.usuario()?.id;
  protected readonly lista = signal<UsuarioAdmin[]>([]);
  protected readonly cargando = signal(false);
  protected readonly aviso = signal<{ texto: string; error: boolean } | null>(null);
  protected texto = '';
  protected rol: CodigoRol | null = null;
  protected soloPendientes = false;
  /** P23: representantes registrados que la administración aún no ha validado. */
  protected visibles(): UsuarioAdmin[] {
    return this.soloPendientes ? this.lista().filter((u) => !u.activo && u.idEntidad) : this.lista();
  }

  protected readonly alta = inject(FormBuilder).nonNullable.group({
    nombre: ['', Validators.required],
    apellidos: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telefono: [''],
    rol: ['ALUMNADO' as AltaUsuario['rol'], Validators.required],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.api.usuarios(this.rol, this.texto.trim()).subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected crear(): void {
    if (this.alta.invalid) {
      this.alta.markAllAsTouched();
      return;
    }
    const v = this.alta.getRawValue();
    this.api.altaUsuario({ ...v, telefono: v.telefono.trim() || null }).subscribe({
      next: (u) => {
        this.alta.reset();
        this.hecho(`${u.nombre} ${u.apellidos} dado de alta como ${u.rol}.`);
      },
      error: (err: unknown) => this.fallo(err),
    });
  }

  protected cambiarRol(u: UsuarioAdmin, rol: CodigoRol): void {
    this.api.cambiarRol(u.id, rol).subscribe({
      next: (n) => this.hecho(`${n.nombre} ahora tiene el perfil ${n.rol}.`),
      error: (err: unknown) => {
        this.fallo(err);
        this.cargar();
      },
    });
  }

  protected activar(u: UsuarioAdmin): void {
    this.api.activarUsuario(u.id, !u.activo).subscribe({
      next: (n) => this.hecho(`${n.nombre} ${n.activo ? 'activado' : 'desactivado'}.`),
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
