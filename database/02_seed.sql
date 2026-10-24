-- =====================================================================
--  SAD-UAL · Datos iniciales (entorno de desarrollo y demo del Hito 1)
--  MopongoSoft S.L. · PRIS1 Grupo 07
--  Requiere haber ejecutado antes 01_schema.sql
--  Contraseña de TODOS los usuarios demo: Sadual2026!  (solo desarrollo)
-- =====================================================================

BEGIN;

-- Perfiles. Límites por perfil reservados para el futuro (ver 01_schema.sql).
-- EXTERNO existe pero, por P4, de momento no hace reservas individuales.
INSERT INTO rol (codigo, nombre, max_reservas_semana, antelacion_max_dias) VALUES
  ('ALUMNADO', 'Alumnado',                                  3,  7),
  ('PDI',      'Personal Docente e Investigador',           3,  7),
  ('PAS',      'Personal de Administración y Servicios',    3,  7),
  ('EXTERNO',  'Usuario externo',                           2,  3),
  ('ADMIN',    'Administración del Servicio de Deportes', 999, 365);

INSERT INTO usuario (id_rol, nombre, apellidos, email, password_hash, telefono) VALUES
  ((SELECT id_rol FROM rol WHERE codigo='ADMIN'),    'Admin',  'Servicio de Deportes', 'admin.deportes@ual.es',
   '$2y$12$q.wXT5k8jab3pMVx/CB1s.AhNTN4kmybpFgMZZk8EYgiYrQ4.Zav2', '950000001'),
  ((SELECT id_rol FROM rol WHERE codigo='ALUMNADO'), 'Lucía',  'Martín Ortega',        'alumna.demo@ual.es',
   '$2y$12$q.wXT5k8jab3pMVx/CB1s.AhNTN4kmybpFgMZZk8EYgiYrQ4.Zav2', NULL),
  ((SELECT id_rol FROM rol WHERE codigo='PDI'),      'Andrés', 'Gil Navarro',          'pdi.demo@ual.es',
   '$2y$12$q.wXT5k8jab3pMVx/CB1s.AhNTN4kmybpFgMZZk8EYgiYrQ4.Zav2', NULL),
  ((SELECT id_rol FROM rol WHERE codigo='PAS'),      'Rosa',   'Pérez Molina',         'pas.demo@ual.es',
   '$2y$12$q.wXT5k8jab3pMVx/CB1s.AhNTN4kmybpFgMZZk8EYgiYrQ4.Zav2', NULL),
  ((SELECT id_rol FROM rol WHERE codigo='EXTERNO'),  'Pablo',  'Romero Sáez',          'externo.demo@correo.test',
   '$2y$12$q.wXT5k8jab3pMVx/CB1s.AhNTN4kmybpFgMZZk8EYgiYrQ4.Zav2', '600000005');

INSERT INTO tipo_instalacion (nombre, descripcion) VALUES
  ('Pádel',             'Pistas de pádel exteriores'),
  ('Tenis',             'Pistas de tenis de pista rápida'),
  ('Fútbol',            'Campo de fútbol de césped artificial'),
  ('Pabellón',          'Pabellón cubierto polideportivo'),
  ('Piscina',           'Piscina cubierta, reservable por calles'),
  ('Sala multifunción', 'Salas para actividades dirigidas');

INSERT INTO instalacion (id_tipo, nombre, ubicacion, aforo, duracion_franja_min) VALUES
  ((SELECT id_tipo FROM tipo_instalacion WHERE nombre='Pádel'),    'Pista de pádel 1',      'Zona exterior norte', 4,  90),
  ((SELECT id_tipo FROM tipo_instalacion WHERE nombre='Pádel'),    'Pista de pádel 2',      'Zona exterior norte', 4,  90),
  ((SELECT id_tipo FROM tipo_instalacion WHERE nombre='Tenis'),    'Pista de tenis 1',      'Zona exterior sur',   4,  60),
  ((SELECT id_tipo FROM tipo_instalacion WHERE nombre='Tenis'),    'Pista de tenis 2',      'Zona exterior sur',   4,  60),
  ((SELECT id_tipo FROM tipo_instalacion WHERE nombre='Fútbol'),   'Campo de fútbol 7',     'Campo principal',    14,  60),
  ((SELECT id_tipo FROM tipo_instalacion WHERE nombre='Pabellón'), 'Pabellón cubierto',     'Edificio deportivo', 40,  60),
  ((SELECT id_tipo FROM tipo_instalacion WHERE nombre='Piscina'),  'Piscina - calle 1',     'Piscina cubierta',    6,  60),
  ((SELECT id_tipo FROM tipo_instalacion WHERE nombre='Piscina'),  'Piscina - calle 2',     'Piscina cubierta',    6,  60),
  ((SELECT id_tipo FROM tipo_instalacion WHERE nombre='Sala multifunción'), 'Sala multifunción A', 'Edificio deportivo, planta 1', 25, 60);

-- Horario: lunes a viernes 08:00-22:00 y sábado 09:00-14:00 en todas las instalaciones.
INSERT INTO horario_instalacion (id_instalacion, dia_semana, hora_apertura, hora_cierre)
SELECT i.id_instalacion, d, TIME '08:00', TIME '22:00'
FROM instalacion i CROSS JOIN generate_series(1, 5) AS d;

INSERT INTO horario_instalacion (id_instalacion, dia_semana, hora_apertura, hora_cierre)
SELECT i.id_instalacion, 6, TIME '09:00', TIME '14:00' FROM instalacion i;

INSERT INTO entidad_convenio (nombre, cif, email_contacto, fecha_inicio) VALUES
  ('Club Deportivo Demo Almería', 'G00000000', 'contacto@clubdemo.test', DATE '2026-09-01');

-- Reservas de ejemplo para la demo (martes 27/10/2026).
INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin, estado) VALUES
  ((SELECT id_usuario FROM usuario WHERE email='alumna.demo@ual.es'),
   (SELECT id_instalacion FROM instalacion WHERE nombre='Pista de pádel 1'),
   DATE '2026-10-27', TIME '09:30', TIME '11:00', 'CONFIRMADA'),
  ((SELECT id_usuario FROM usuario WHERE email='pdi.demo@ual.es'),
   (SELECT id_instalacion FROM instalacion WHERE nombre='Pista de tenis 1'),
   DATE '2026-10-27', TIME '18:00', TIME '19:00', 'CONFIRMADA'),
  ((SELECT id_usuario FROM usuario WHERE email='pas.demo@ual.es'),
   (SELECT id_instalacion FROM instalacion WHERE nombre='Piscina - calle 1'),
   DATE '2026-10-28', TIME '14:00', TIME '15:00', 'CONFIRMADA');

-- Reserva institucional pendiente de autorización (Variación 7).
INSERT INTO reserva (id_usuario, id_instalacion, id_entidad, fecha, hora_inicio, hora_fin, estado) VALUES
  ((SELECT id_usuario FROM usuario WHERE email='externo.demo@correo.test'),
   (SELECT id_instalacion FROM instalacion WHERE nombre='Campo de fútbol 7'),
   (SELECT id_entidad FROM entidad_convenio WHERE cif='G00000000'),
   DATE '2026-10-29', TIME '19:00', TIME '21:00', 'PENDIENTE');

-- Bloqueo por docencia del pabellón (lunes 26/10/2026 por la mañana).
INSERT INTO bloqueo (id_instalacion, id_creador, inicio, fin, motivo, observaciones) VALUES
  ((SELECT id_instalacion FROM instalacion WHERE nombre='Pabellón cubierto'),
   (SELECT id_usuario FROM usuario WHERE email='admin.deportes@ual.es'),
   TIMESTAMP '2026-10-26 08:00', TIMESTAMP '2026-10-26 14:00', 'DOCENCIA',
   'Prácticas del Grado en Ciencias de la Actividad Física y del Deporte');

COMMIT;
