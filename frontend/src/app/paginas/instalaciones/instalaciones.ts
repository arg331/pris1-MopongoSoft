import { DatePipe } from '@angular/common';
import { Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { codigoDeError, mensajeDeError } from '../../core/errores';
import { aFechaIso, horaCorta, inicioDelDia, sumarDias } from '../../core/fechas';
import { InstalacionesService } from '../../core/instalaciones.service';
import { Disponibilidad, Franja, Instalacion, TipoInstalacion } from '../../core/modelos';
import { ReservasService } from '../../core/reservas.service';

interface Aviso {
  tipo: 'ok' | 'error';
  texto: string;
}

const ETIQUETA_ESTADO: Record<Franja['estado'], string> = {
  LIBRE: 'Libre',
  RESERVADA: 'Ocupada',
  BLOQUEADA: 'Bloqueada',
  FUERA_DE_PLAZO: 'Fuera de plazo',
  PASADA: 'No disponible',
};

/** Errores tras los que conviene recargar las franjas para mostrar el estado real. */
const CODIGOS_QUE_REFRESCAN = [
  'FRANJA_OCUPADA', 'FRANJA_BLOQUEADA', 'RESERVA_EN_PASADO', 'ANTELACION_MINIMA', 'CONFLICTO_CONCURRENTE',
];

/**
 * Pantalla 2 · F02 Catálogo + F03 Disponibilidad + F04 Reservar.
 * Flujo de reserva en tres toques: instalación → franja libre → confirmar.
 */
@Component({
  selector: 'app-instalaciones',
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  templateUrl: './instalaciones.html',
  styleUrl: './instalaciones.scss',
})
export class InstalacionesPage implements OnInit {
  private readonly instalacionesApi = inject(InstalacionesService);
  private readonly reservasApi = inject(ReservasService);
  private readonly auth = inject(AuthService);

  private readonly panelDetalle = viewChild<ElementRef<HTMLElement>>('detalle');

  protected readonly hoy = inicioDelDia();
  protected readonly horaCorta = horaCorta;

  protected readonly tipos = signal<TipoInstalacion[]>([]);
  protected readonly instalaciones = signal<Instalacion[]>([]);
  protected readonly filtroTipo = signal<number | null>(null);
  protected readonly cargandoCatalogo = signal(true);

  protected readonly seleccionada = signal<Instalacion | null>(null);
  protected readonly fecha = signal<Date>(this.hoy);
  protected readonly disponibilidad = signal<Disponibilidad | null>(null);
  protected readonly cargandoFranjas = signal(false);

  /** Número de la última consulta de franjas: solo se pinta la respuesta de la más reciente. */
  private ultimaConsultaFranjas = 0;

  protected readonly franjaElegida = signal<Franja | null>(null);
  protected readonly reservando = signal(false);
  protected readonly aviso = signal<Aviso | null>(null);

  protected readonly libres = computed(
    () => this.disponibilidad()?.franjas.filter((f) => f.estado === 'LIBRE').length ?? 0,
  );
  protected readonly esHoy = computed(() => this.fecha().getTime() === this.hoy.getTime());
  /** P4: los perfiles sin reservas individuales ven la disponibilidad pero no reservan. */
  protected readonly puedeReservar = computed(() => this.auth.usuario()?.puedeReservar ?? false);

  ngOnInit(): void {
    this.instalacionesApi.tipos().subscribe({
      next: (tipos) => this.tipos.set(tipos),
      error: (err: unknown) => this.mostrarError(err),
    });
    this.cargarCatalogo();
  }

  protected filtrar(idTipo: number | null): void {
    this.filtroTipo.set(idTipo);
    this.cargarCatalogo();
  }

  protected seleccionar(instalacion: Instalacion): void {
    this.seleccionada.set(instalacion);
    this.aviso.set(null);
    this.cargarDisponibilidad();
    // En móvil el panel queda debajo del catálogo: lo traemos a la vista.
    if (typeof matchMedia === 'function' && matchMedia('(max-width: 800px)').matches) {
      this.panelDetalle()?.nativeElement.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
    }
  }

  protected cambiarFecha(fecha: Date | null): void {
    if (!fecha) {
      return;
    }
    const dia = inicioDelDia(fecha);
    this.fecha.set(dia < this.hoy ? this.hoy : dia);
    this.aviso.set(null);
    this.cargarDisponibilidad();
  }

  protected moverDia(dias: number): void {
    this.cambiarFecha(sumarDias(this.fecha(), dias));
  }

  protected elegir(franja: Franja): void {
    if (franja.estado === 'LIBRE' && this.puedeReservar()) {
      this.franjaElegida.set(franja);
      this.aviso.set(null);
    }
  }

  protected confirmarReserva(): void {
    const instalacion = this.seleccionada();
    const franja = this.franjaElegida();
    if (!instalacion || !franja || this.reservando()) {
      return;
    }
    this.reservando.set(true);
    this.reservasApi
      .crear({ idInstalacion: instalacion.id, fecha: aFechaIso(this.fecha()), horaInicio: horaCorta(franja.horaInicio) })
      .subscribe({
        next: (r) => {
          this.reservando.set(false);
          this.aviso.set({
            tipo: 'ok',
            texto: `Reserva confirmada: ${r.instalacion}, ${horaCorta(r.horaInicio)}–${horaCorta(r.horaFin)}.`,
          });
          this.cargarDisponibilidad();
        },
        error: (err: unknown) => {
          this.reservando.set(false);
          this.mostrarError(err);
          // Si otra persona se adelantó, refrescamos para que vea el estado real.
          if (CODIGOS_QUE_REFRESCAN.includes(codigoDeError(err) ?? '')) {
            this.cargarDisponibilidad();
          }
        },
      });
  }

  protected etiqueta(franja: Franja): string {
    if (franja.estado === 'BLOQUEADA' && franja.motivo) {
      // P11: festivos y cierres extraordinarios se muestran como "Cerrado"
      return franja.motivo === 'CIERRE' ? 'Cerrado' : `Bloqueada · ${franja.motivo.toLowerCase()}`;
    }
    return ETIQUETA_ESTADO[franja.estado];
  }

  private cargarCatalogo(): void {
    this.cargandoCatalogo.set(true);
    this.instalacionesApi.catalogo(this.filtroTipo()).subscribe({
      next: (lista) => {
        this.instalaciones.set(lista);
        this.cargandoCatalogo.set(false);
        const actual = this.seleccionada();
        if (actual && !lista.some((i) => i.id === actual.id)) {
          this.seleccionada.set(null);
          this.disponibilidad.set(null);
        }
      },
      error: (err: unknown) => {
        this.cargandoCatalogo.set(false);
        this.mostrarError(err);
      },
    });
  }

  private cargarDisponibilidad(): void {
    const instalacion = this.seleccionada();
    if (!instalacion) {
      return;
    }
    this.franjaElegida.set(null);
    this.cargandoFranjas.set(true);
    // Si el usuario cambia de instalación o de día antes de que llegue la respuesta, las
    // respuestas anteriores pueden llegar desordenadas: se descartan todas menos la última.
    const consulta = ++this.ultimaConsultaFranjas;
    this.instalacionesApi.disponibilidad(instalacion.id, aFechaIso(this.fecha())).subscribe({
      next: (d) => {
        if (consulta !== this.ultimaConsultaFranjas) {
          return;
        }
        this.disponibilidad.set(d);
        this.cargandoFranjas.set(false);
      },
      error: (err: unknown) => {
        if (consulta !== this.ultimaConsultaFranjas) {
          return;
        }
        this.cargandoFranjas.set(false);
        this.disponibilidad.set(null);
        this.mostrarError(err);
      },
    });
  }

  private mostrarError(err: unknown): void {
    this.aviso.set({ tipo: 'error', texto: mensajeDeError(err) });
  }
}
