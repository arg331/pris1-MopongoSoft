# Backend · API REST (Spring Boot)

Responsable: María Camila Soto Zapata

## Arquitectura

- Java 21 (LTS) y Spring Boot, proyecto Maven generado con Spring Initializr.
- Dependencias: Spring Web, Spring Data JPA, Spring Security, Validation y driver PostgreSQL.
- Capas: `controller` (endpoints REST) → `service` (reglas de negocio) → `repository` (Spring Data JPA) → `entity` (mapeo de las tablas del script).
- Autenticación por perfiles con Spring Security y token JWT. El módulo de autenticación está desacoplado para conectar más adelante el acceso institucional de la UAL (riesgo R4).
- El esquema lo crea `../database/01_schema.sql`; JPA trabaja con `spring.jpa.hibernate.ddl-auto=validate`, sin generar tablas. La API valida y devuelve errores claros; la base de datos es la última barrera contra las dobles reservas.

## Paquetes

```
es.ual.sadual
├── config/        Seguridad (Spring Security, JWT), CORS
├── controller/    AuthController, InstalacionController, ReservaController, admin/
├── service/       Lógica de negocio
├── repository/    Interfaces Spring Data JPA
├── entity/        Usuario, Rol, Instalacion, Reserva, Bloqueo...
├── dto/           Peticiones y respuestas de la API
└── exception/     Traducción de errores a respuestas HTTP
```

## Contrato inicial de la API (Release V1)

| Método | Ruta | Descripción | Func. |
|---|---|---|---|
| POST | `/api/auth/login` · `/api/auth/logout` | Inicio y cierre de sesión; devuelve token JWT y perfil | F01 |
| GET | `/api/instalaciones?tipo=` | Catálogo de instalaciones activas | F02 |
| GET | `/api/instalaciones/{id}/disponibilidad?fecha=YYYY-MM-DD` | Franjas libres y ocupadas (usa `fn_franjas_disponibles`) | F03 |
| POST | `/api/reservas` | Crea una reserva; `409 Conflict` si la franja ya no está libre | F04 |
| GET | `/api/reservas/mias` | Reservas del usuario autenticado | F05 |
| PATCH | `/api/reservas/{id}/cancelar` | Cancela una reserva propia | F05 |
| CRUD | `/api/admin/instalaciones` · `/api/admin/usuarios` · `/api/admin/bloqueos` | Administración (perfil ADMIN) | F06–F08 |

## Traducción de errores de la base de datos (`@RestControllerAdvice`)

| Error PostgreSQL | Causa | Respuesta HTTP |
|---|---|---|
| `23P01` exclusion_violation | Franja ya reservada | 409 Conflict |
| `P0001` | Franja bloqueada | 409 Conflict |
| `P0002` | Fuera del horario de apertura | 422 Unprocessable Entity |
| `23514` check_violation | Datos incoherentes | 422 Unprocessable Entity |

## Arranque

```bash
./mvnw spring-boot:run      # http://localhost:8080
```
