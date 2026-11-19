# Frontend · SAD-UAL

Aplicación web adaptada a móvil en **Angular 21** (componentes standalone, signals, sin zone.js)
con TypeScript, Angular Material y RxJS. Consume la API REST del backend (ver [`../backend`](../backend)).

**Responsable:** Daniel Almendros Arévalo · **Respaldo:** Abel Rodríguez Gutiérrez

## Pantallas

### Release V1

| Ruta | Pantalla | Funcionalidades |
|---|---|---|
| `/login` | Inicio de sesión (con atajos a los usuarios demo en desarrollo) | F01 |
| `/instalaciones` | Bienvenida según el perfil, catálogo filtrable, disponibilidad por día y reserva en dos toques | F02, F03, F04 |
| `/mis-reservas` | Próximas reservas e historial, con cancelación confirmada | F05 |

### Incremento 2 (Variación 7)

| Ruta | Pantalla | Quién | Req. |
|---|---|---|---|
| `/registro` | Alta de tutores y representantes (el representante queda pendiente de validar) | Público | P17, P23, P24 |
| `/institucional` | Solicitud de reservas institucionales (varias franjas seguidas, hasta 4 h) y su estado | Representante | P21, P27, P28 |
| `/eventos` | Eventos publicados, plazas libres e inscripción | Comunidad UAL y externos (según el evento) | P18, P33, P35 |
| `/escuelas` | Menores a cargo (con consentimiento RGPD y contacto de emergencia) e inscripción en escuelas | Tutor | P19, P20, P22, P44, P45, P52 |

### Panel de administración (`/admin`, solo ADMIN)

| Ruta | Pantalla | Req. |
|---|---|---|
| `/admin/reservas` | Consulta general de reservas y *No presentado* | F10, F12 |
| `/admin/institucionales` | Bandeja de reservas institucionales: autorizar o rechazar con motivo | P21, P30 |
| `/admin/instalaciones` | Alta, edición, horario, duración de franja, baja y reactivación | F06, P12 |
| `/admin/bloqueos` | Bloqueos de franjas y cierres por festivos | F08, P7, P11 |
| `/admin/entidades` | Entidades con convenio, representantes, ausencias y suspensión | P25, P32 |
| `/admin/usuarios` | Alta de la comunidad UAL, perfil, activación y validación de representantes | F07, P16, P23 |
| `/admin/eventos` | Crear, publicar y cancelar eventos; inscritos | P18 |
| `/admin/escuelas` | Crear escuelas con horario semanal; inscritos con contacto del tutor y **contacto de emergencia** | P19, P46, P52 |
| `/admin/informes` | Informes semanal y mensual de ocupación, uso, cancelaciones y no presentados | P9 |

Navegación: cabecera con enlaces según el perfil, datos del usuario y cierre de sesión. Las rutas privadas
están protegidas por `authGuard` (y `/admin` además por `adminGuard`); `/login` y `/registro`, por
`invitadoGuard`. Si la sesión caduca (401), el interceptor lleva al login y avisa.

## Estructura

```
src/app
├── core/                   Sesión (AuthService), interceptor JWT, guards, servicios de la API,
│                           modelos (espejo de los DTO del backend), utilidades de fechas y errores,
│                           estilo-deporte.ts (foto, icono y color de cada tipo de instalación)
├── componentes/            Componentes de presentación reutilizables (datos por input(), eventos por output())
│   ├── hero-bienvenida/        Banner con accesos rápidos según el perfil y cifras del día
│   ├── catalogo-instalaciones/ Filtros por tipo, búsqueda y tarjetas con foto y disponibilidad (F02)
│   └── selector-franjas/       Franjas por mañana y tarde con su estado; solo se eligen las libres (F03, F04)
└── paginas/
    ├── login/  registro/
    ├── instalaciones/  mis-reservas/
    ├── institucional/  eventos/  escuelas/
    └── admin/              Contenedor del panel y una página por sección
```

Las páginas llaman a la API y pasan los datos a los componentes; los componentes no conocen el backend.

## Puesta en marcha

```bash
npm install
npm start          # http://localhost:4200 · /api se redirige a http://localhost:8080 (proxy.conf.json)
npm test           # pruebas unitarias con Vitest
npm run build      # versión de producción en dist/
```

Resultado a 19/11/2026: 79 pruebas correctas (14 ficheros) y build de producción sin errores.

Fuentes (Inter) e iconos (Material Icons) van incluidos en el proyecto, sin CDN. Las fotos del catálogo se
cargan de Unsplash; sin Internet, la tarjeta se muestra sin foto (con el icono del deporte), así que la demo
sigue funcionando.
