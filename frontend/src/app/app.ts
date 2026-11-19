import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

export interface EnlaceNav {
  ruta: string;
  texto: string;
  icono: string;
}

/**
 * Armazón de la aplicación: barra lateral en escritorio y barra inferior flotante en móvil.
 * Los enlaces se calculan con Signals a partir del perfil de la sesión.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);

  protected readonly iniciales = computed(() => {
    const u = this.auth.usuario();
    return u ? `${u.nombre.charAt(0)}${u.apellidos.charAt(0)}`.toUpperCase() : '';
  });

  /** Mismas reglas de visibilidad que la navegación anterior (P4, P17, P21, P39). */
  protected readonly enlaces = computed<EnlaceNav[]>(() => {
    const u = this.auth.usuario();
    if (!u) {
      return [];
    }
    const lista: EnlaceNav[] = [{ ruta: '/instalaciones', texto: 'Reservar', icono: 'sports_tennis' }];
    if (u.puedeReservar) {
      lista.push({ ruta: '/mis-reservas', texto: 'Mis reservas', icono: 'event_available' });
    }
    if (!this.auth.esAdmin()) {
      lista.push({ ruta: '/eventos', texto: 'Eventos', icono: 'emoji_events' });
    }
    if (this.auth.puedeTutelar()) {
      lista.push({ ruta: '/escuelas', texto: 'Escuelas', icono: 'child_care' });
    }
    if (this.auth.esRepresentante()) {
      lista.push({ ruta: '/institucional', texto: 'Institucional', icono: 'apartment' });
    }
    if (this.auth.esAdmin()) {
      lista.push({ ruta: '/admin', texto: 'Administración', icono: 'admin_panel_settings' });
    }
    return lista;
  });
}
