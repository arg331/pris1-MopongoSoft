import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const URL_LOGIN = '/api/auth/login';

/** Añade "Authorization: Bearer" a las llamadas a la API y gestiona la sesión caducada (401). */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const esApi = req.url.startsWith('/api/');
  const esLogin = req.url === URL_LOGIN;
  const token = esApi && !esLogin ? auth.token : null;
  const peticion = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(peticion).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 401 && esApi && !esLogin) {
        auth.cerrarSesionLocal();
        void router.navigate(['/login'], { queryParams: { caducada: 1 } });
      }
      return throwError(() => err);
    }),
  );
};
