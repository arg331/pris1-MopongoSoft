import { Routes } from '@angular/router';
import { adminGuard, authGuard, invitadoGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [invitadoGuard],
    title: 'Iniciar sesión · SAD-UAL',
    loadComponent: () => import('./paginas/login/login').then((m) => m.LoginPage),
  },
  {
    path: 'registro',
    canActivate: [invitadoGuard],
    title: 'Crear cuenta · SAD-UAL',
    loadComponent: () => import('./paginas/registro/registro').then((m) => m.RegistroPage),
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
      {
        path: 'eventos',
        title: 'Eventos · SAD-UAL',
        loadComponent: () => import('./paginas/eventos/eventos').then((m) => m.EventosPage),
      },
      {
        path: 'escuelas',
        title: 'Escuelas · SAD-UAL',
        loadComponent: () => import('./paginas/escuelas/escuelas').then((m) => m.EscuelasPage),
      },
      {
        path: 'institucional',
        title: 'Reservas institucionales · SAD-UAL',
        loadComponent: () => import('./paginas/institucional/institucional').then((m) => m.InstitucionalPage),
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        loadComponent: () => import('./paginas/admin/admin').then((m) => m.AdminPage),
        children: [
          { path: 'reservas', title: 'Reservas · Administración',
            loadComponent: () => import('./paginas/admin/admin-reservas').then((m) => m.AdminReservasPage) },
          { path: 'institucionales', title: 'Institucionales · Administración',
            loadComponent: () => import('./paginas/admin/admin-institucionales').then((m) => m.AdminInstitucionalesPage) },
          { path: 'instalaciones', title: 'Instalaciones · Administración',
            loadComponent: () => import('./paginas/admin/admin-instalaciones').then((m) => m.AdminInstalacionesPage) },
          { path: 'bloqueos', title: 'Bloqueos y cierres · Administración',
            loadComponent: () => import('./paginas/admin/admin-bloqueos').then((m) => m.AdminBloqueosPage) },
          { path: 'entidades', title: 'Entidades · Administración',
            loadComponent: () => import('./paginas/admin/admin-entidades').then((m) => m.AdminEntidadesPage) },
          { path: 'usuarios', title: 'Usuarios · Administración',
            loadComponent: () => import('./paginas/admin/admin-usuarios').then((m) => m.AdminUsuariosPage) },
          { path: 'eventos', title: 'Eventos · Administración',
            loadComponent: () => import('./paginas/admin/admin-eventos').then((m) => m.AdminEventosPage) },
          { path: 'escuelas', title: 'Escuelas · Administración',
            loadComponent: () => import('./paginas/admin/admin-escuelas').then((m) => m.AdminEscuelasPage) },
          { path: 'informes', title: 'Informes · Administración',
            loadComponent: () => import('./paginas/admin/admin-informes').then((m) => m.AdminInformesPage) },
          { path: '', pathMatch: 'full', redirectTo: 'reservas' },
        ],
      },
      { path: '', pathMatch: 'full', redirectTo: 'instalaciones' },
    ],
  },
  { path: '**', redirectTo: '' },
];
