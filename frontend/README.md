# Frontend · SAD-UAL

Aplicación web adaptada a móvil en **Angular 21** (componentes standalone, signals, sin zone.js)
con TypeScript, Angular Material y RxJS. Consume la API REST del backend (ver [`../backend`](../backend)).

**Responsable:** Daniel Almendros Arévalo · **Respaldo:** Abel Rodríguez Gutiérrez

## Pantallas de la Release V1

| Ruta | Pantalla | Funcionalidades |
|---|---|---|
| `/login` | Inicio de sesión (con atajos a los usuarios demo en desarrollo) | F01 |
| `/instalaciones` | Catálogo filtrable + disponibilidad por día + reserva en dos toques | F02, F03, F04 |
| `/mis-reservas` | Próximas reservas e historial, con cancelación confirmada | F05 |

Navegación: cabecera con enlaces, perfil del usuario y cierre de sesión. Las rutas privadas están
protegidas por `authGuard`; si la sesión caduca (401) el interceptor lleva al login y avisa.

## Estructura

```
src/app
├── core/                   Sesión (AuthService), interceptor JWT, guards, servicios de la API,
│                           modelos (espejo de los DTO del backend), utilidades de fechas y errores
└── paginas/
    ├── login/
    ├── instalaciones/
    └── mis-reservas/
```

## Puesta en marcha

```bash
npm install
npm start          # http://localhost:4200 · /api se redirige a http://localhost:8080 (proxy.conf.json)
npm test           # pruebas unitarias con Vitest
npm run build      # versión de producción en dist/
```

Fuentes (Inter) e iconos (Material Icons) van incluidos en el proyecto, sin CDN: la demo funciona sin Internet.
