import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { mensajeDeError } from '../../core/errores';

/**
 * P17 · Autorregistro de usuarios externos: tutores legales (para inscribir a menores en las escuelas)
 * y representantes de entidades con convenio (para solicitar reservas institucionales).
 * La comunidad universitaria no se registra aquí: la da de alta la administración (P16).
 */
@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatButtonToggleModule, MatFormFieldModule, MatIconModule,
    MatInputModule],
  templateUrl: './registro.html',
  styleUrl: '../login/login.scss',
})
export class RegistroPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  /** P23: el representante queda pendiente de validación por la administración. */
  protected readonly pendiente = signal(false);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    tipo: ['TUTOR' as 'TUTOR' | 'REPRESENTANTE', Validators.required],
    nombre: ['', [Validators.required, Validators.maxLength(60)]],
    apellidos: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    telefono: ['', Validators.maxLength(20)],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    cifEntidad: ['', Validators.pattern(/^[A-Za-z0-9]{9}$/)],
  });

  protected esRepresentante(): boolean {
    return this.form.controls.tipo.value === 'REPRESENTANTE';
  }

  protected registrar(): void {
    const cif = this.form.controls.cifEntidad;
    if (this.esRepresentante() && !cif.value.trim()) {
      cif.setErrors({ required: true });
    }
    // P24: el teléfono es obligatorio para los tutores y opcional para los representantes
    const telefono = this.form.controls.telefono;
    if (!this.esRepresentante() && !telefono.value.trim()) {
      telefono.setErrors({ required: true });
    }
    if (this.form.invalid || this.enviando()) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();
    this.auth
      .registrar({
        tipo: v.tipo,
        nombre: v.nombre.trim(),
        apellidos: v.apellidos.trim(),
        email: v.email.trim(),
        password: v.password,
        telefono: v.telefono.trim() || null,
        cifEntidad: v.tipo === 'REPRESENTANTE' ? v.cifEntidad.trim().toUpperCase() : null,
      })
      .subscribe({
        next: (r) => {
          if (r.pendienteValidacion) {
            this.enviando.set(false);
            this.pendiente.set(true);
          } else {
            void this.router.navigateByUrl('/escuelas');
          }
        },
        error: (err: unknown) => {
          this.error.set(mensajeDeError(err));
          this.enviando.set(false);
        },
      });
  }
}
