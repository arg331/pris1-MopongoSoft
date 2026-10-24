/** Datos de prueba compartidos por los spec (equivalentes al seed 02_seed.sql). */
import { Disponibilidad, Instalacion, LoginResponse, Reserva, TipoInstalacion, Usuario } from './modelos';

export const ALUMNA: Usuario = {
  id: 2,
  nombre: 'Lucía',
  apellidos: 'Martín Ortega',
  email: 'alumna.demo@ual.es',
  rol: 'ALUMNADO',
  rolNombre: 'Alumnado',
  puedeReservar: true,
};

export const EXTERNO: Usuario = {
  id: 5,
  nombre: 'Pablo',
  apellidos: 'Romero Sáez',
  email: 'externo.demo@correo.test',
  rol: 'EXTERNO',
  rolNombre: 'Usuario externo',
  puedeReservar: false,
};

export function respuestaLogin(expiraEn: Date, usuario: Usuario = ALUMNA): LoginResponse {
  return { token: 'token-de-prueba', expiraEn: expiraEn.toISOString(), usuario };
}

export const TIPOS: TipoInstalacion[] = [
  { id: 1, nombre: 'Pádel', descripcion: null },
  { id: 2, nombre: 'Tenis', descripcion: null },
];

export const PADEL_1: Instalacion = {
  id: 1, nombre: 'Pista de pádel 1', idTipo: 1, tipo: 'Pádel', ubicacion: 'Zona exterior norte',
  aforo: 4, duracionFranjaMin: 90, descripcion: null,
};
export const TENIS_1: Instalacion = {
  id: 3, nombre: 'Pista de tenis 1', idTipo: 2, tipo: 'Tenis', ubicacion: 'Zona exterior sur',
  aforo: 4, duracionFranjaMin: 60, descripcion: null,
};

export function disponibilidadPadel(fecha: string): Disponibilidad {
  return {
    idInstalacion: 1,
    instalacion: 'Pista de pádel 1',
    fecha,
    duracionFranjaMin: 90,
    antelacionMinimaHoras: 3,
    franjas: [
      { horaInicio: '08:00:00', horaFin: '09:30:00', estado: 'PASADA', motivo: null },
      { horaInicio: '09:30:00', horaFin: '11:00:00', estado: 'RESERVADA', motivo: null },
      { horaInicio: '11:00:00', horaFin: '12:30:00', estado: 'LIBRE', motivo: null },
      { horaInicio: '12:30:00', horaFin: '14:00:00', estado: 'BLOQUEADA', motivo: 'DOCENCIA' },
    ],
  };
}

export function reserva(parcial: Partial<Reserva>): Reserva {
  return {
    id: 1, idInstalacion: 1, instalacion: 'Pista de pádel 1', tipo: 'Pádel', fecha: '2026-10-27',
    horaInicio: '09:30:00', horaFin: '11:00:00', estado: 'CONFIRMADA', cancelable: true,
    ...parcial,
  };
}
