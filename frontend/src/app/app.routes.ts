import { Routes } from '@angular/router';
import { authGuard, invitadoGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [invitadoGuard],
    title: 'Iniciar sesión · SAD-UAL',
    loadComponent: () => import('./paginas/login/login').then((m) => m.LoginPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      {
        path: 'instalaciones',
        title: 'Instalaciones · SAD-UAL',
        loadComponent: () => import('./paginas/instalaciones/instalaciones').then((m) => m.InstalacionesPage),
      },
      {
        path: 'mis-reservas',
        title: 'Mis reservas · SAD-UAL',
        loadComponent: () => import('./paginas/mis-reservas/mis-reservas').then((m) => m.MisReservasPage),
      },
      { path: '', pathMatch: 'full', redirectTo: 'instalaciones' },
    ],
  },
  { path: '**', redirectTo: '' },
];
