import { TestBed } from '@angular/core/testing';
import { MAT_DATE_LOCALE } from '@angular/material/core';
import { AdaptadorFechaEs } from './adaptador-fecha';
import { aFechaIso } from './fechas';

describe('AdaptadorFechaEs', () => {
  let adaptador: AdaptadorFechaEs;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [AdaptadorFechaEs, { provide: MAT_DATE_LOCALE, useValue: 'es-ES' }] });
    adaptador = TestBed.inject(AdaptadorFechaEs);
  });

  const iso = (texto: string) => {
    const d = adaptador.parse(texto);
    return d && adaptador.isValid(d) ? aFechaIso(d) : null;
  };

  it('entiende día/mes/año (el 8/12 es 8 de diciembre, no 12 de agosto)', () => {
    expect(iso('8/12/2026')).toBe('2026-12-08');
    expect(iso('27/10/2026')).toBe('2026-10-27');
    expect(iso('01/11/2026')).toBe('2026-11-01');
  });

  it('acepta guiones, puntos, espacios y año de dos cifras', () => {
    expect(iso('8-12-2026')).toBe('2026-12-08');
    expect(iso('8.12.26')).toBe('2026-12-08');
    expect(iso('  8/12/2026 ')).toBe('2026-12-08');
  });

  it('rechaza fechas imposibles en vez de desbordarlas', () => {
    expect(iso('31/02/2026')).toBeNull();
    expect(iso('13/13/2026')).toBeNull();
  });

  it('un texto vacío no es una fecha', () => {
    expect(adaptador.parse('')).toBeNull();
  });
});
