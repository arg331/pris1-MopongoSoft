import { aFechaIso, desdeFechaIso, horaCorta, inicioDelDia, sumarDias } from './fechas';

describe('fechas', () => {
  it('aFechaIso usa la fecha local, sin desplazamiento a UTC', () => {
    expect(aFechaIso(new Date(2026, 9, 27, 0, 30))).toBe('2026-10-27');
    expect(aFechaIso(new Date(2026, 0, 5, 23, 59))).toBe('2026-01-05');
  });

  it('desdeFechaIso y aFechaIso son inversas', () => {
    expect(aFechaIso(desdeFechaIso('2026-11-01'))).toBe('2026-11-01');
  });

  it('horaCorta quita los segundos', () => {
    expect(horaCorta('09:30:00')).toBe('09:30');
  });

  it('sumarDias cruza meses y años', () => {
    expect(aFechaIso(sumarDias(new Date(2026, 9, 31), 1))).toBe('2026-11-01');
    expect(aFechaIso(sumarDias(new Date(2026, 11, 31), 1))).toBe('2027-01-01');
  });

  it('inicioDelDia pone la hora a medianoche', () => {
    const d = inicioDelDia(new Date(2026, 9, 23, 17, 45));
    expect([d.getHours(), d.getMinutes(), d.getDate()]).toEqual([0, 0, 23]);
  });
});
