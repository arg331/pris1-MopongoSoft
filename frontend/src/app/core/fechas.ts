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
