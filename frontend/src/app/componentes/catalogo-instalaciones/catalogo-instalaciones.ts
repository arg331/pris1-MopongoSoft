import { Component, computed, input, output, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { estiloDeporte } from '../../core/estilo-deporte';
import { ResumenDia } from '../../core/instalaciones.service';
import { Instalacion, TipoInstalacion } from '../../core/modelos';

type Disponible = 'libre' | 'pocas' | 'completa' | 'agotada' | 'cerrada';

interface TarjetaVista {
  inst: Instalacion;
  foto: string;
  icono: string;
  acento: string;
  resumen: ResumenDia | null;
  estado: Disponible | null;
  textoEstado: string;
  porcentajeLibre: number;
}

/**
 * Catálogo de instalaciones (F02): filtros rápidos por tipo, búsqueda local y tarjetas con foto,
 * badges de cristal y disponibilidad del día. Componente de presentación: recibe datos por
 * `input()` y comunica la elección por `output()`; las llamadas al API las hace la página.
 */
@Component({
  selector: 'app-catalogo-instalaciones',
  imports: [MatIconModule],
  templateUrl: './catalogo-instalaciones.html',
  styleUrl: './catalogo-instalaciones.scss',
})
export class CatalogoInstalaciones {
  readonly instalaciones = input.required<Instalacion[]>();
  readonly tipos = input<TipoInstalacion[]>([]);
  readonly filtroTipo = input<number | null>(null);
  readonly cargando = input(false);
  readonly seleccionadaId = input<number | null>(null);
  readonly resumen = input<Record<number, ResumenDia>>({});

  readonly filtrar = output<number | null>();
  readonly seleccionar = output<Instalacion>();

  protected readonly busqueda = signal('');
  /** Fotos que no han cargado (sin red, bloqueadas...): se muestra el degradado con el icono. */
  protected readonly fotosRotas = signal<ReadonlySet<number>>(new Set());
  protected readonly esqueletos = [1, 2, 3, 4];

  protected readonly tarjetas = computed<TarjetaVista[]>(() => {
    const texto = normalizar(this.busqueda());
    const resumen = this.resumen();
    return this.instalaciones()
      .filter((i) => !texto || normalizar(`${i.nombre} ${i.tipo} ${i.ubicacion ?? ''}`).includes(texto))
      .map((inst) => {
        const estilo = estiloDeporte(inst.tipo);
        const r = resumen[inst.id] ?? null;
        const estado = estadoDe(r);
        return {
          inst,
          ...estilo,
          resumen: r,
          estado,
          textoEstado: textoDe(estado, r),
          porcentajeLibre: r && r.total ? Math.round((r.libres / r.total) * 100) : 0,
        };
      });
  });

  protected iconoTipo(nombre: string): string {
    return estiloDeporte(nombre).icono;
  }

  protected fotoRota(id: number): void {
    this.fotosRotas.update((s) => new Set(s).add(id));
  }

  protected buscar(evento: Event): void {
    this.busqueda.set((evento.target as HTMLInputElement).value);
  }
}

function normalizar(s: string): string {
  return s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
}

function estadoDe(r: ResumenDia | null): Disponible | null {
  if (!r) {
    return null;
  }
  if (r.cerrada) {
    return 'cerrada';
  }
  if (r.libres === 0) {
    // Sin franjas libres: "completa" si hay reservas; si no, el día ya no ofrece huecos (pasadas o fuera de plazo).
    return r.reservadas > 0 ? 'completa' : 'agotada';
  }
  return r.libres <= 2 ? 'pocas' : 'libre';
}

function textoDe(estado: Disponible | null, r: ResumenDia | null): string {
  switch (estado) {
    case 'cerrada':
      return 'Cerrada hoy';
    case 'completa':
      return 'Completa hoy';
    case 'agotada':
      return 'Sin huecos hoy';
    case 'pocas':
    case 'libre':
      return `${r!.libres} ${r!.libres === 1 ? 'libre' : 'libres'} hoy`;
    default:
      return '';
  }
}
