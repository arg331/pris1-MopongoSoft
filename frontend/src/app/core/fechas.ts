/**
 * Fecha local en formato yyyy-MM-dd. No usar toISOString(): convierte a UTC y
 * en España (UTC+1/+2) a primera hora devolvería el día anterior.
 */
export function aFechaIso(fecha: Date): string {
  const anio = fecha.getFullYear();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${anio}-${mes}-${dia}`;
}

/** "yyyy-MM-dd" → Date local a medianoche. */
export function desdeFechaIso(iso: string): Date {
  const [anio, mes, dia] = iso.split('-').map(Number);
  return new Date(anio, mes - 1, dia);
}

/** "09:30:00" → "09:30". */
export function horaCorta(hora: string): string {
  return hora.slice(0, 5);
}

export function inicioDelDia(fecha: Date = new Date()): Date {
  const d = new Date(fecha);
  d.setHours(0, 0, 0, 0);
  return d;
}

export function sumarDias(fecha: Date, dias: number): Date {
  const d = new Date(fecha);
  d.setDate(d.getDate() + dias);
  return d;
}

/** "2026-10-27" + "09:30" → "2026-10-27T09:30:00" (LocalDateTime del backend, sin zona). */
export function fechaHora(fecha: string, hora: string): string {
  return `${fecha}T${hora.length === 5 ? hora + ':00' : hora}`;
}

/** Valor de un <input type="datetime-local"> ("2026-10-27T09:30") → "2026-10-27T09:30:00". */
export function desdeDatetimeLocal(valor: string): string {
  return valor.length === 16 ? `${valor}:00` : valor;
}

/** "2026-10-27T09:30:00" → "27/10/2026 09:30". */
export function fechaHoraLegible(iso: string): string {
  const [fecha, hora = ''] = iso.split('T');
  const [anio, mes, dia] = fecha.split('-');
  return `${dia}/${mes}/${anio}${hora ? ' ' + hora.slice(0, 5) : ''}`;
}

/** "2026-10-27" → "27/10/2026". */
export function fechaLegible(iso: string): string {
  return fechaHoraLegible(iso.slice(0, 10));
}

/** Días de la semana ISO (1 = lunes ... 7 = domingo). */
export const DIAS_SEMANA = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
export const DIAS_CORTOS = ['', 'L', 'M', 'X', 'J', 'V', 'S', 'D'];
