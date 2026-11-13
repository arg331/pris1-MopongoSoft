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
  /** P17/P21: externo vinculado a una entidad con convenio (puede solicitar reservas institucionales). */
  representante?: boolean;
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

export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA' | 'NO_PRESENTADO' | 'FINALIZADA' | 'RECHAZADA';

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
  /** P21 (Incremento 2): reserva institucional de un representante y, si se rechazó, el motivo. */
  institucional?: boolean;
  motivoRechazo?: string | null;
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

// ===========================================================================================
// Incremento 2 · Variación 7 (P17–P22)
// ===========================================================================================

export interface Registro {
  tipo: 'TUTOR' | 'REPRESENTANTE';
  nombre: string;
  apellidos: string;
  email: string;
  password: string;
  telefono?: string | null;
  cifEntidad?: string | null;
}

/** P23: el representante queda pendiente de validación (sin token); el tutor entra directamente. */
export interface RegistroResponse {
  token: string | null;
  expiraEn: string | null;
  usuario: Usuario;
  pendienteValidacion: boolean;
}

export interface NuevaReservaInstitucional {
  idInstalacion: number;
  fecha: string;
  horaInicio: string;
  horaFin: string;
}

export interface ReservaInstitucional {
  id: number;
  idInstalacion: number;
  instalacion: string;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  estado: EstadoReserva;
  idEntidad: number;
  entidad: string;
  representante: string;
  emailRepresentante: string;
  autorizador: string | null;
  fechaCreacion: string;
  fechaResolucion: string | null;
  motivoRechazo: string | null;
}

export type EstadoEvento = 'PLANIFICADO' | 'PUBLICADO' | 'CANCELADO' | 'FINALIZADO';
/** P33: la administración decide en cada evento quién puede inscribirse. */
export type PublicoEvento = 'COMUNIDAD_UAL' | 'COMUNIDAD_Y_EXTERNOS';

export interface Evento {
  id: number;
  nombre: string;
  descripcion: string | null;
  inicio: string;
  fin: string;
  aforoMaximo: number;
  estado: EstadoEvento;
  inscritos: number;
  plazasLibres: number;
  instalaciones: string[];
  inscrito: boolean;
  publico: PublicoEvento;
  /** P35: inscripciones y anulaciones hasta 24 h antes del inicio. */
  cierreInscripcion: string;
}

export interface NuevoEvento {
  nombre: string;
  descripcion?: string | null;
  inicio: string; // yyyy-MM-ddTHH:mm:ss
  fin: string;
  aforoMaximo: number;
  idInstalaciones: number[];
  publico: PublicoEvento;
}

export interface ResultadoEvento {
  evento: Evento;
  reservasCanceladas: number;
  inscripcionesAnuladas: number;
}

export interface InscritoEvento {
  idUsuario: number;
  nombre: string;
  email: string;
  rol: CodigoRol;
  fechaInscripcion: string;
  estado: 'ACTIVA' | 'ANULADA';
}

export interface SesionEscuela {
  diaSemana: number; // 1 = lunes ... 7 = domingo
  horaInicio: string;
  horaFin: string;
}

export interface Escuela {
  id: number;
  nombre: string;
  temporada: string;
  fechaInicio: string;
  fechaFin: string;
  idInstalacion: number;
  instalacion: string;
  edadMin: number;
  edadMax: number;
  plazas: number;
  inscritos: number;
  plazasLibres: number;
  diasHorario: string | null;
  horario: SesionEscuela[];
  /** ❓P56: la administración puede cancelar una escuela. */
  estado: 'ACTIVA' | 'CANCELADA';
}

export interface NuevaEscuela {
  idInstalacion: number;
  nombre: string;
  temporada: string;
  fechaInicio: string;
  fechaFin: string;
  edadMin: number;
  edadMax: number;
  plazas: number;
  horario: SesionEscuela[];
}

export interface ResultadoEscuela {
  escuela: Escuela;
  sesionesBloqueadas: number;
  sesionesOmitidas: number;
  reservasCanceladas: number;
}

export interface InscripcionMenor {
  idEscuela: number;
  escuela: string;
  temporada: string;
  fechaInscripcion: string;
  estado: 'ACTIVA' | 'BAJA';
}

export interface Menor {
  id: number;
  nombre: string;
  apellidos: string;
  fechaNacimiento: string;
  fechaConsentimiento: string;
  telefonoTutor: string;
  contactoEmergenciaNombre: string;
  contactoEmergenciaTelefono: string;
  inscripciones: InscripcionMenor[];
}

export interface NuevoMenor {
  nombre: string;
  apellidos: string;
  fechaNacimiento: string;
  /** P44: teléfono del tutor y persona de contacto de emergencia (sin datos médicos). */
  telefonoTutor: string;
  contactoEmergenciaNombre: string;
  contactoEmergenciaTelefono: string;
  consentimientoRgpd: boolean;
}

export interface InscritoEscuela {
  idMenor: number;
  menor: string;
  fechaNacimiento: string;
  tutor: string;
  emailTutor: string;
  telefonoTutor: string | null;
  fechaInscripcion: string;
  estado: 'ACTIVA' | 'BAJA';
}

// ===========================================================================================
// Panel de administración (F06, F07, F08, F10) e informes (P9)
// ===========================================================================================

export interface Horario {
  diaSemana: number;
  horaApertura: string;
  horaCierre: string;
}

export interface InstalacionAdmin extends Instalacion {
  activa: boolean;
  horario: Horario[];
  reservasFuturas: number;
}

export interface InstalacionForm {
  idTipo: number;
  nombre: string;
  ubicacion?: string | null;
  aforo: number;
  duracionFranjaMin: number;
  descripcion?: string | null;
  horario?: Horario[] | null;
}

export interface UsuarioAdmin {
  id: number;
  nombre: string;
  apellidos: string;
  email: string;
  telefono: string | null;
  rol: CodigoRol;
  rolNombre: string;
  activo: boolean;
  fechaAlta: string;
  idEntidad: number | null;
  entidad: string | null;
  reservasFuturas: number;
}

export interface AltaUsuario {
  nombre: string;
  apellidos: string;
  email: string;
  telefono?: string | null;
  rol: 'ALUMNADO' | 'PDI' | 'PAS' | 'ADMIN';
  password: string;
}

export type MotivoBloqueo = 'DOCENCIA' | 'MANTENIMIENTO' | 'COMPETICION' | 'CIERRE' | 'OTRO' | 'EVENTO' | 'ESCUELA';

export interface Bloqueo {
  id: number;
  idInstalacion: number;
  instalacion: string;
  inicio: string;
  fin: string;
  motivo: MotivoBloqueo;
  observaciones: string | null;
  creador: string;
  gestionadoPor: 'EVENTO' | 'ESCUELA' | null;
}

export interface NuevoBloqueo {
  idInstalacion: number;
  inicio: string;
  fin: string;
  motivo: Exclude<MotivoBloqueo, 'EVENTO' | 'ESCUELA'>;
  observaciones?: string | null;
}

export interface Cierre {
  inicio: string;
  fin: string;
  observaciones: string;
}

export interface ResultadoBloqueo {
  bloqueosCreados: number;
  reservasCanceladas: number;
  bloqueo: Bloqueo | null;
}

export interface ReservaAdmin {
  id: number;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  estado: EstadoReserva;
  idInstalacion: number;
  instalacion: string;
  tipo: string;
  idUsuario: number;
  usuario: string;
  rol: CodigoRol;
  entidad: string | null;
}

export interface FiltroReservas {
  desde?: string;
  hasta?: string;
  idInstalacion?: number | null;
  estado?: EstadoReserva | null;
  texto?: string | null;
}

export interface Informe {
  periodo: 'SEMANAL' | 'MENSUAL';
  desde: string;
  hasta: string;
  ocupacionPorInstalacion: {
    idInstalacion: number;
    instalacion: string;
    tipo: string;
    horasApertura: number;
    horasBloqueadas: number;
    horasReservadas: number;
    porcentaje: number;
  }[];
  ocupacionPorFranja: { hora: number; reservas: number }[];
  usoPorPerfil: { rol: string; reservas: number; usuarios: number; horas: number }[];
  cancelaciones: { canceladas: number; rechazadas: number; porInstalacion: { instalacion: string; cantidad: number }[] };
  noPresentados: { total: number; porUsuario: { idUsuario: number; usuario: string; rol: string; veces: number }[] };
}

/** P25/P32 · Entidad con convenio en el panel de administración. */
export interface EntidadAdmin {
  id: number;
  nombre: string;
  cif: string;
  emailContacto: string | null;
  telefono: string | null;
  fechaInicio: string;
  fechaFin: string | null;
  activa: boolean;
  representantes: number;
  representantesPendientes: number;
  ausencias: number;
  puedeSuspenderse: boolean;
  suspendidaHasta: string | null;
  motivoSuspension: string | null;
}

export interface EntidadForm {
  nombre: string;
  cif: string;
  emailContacto?: string | null;
  telefono?: string | null;
  fechaInicio: string;
  fechaFin?: string | null;
  activa: boolean;
}
