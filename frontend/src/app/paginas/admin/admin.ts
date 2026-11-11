import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/** Panel de administración del Servicio de Deportes (solo perfil ADMIN, adminGuard). */
@Component({
  selector: 'app-admin',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <section class="pagina-ancha" aria-labelledby="titulo-admin">
      <h1 id="titulo-admin" class="titulo-pagina">Administración</h1>
      <nav class="pestanas" aria-label="Secciones del panel">
        @for (s of secciones; track s.ruta) {
          <a [routerLink]="s.ruta" routerLinkActive="activa">{{ s.texto }}</a>
        }
      </nav>
      <router-outlet />
    </section>
  `,
})
export class AdminPage {
  protected readonly secciones = [
    { ruta: 'reservas', texto: 'Reservas' },
    { ruta: 'institucionales', texto: 'Institucionales' },
    { ruta: 'instalaciones', texto: 'Instalaciones' },
    { ruta: 'bloqueos', texto: 'Bloqueos y cierres' },
    { ruta: 'usuarios', texto: 'Usuarios' },
    { ruta: 'entidades', texto: 'Entidades' },
    { ruta: 'eventos', texto: 'Eventos' },
    { ruta: 'escuelas', texto: 'Escuelas' },
    { ruta: 'informes', texto: 'Informes' },
  ];
}
