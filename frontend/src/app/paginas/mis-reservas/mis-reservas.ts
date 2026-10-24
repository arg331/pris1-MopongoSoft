import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { mensajeDeError } from '../../core/errores';
import { desdeFechaIso, horaCorta } from '../../core/fechas';
import { EstadoReserva, Reserva } from '../../core/modelos';
import { ReservasService } from '../../core/reservas.service';

const ETIQUETA_ESTADO: Record<EstadoReserva, string> = {
  PENDIENTE: 'Pendiente',
  CONFIRMADA: 'Confirmada',
  CANCELADA: 'Cancelada',
  NO_PRESENTADO: 'No presentado',
  FINALIZADA: 'Finalizada',
};

/** Pantalla 3 · F05 Mis reservas y cancelación. */
@Component({
  selector: 'app-mis-reservas',
  imports: [DatePipe, RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule],
  templateUrl: './mis-reservas.html',
  styleUrl: './mis-reservas.scss',
})
export class MisReservasPage implements OnInit {
  private readonly api = inject(ReservasService);

  protected readonly horaCorta = horaCorta;
  protected readonly desdeFechaIso = desdeFechaIso;

  protected readonly reservas = signal<Reserva[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly confirmando = signal<number | null>(null);
  protected readonly cancelando = signal<number | null>(null);

  /** Próximas: las que aún se pueden cancelar, de la más cercana a la más lejana. */
  protected readonly proximas = computed(() =>
    this.reservas()
      .filter((r) => r.cancelable)
      .sort((a, b) => (a.fecha + a.horaInicio).localeCompare(b.fecha + b.horaInicio)),
  );
  protected readonly historial = computed(() => this.reservas().filter((r) => !r.cancelable));

  ngOnInit(): void {
    this.cargar();
  }

  protected etiqueta(estado: EstadoReserva): string {
    return ETIQUETA_ESTADO[estado];
  }

  protected pedirConfirmacion(id: number): void {
    this.aviso.set(null);
    this.error.set(null);
    this.confirmando.set(id);
  }

  protected cancelar(reserva: Reserva): void {
    this.confirmando.set(null);
    this.cancelando.set(reserva.id);
    this.api.cancelar(reserva.id).subscribe({
      next: (actualizada) => {
        this.reservas.update((lista) => lista.map((r) => (r.id === actualizada.id ? actualizada : r)));
        this.cancelando.set(null);
        this.error.set(null);
        this.aviso.set(`Reserva cancelada. La franja de ${actualizada.instalacion} vuelve a estar libre.`);
      },
      error: (err: unknown) => {
        this.cancelando.set(null);
        this.error.set(mensajeDeError(err));
        this.cargar();
      },
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.mias().subscribe({
      next: (lista) => {
        this.reservas.set(lista);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.cargando.set(false);
        this.error.set(mensajeDeError(err));
      },
    });
  }
}
