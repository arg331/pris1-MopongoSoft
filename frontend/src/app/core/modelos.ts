/** Tipos que reflejan exactamente los DTO del backend (es.ual.sadual.dto). */

export type CodigoRol = 'ALUMNADO' | 'PDI' | 'PAS' | 'EXTERNO' | 'ADMIN';

export interface Usuario {
  id: number;
  nombre: string;
  apellidos: string;
  email: string;
  rol: CodigoRol;
  rolNombre: string;
  /** P4/P17: solo la comunidad universitaria hace reservas individuales (los externos no). */
  puedeReservar: boolean;
}

export interface LoginResponse {
  token: string;
  expiraEn: string; // ISO-8601
  usuario: Usuario;
}

export interface TipoInstalacion {
  id: number;
  nombre: string;
  descripcion: string | null;
}

export interface Instalacion {
  id: number;
  nombre: string;
  idTipo: number;
  tipo: string;
  ubicacion: string | null;
  aforo: number;
  duracionFranjaMin: number;
  descripcion: string | null;
}

/** FUERA_DE_PLAZO: libre, pero empieza antes de la antelación mínima (P2: 3 horas). */
export type EstadoFranja = 'LIBRE' | 'RESERVADA' | 'BLOQUEADA' | 'FUERA_DE_PLAZO' | 'PASADA';

export interface Franja {
  horaInicio: string; // "HH:mm:ss"
  horaFin: string;
  estado: EstadoFranja;
  motivo: string | null;
}

export interface Disponibilidad {
  idInstalacion: number;
  instalacion: string;
  fecha: string; // "yyyy-MM-dd"
  duracionFranjaMin: number;
  antelacionMinimaHoras: number;
  franjas: Franja[];
}

export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA' | 'NO_PRESENTADO' | 'FINALIZADA';

export interface Reserva {
  id: number;
  idInstalacion: number;
  instalacion: string;
  tipo: string;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  estado: EstadoReserva;
  cancelable: boolean;
}

export interface NuevaReserva {
  idInstalacion: number;
  fecha: string;
  horaInicio: string;
}

/** Cuerpo de error RFC 7807 que devuelve GlobalExceptionHandler. */
export interface Problema {
  status: number;
  title?: string;
  detail?: string;
  codigo?: string;
  errores?: Record<string, string>;
}
