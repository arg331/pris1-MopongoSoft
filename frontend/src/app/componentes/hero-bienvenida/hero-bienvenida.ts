import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { FOTOS, fotoUnsplash } from '../../core/estilo-deporte';
import { Usuario } from '../../core/modelos';

interface AccesoRapido {
  ruta: string;
  fragmento?: string;
  texto: string;
  detalle: string;
  icono: string;
  tono: 'naranja' | 'marino' | 'verde' | 'purpura';
}

type Perfil = 'COMUNIDAD' | 'TUTOR' | 'REPRESENTANTE' | 'ADMIN';

const ACCESOS: Record<Perfil, AccesoRapido[]> = {
  COMUNIDAD: [
    { ruta: '/instalaciones', fragmento: 'catalogo', texto: 'Reservar pista', detalle: 'Elige y juega', icono: 'bolt', tono: 'naranja' },
    { ruta: '/mis-reservas', texto: 'Mis reservas', detalle: 'Tu agenda', icono: 'event_available', tono: 'marino' },
    { ruta: '/eventos', texto: 'Eventos UAL', detalle: 'Carreras y torneos', icono: 'emoji_events', tono: 'verde' },
    { ruta: '/escuelas', texto: 'Escuelas', detalle: 'Para peques', icono: 'child_care', tono: 'purpura' },
  ],
  TUTOR: [
    { ruta: '/escuelas', texto: 'Mis menores', detalle: 'Inscripciones y RGPD', icono: 'child_care', tono: 'purpura' },
    { ruta: '/eventos', texto: 'Eventos UAL', detalle: 'Carreras y torneos', icono: 'emoji_events', tono: 'verde' },
    { ruta: '/instalaciones', fragmento: 'catalogo', texto: 'Instalaciones', detalle: 'Consulta horarios', icono: 'stadium', tono: 'marino' },
  ],
  REPRESENTANTE: [
    { ruta: '/institucional', texto: 'Reserva institucional', detalle: 'Para tu entidad', icono: 'apartment', tono: 'naranja' },
    { ruta: '/escuelas', texto: 'Escuelas', detalle: 'Inscribe menores', icono: 'child_care', tono: 'purpura' },
    { ruta: '/eventos', texto: 'Eventos UAL', detalle: 'Carreras y torneos', icono: 'emoji_events', tono: 'verde' },
  ],
  ADMIN: [
    { ruta: '/admin/reservas', texto: 'Reservas', detalle: 'Todas las del día', icono: 'fact_check', tono: 'marino' },
    { ruta: '/admin/institucionales', texto: 'Aprobaciones', detalle: 'Entidades y clubes', icono: 'approval', tono: 'naranja' },
    { ruta: '/admin/bloqueos', texto: 'Bloqueos', detalle: 'Cierres y mantenimiento', icono: 'block', tono: 'purpura' },
    { ruta: '/admin/informes', texto: 'Informes', detalle: 'Ocupación', icono: 'insights', tono: 'verde' },
  ],
};

/** Banner de bienvenida con accesos rápidos según el perfil activo y cifras del día. */
@Component({
  selector: 'app-hero-bienvenida',
  imports: [MatIconModule, RouterLink],
  templateUrl: './hero-bienvenida.html',
  styleUrl: './hero-bienvenida.scss',
})
export class HeroBienvenida {
  readonly usuario = input.required<Usuario>();
  readonly instalaciones = input(0);
  /** null mientras se calcula. */
  readonly libresHoy = input<number | null>(null);

  protected readonly foto = fotoUnsplash(FOTOS.atletismo, 1600, 640);

  protected readonly saludo = computed(() => {
    const h = new Date().getHours();
    return h < 14 ? 'Buenos días' : h < 21 ? 'Buenas tardes' : 'Buenas noches';
  });

  protected readonly perfil = computed<Perfil>(() => {
    const u = this.usuario();
    if (u.rol === 'ADMIN') {
      return 'ADMIN';
    }
    if (u.representante) {
      return 'REPRESENTANTE';
    }
    return u.puedeReservar ? 'COMUNIDAD' : 'TUTOR';
  });

  protected readonly etiquetaPerfil = computed(() => {
    switch (this.perfil()) {
      case 'ADMIN':
        return 'Servicio de Deportes';
      case 'REPRESENTANTE':
        return 'Representante de entidad';
      case 'TUTOR':
        return 'Tutor/a de escuela';
      default:
        return this.usuario().rolNombre;
    }
  });

  protected readonly lema = computed(() => {
    switch (this.perfil()) {
      case 'ADMIN':
        return 'Todo el complejo deportivo, de un vistazo.';
      case 'REPRESENTANTE':
        return 'Reserva espacios para tu club o entidad en pocos pasos.';
      case 'TUTOR':
        return 'Las escuelas deportivas de la UAL, al alcance de tu familia.';
      default:
        return '¿Listo para sudar la camiseta? Tu pista te espera.';
    }
  });

  protected readonly accesos = computed(() => ACCESOS[this.perfil()]);
}
