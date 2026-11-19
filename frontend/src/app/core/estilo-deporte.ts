/**
 * Aspecto visual de cada tipo de instalación (foto, icono y color de acento).
 * Solo es presentación: no forma parte del modelo de datos del backend.
 * Fotos libres de Unsplash servidas con parámetros de tamaño y calidad.
 */
export interface EstiloDeporte {
  foto: string;
  icono: string;
  acento: string;
}

const UNSPLASH = 'https://images.unsplash.com/photo-';

/** URL de Unsplash parametrizada (recorte centrado, compresión automática). */
export function fotoUnsplash(id: string, ancho = 800, alto = 520): string {
  return `${UNSPLASH}${id}?auto=format&fit=crop&w=${ancho}&h=${alto}&q=75`;
}

export const FOTOS = {
  padel: '1595435934249-5df7ed86e1c0',
  tenis: '1554068865-24cecd4e34b8',
  futbol: '1574629810360-7efbbe195018',
  pabellon: '1546519638-68e109498ffc',
  piscina: '1530549387789-4c1017266635',
  sala: '1571019613454-1cb2f99b2d8b',
  atletismo: '1461896836934-ffe607ba8211',
  estadio: '1504450758481-7338eba7524a',
} as const;

const ESTILOS: { clave: RegExp; foto: string; icono: string; acento: string }[] = [
  { clave: /p[aá]del/i, foto: FOTOS.padel, icono: 'sports_tennis', acento: '#0ea5e9' },
  { clave: /tenis/i, foto: FOTOS.tenis, icono: 'sports_tennis', acento: '#ea580c' },
  { clave: /f[uú]tbol/i, foto: FOTOS.futbol, icono: 'sports_soccer', acento: '#16a34a' },
  { clave: /pabell[oó]n|baloncesto|voleibol/i, foto: FOTOS.pabellon, icono: 'sports_basketball', acento: '#f59e0b' },
  { clave: /piscina|nataci[oó]n/i, foto: FOTOS.piscina, icono: 'pool', acento: '#0284c7' },
  { clave: /sala|gimnasio|fitness/i, foto: FOTOS.sala, icono: 'self_improvement', acento: '#8b5cf6' },
  { clave: /atletismo|pista/i, foto: FOTOS.atletismo, icono: 'directions_run', acento: '#dc2626' },
];

export function estiloDeporte(tipo: string | null | undefined): EstiloDeporte {
  const e = ESTILOS.find((x) => x.clave.test(tipo ?? ''));
  return e
    ? { foto: fotoUnsplash(e.foto), icono: e.icono, acento: e.acento }
    : { foto: fotoUnsplash(FOTOS.estadio), icono: 'stadium', acento: '#003366' };
}
