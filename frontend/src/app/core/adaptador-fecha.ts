import { Injectable } from '@angular/core';
import { NativeDateAdapter } from '@angular/material/core';

/**
 * El NativeDateAdapter delega en Date.parse, que interpreta "8/12/2026" como 12 de agosto
 * (formato estadounidense). Este adaptador entiende lo que escribe un usuario en España:
 * día/mes/año, con "/", "-" o "." y año de 2 o 4 cifras.
 */
@Injectable()
export class AdaptadorFechaEs extends NativeDateAdapter {
  override parse(valor: unknown, formato?: object): Date | null {
    if (typeof valor === 'string') {
      const texto = valor.trim();
      if (texto === '') {
        return null;
      }
      const partes = /^(\d{1,2})[/.-](\d{1,2})[/.-](\d{2}|\d{4})$/.exec(texto);
      if (partes) {
        const dia = Number(partes[1]);
        const mes = Number(partes[2]);
        const anio = partes[3].length === 2 ? 2000 + Number(partes[3]) : Number(partes[3]);
        const fecha = new Date(anio, mes - 1, dia);
        // Rechaza fechas imposibles (31/02, 13/13...) en vez de desbordarlas al mes siguiente
        const valida = fecha.getFullYear() === anio && fecha.getMonth() === mes - 1 && fecha.getDate() === dia;
        return valida ? fecha : this.invalid();
      }
    }
    return super.parse(valor, formato);
  }
}
