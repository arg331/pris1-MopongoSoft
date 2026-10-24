import { HttpErrorResponse } from '@angular/common/http';
import { codigoDeError, mensajeDeError } from './errores';

describe('errores', () => {
  it('usa el detalle y el código del ProblemDetail del backend', () => {
    const err = new HttpErrorResponse({
      status: 409,
      error: { status: 409, detail: 'La franja ya está reservada', codigo: 'FRANJA_OCUPADA' },
    });
    expect(mensajeDeError(err)).toBe('La franja ya está reservada');
    expect(codigoDeError(err)).toBe('FRANJA_OCUPADA');
  });

  it('explica cuando el backend no responde', () => {
    expect(mensajeDeError(new HttpErrorResponse({ status: 0 }))).toContain('No se puede conectar');
  });

  it('tiene mensajes por defecto para 401 y 403 sin cuerpo', () => {
    expect(mensajeDeError(new HttpErrorResponse({ status: 401 }))).toContain('sesión ha caducado');
    expect(mensajeDeError(new HttpErrorResponse({ status: 403 }))).toContain('permiso');
  });

  it('nunca devuelve vacío ante errores desconocidos', () => {
    expect(mensajeDeError(new Error('x'))).toContain('error inesperado');
    expect(codigoDeError(new Error('x'))).toBeUndefined();
  });
});
