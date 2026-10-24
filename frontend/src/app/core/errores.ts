import { HttpErrorResponse } from '@angular/common/http';
import { Problema } from './modelos';

function problema(err: unknown): Problema | null {
  if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object') {
    return err.error as Problema;
  }
  return null;
}

/** Código estable del backend (FRANJA_OCUPADA, RESERVA_AJENA...) o undefined. */
export function codigoDeError(err: unknown): string | undefined {
  return problema(err)?.codigo;
}

/** Mensaje para mostrar al usuario a partir de cualquier error HTTP. */
export function mensajeDeError(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'No se puede conectar con el servidor. Comprueba que el backend está arrancado.';
    }
    const detalle = problema(err)?.detail;
    if (detalle) {
      return detalle;
    }
    if (err.status === 401) {
      return 'Tu sesión ha caducado. Vuelve a iniciar sesión.';
    }
    if (err.status === 403) {
      return 'No tienes permiso para realizar esta acción.';
    }
  }
  return 'Ha ocurrido un error inesperado. Inténtalo de nuevo.';
}
