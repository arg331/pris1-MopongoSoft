# Frontend · SAD-UAL

Aplicación web (adaptada a móvil) en **Angular** con TypeScript, Angular Material y RxJS.
Consume la API REST del backend Spring Boot (ver [`../backend`](../backend)).

**Responsable:** Daniel Almendros Arévalo · **Respaldo:** Abel Rodríguez Gutiérrez

## Pantallas previstas para la Release V1 (Hito 1, 26/10/2026)

- Inicio de sesión.
- Listado de instalaciones y consulta de disponibilidad.
- Reserva y cancelación ("Mis reservas").

## Puesta en marcha

El proyecto se generará en esta carpeta con Angular CLI:
```bash
npm install -g @angular/cli
ng new frontend --directory . --routing --style=scss
ng add @angular/material
```

Para arrancarlo en desarrollo:

```bash
npm install
ng serve
```
