import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { CatalogoInstalaciones } from '../../componentes/catalogo-instalaciones/catalogo-instalaciones';
import { HeroBienvenida } from '../../componentes/hero-bienvenida/hero-bienvenida';
import { SelectorFranjas } from '../../componentes/selector-franjas/selector-franjas';
import { AuthService } from '../../core/auth.service';
import { codigoDeError, mensajeDeError } from '../../core/errores';
import { estiloDeporte } from '../../core/estilo-deporte';
import { aFechaIso, horaCorta, inicioDelDia, sumarDias } from '../../core/fechas';
import { InstalacionesService, ResumenDia } from '../../core/instalaciones.service';
import { Disponibilidad, Franja, Instalacion, TipoInstalacion } from '../../core/modelos';
import { ReservasService } from '../../core/reservas.service';

interface Aviso {
  tipo: 'ok' | 'error';
  texto: string;
}

/** Errores tras los que conviene recargar las franjas para mostrar el estado real. */
const CODIGOS_QUE_REFRESCAN = [
  'FRANJA_OCUPADA', 'FRANJA_BLOQUEADA', 'RESERVA_EN_PASADO', 'ANTELACION_MINIMA', 'CONFLICTO_CONCURRENTE',
];

/** Días que se ven en la tira de fechas. */
const DIAS_TIRA = 7;

/**
 * Pantalla 2 · F02 Catálogo + F03 Disponibilidad + F04 Reservar.
 * Flujo de reserva en tres toques: instalación → franja libre → confirmar.
 * La página coordina el estado (Signals) y las llamadas al API; la presentación vive en
 * HeroBienvenida, CatalogoInstalaciones y SelectorFranjas.
 */
@Component({
  selector: 'app-instalaciones',
  imports: [
    DatePipe,
    NgTemplateOutlet,
    RouterLink,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    HeroBienvenida,
    CatalogoInstalaciones,
    SelectorFranjas,
  ],
  templateUrl: './instalaciones.html',
  styleUrl: './instalaciones.scss',
  host: { '(document:keydown.escape)': 'cerrarPanel()' },
})
export class InstalacionesPage implements OnInit {
  private readonly instalacionesApi = inject(InstalacionesService);
  private readonly reservasApi = inject(ReservasService);
  private readonly auth = inject(AuthService);

  private readonly panelDetalle = viewChild<ElementRef<HTMLElement>>('detalle');

  protected readonly hoy = inicioDelDia();
  protected readonly horaCorta = horaCorta;

  protected readonly usuario = computed(() => this.auth.usuario());
  protected readonly tipos = signal<TipoInstalacion[]>([]);
  protected readonly instalaciones = signal<Instalacion[]>([]);
  protected readonly filtroTipo = signal<number | null>(null);
  protected readonly cargandoCatalogo = signal(true);
  /** Franjas libres de hoy por instalación (badges del catálogo y cifra del hero). */
  protected readonly resumenHoy = signal<Record<number, ResumenDia>>({});
  protected readonly resumenCargado = signal(false);

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
  protected readonly totalFranjas = computed(() => this.disponibilidad()?.franjas.length ?? 0);
  /** Porcentaje libre del día para el anillo de disponibilidad. */
  protected readonly porcentajeLibre = computed(() =>
    this.totalFranjas() ? Math.round((this.libres() / this.totalFranjas()) * 100) : 0,
  );
  protected readonly libresHoyTotal = computed(() =>
    this.resumenCargado() ? Object.values(this.resumenHoy()).reduce((s, r) => s + r.libres, 0) : null,
  );
  protected readonly esHoy = computed(() => this.fecha().getTime() === this.hoy.getTime());
  /** P4: los perfiles sin reservas individuales ven la disponibilidad pero no reservan. */
  protected readonly puedeReservar = computed(() => this.auth.usuario()?.puedeReservar ?? false);
  protected readonly esRepresentante = computed(() => this.auth.usuario()?.representante === true);
  protected readonly estiloSeleccionada = computed(() => estiloDeporte(this.seleccionada()?.tipo));

  /** Tira de 7 días: empieza hoy y se desplaza si la fecha elegida queda fuera. */
  protected readonly tiraDias = computed(() => {
    const elegido = this.fecha();
    const desfase = Math.round((elegido.getTime() - this.hoy.getTime()) / 86_400_000);
    const inicio = desfase < DIAS_TIRA ? this.hoy : sumarDias(elegido, -3);
    return Array.from({ length: DIAS_TIRA }, (_, i) => sumarDias(inicio, i));
  });

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
    // El panel se abre encima del catálogo: llevamos el foco a él (teclado y lectores de pantalla).
    setTimeout(() => this.panelDetalle()?.nativeElement.focus({ preventScroll: true }));
  }

  /** Cierra el panel de reserva (botón, clic fuera o tecla Escape). */
  protected cerrarPanel(): void {
    if (!this.seleccionada()) {
      return;
    }
    this.ultimaConsultaFranjas++; // descarta respuestas pendientes
    this.seleccionada.set(null);
    this.disponibilidad.set(null);
    this.franjaElegida.set(null);
    this.cargandoFranjas.set(false);
    this.aviso.set(null);
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

  protected esElDia(dia: Date): boolean {
    return dia.getTime() === this.fecha().getTime();
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
            texto: `¡Reserva confirmada! ${r.instalacion}, ${horaCorta(r.horaInicio)}–${horaCorta(r.horaFin)}. A darlo todo.`,
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

  private cargarCatalogo(): void {
    this.cargandoCatalogo.set(true);
    this.instalacionesApi.catalogo(this.filtroTipo()).subscribe({
      next: (lista) => {
        this.instalaciones.set(lista);
        this.cargandoCatalogo.set(false);
        this.cargarResumen(lista);
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

  /** Badges "n libres hoy": solo se piden las instalaciones que aún no se conocen. */
  private cargarResumen(lista: Instalacion[]): void {
    const conocidas = this.resumenHoy();
    const pendientes = lista.map((i) => i.id).filter((id) => !(id in conocidas));
    if (pendientes.length === 0) {
      this.resumenCargado.set(true);
      return;
    }
    this.instalacionesApi.resumenDelDia(pendientes, aFechaIso(this.hoy)).subscribe({
      next: (r) => {
        this.resumenHoy.update((actual) => ({ ...actual, ...r }));
        this.resumenCargado.set(true);
      },
      error: () => this.resumenCargado.set(true),
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
        if (this.esHoy()) {
          this.actualizarResumen(instalacion.id, d);
        }
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

  /** Mantiene al día el badge de la tarjeta con la disponibilidad que acabamos de recibir. */
  private actualizarResumen(id: number, d: Disponibilidad): void {
    this.resumenHoy.update((r) => ({
      ...r,
      [id]: {
        total: d.franjas.length,
        libres: d.franjas.filter((f) => f.estado === 'LIBRE').length,
        reservadas: d.franjas.filter((f) => f.estado === 'RESERVADA').length,
        cerrada: d.franjas.length === 0 || d.franjas.every((f) => f.motivo === 'CIERRE'),
      },
    }));
  }

  private mostrarError(err: unknown): void {
    this.aviso.set({ tipo: 'error', texto: mensajeDeError(err) });
  }
}
