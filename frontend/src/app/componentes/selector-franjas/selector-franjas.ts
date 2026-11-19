import { Component, computed, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { horaCorta } from '../../core/fechas';
import { EstadoFranja, Franja } from '../../core/modelos';

interface GrupoFranjas {
  clave: 'manana' | 'tarde';
  titulo: string;
  icono: string;
  franjas: Franja[];
  libres: number;
}

const ETIQUETA_ESTADO: Record<EstadoFranja, string> = {
  LIBRE: 'Libre',
  RESERVADA: 'Ocupada',
  BLOQUEADA: 'Bloqueada',
  FUERA_DE_PLAZO: 'Fuera de plazo',
  PASADA: 'No disponible',
};

const ICONO_ESTADO: Record<EstadoFranja, string> = {
  LIBRE: 'add_circle',
  RESERVADA: 'person',
  BLOQUEADA: 'lock',
  FUERA_DE_PLAZO: 'schedule',
  PASADA: 'history',
};

/** Las franjas que empiezan antes de las 14:00 son de mañana; el resto, de tarde. */
const INICIO_TARDE = '14:00';

/**
 * Selector visual de franjas horarias (F03/F04): cápsulas agrupadas por mañana y tarde
 * con los estados Libre, Ocupada, Bloqueada, Fuera de plazo y No disponible.
 * Solo las franjas libres se pueden elegir y solo si el perfil puede reservar (P4).
 */
@Component({
  selector: 'app-selector-franjas',
  imports: [MatIconModule],
  templateUrl: './selector-franjas.html',
  styleUrl: './selector-franjas.scss',
})
export class SelectorFranjas {
  readonly franjas = input.required<Franja[]>();
  /** horaInicio de la franja elegida, o null. */
  readonly elegida = input<string | null>(null);
  readonly puedeReservar = input(true);
  readonly cargando = input(false);

  readonly elegir = output<Franja>();

  protected readonly horaCorta = horaCorta;
  protected readonly leyenda: { clase: string; texto: string }[] = [
    { clase: 'libre', texto: 'Libre' },
    { clase: 'reservada', texto: 'Ocupada' },
    { clase: 'bloqueada', texto: 'Bloqueada' },
    { clase: 'fuera_de_plazo', texto: 'Fuera de plazo' },
  ];

  protected readonly grupos = computed<GrupoFranjas[]>(() => {
    const manana = this.franjas().filter((f) => horaCorta(f.horaInicio) < INICIO_TARDE);
    const tarde = this.franjas().filter((f) => horaCorta(f.horaInicio) >= INICIO_TARDE);
    const libres = (l: Franja[]) => l.filter((f) => f.estado === 'LIBRE').length;
    return [
      { clave: 'manana' as const, titulo: 'Mañana', icono: 'wb_sunny', franjas: manana, libres: libres(manana) },
      { clave: 'tarde' as const, titulo: 'Tarde', icono: 'wb_twilight', franjas: tarde, libres: libres(tarde) },
    ].filter((g) => g.franjas.length > 0);
  });

  protected seleccionable(f: Franja): boolean {
    return f.estado === 'LIBRE' && this.puedeReservar();
  }

  protected etiqueta(f: Franja): string {
    if (f.estado === 'BLOQUEADA' && f.motivo) {
      // P11: festivos y cierres extraordinarios se muestran como "Cerrado"
      return f.motivo === 'CIERRE' ? 'Cerrado' : `Bloqueada · ${f.motivo.toLowerCase()}`;
    }
    return ETIQUETA_ESTADO[f.estado];
  }

  protected icono(f: Franja): string {
    return this.elegida() === f.horaInicio ? 'check_circle' : ICONO_ESTADO[f.estado];
  }

  protected pulsar(f: Franja): void {
    if (this.seleccionable(f)) {
      this.elegir.emit(f);
    }
  }
}
