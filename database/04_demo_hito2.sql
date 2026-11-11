-- =====================================================================
--  SAD-UAL · Datos de demostración para la defensa del Hito 2 (14/12/2026)
--  MopongoSoft S.L. · PRIS1 Grupo 07
--  Requiere 01_schema.sql y 02_seed.sql. Lo carga docker-compose (no las pruebas
--  automáticas, que usan solo 01 y 02 con un reloj fijo en octubre).
--  Fechas de la semana de la defensa para que el panel y "Mis reservas" tengan datos.
-- =====================================================================
BEGIN;

-- Reservas individuales (una por usuario y día, P1; fuera de las sesiones de la escuela).
INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin, estado) VALUES
  ((SELECT id_usuario FROM usuario WHERE email='alumna.demo@ual.es'),
   (SELECT id_instalacion FROM instalacion WHERE nombre='Pista de pádel 1'),
   DATE '2026-12-15', TIME '18:30', TIME '20:00', 'CONFIRMADA'),
  ((SELECT id_usuario FROM usuario WHERE email='pdi.demo@ual.es'),
   (SELECT id_instalacion FROM instalacion WHERE nombre='Pista de tenis 1'),
   DATE '2026-12-16', TIME '10:00', TIME '11:00', 'CONFIRMADA'),
  ((SELECT id_usuario FROM usuario WHERE email='pas.demo@ual.es'),
   (SELECT id_instalacion FROM instalacion WHERE nombre='Sala multifunción A'),
   DATE '2026-12-17', TIME '12:00', TIME '13:00', 'CONFIRMADA');

-- P21: una reserva institucional pendiente y otra ya autorizada por la administración.
INSERT INTO reserva (id_usuario, id_instalacion, id_entidad, fecha, hora_inicio, hora_fin, estado) VALUES
  ((SELECT id_usuario FROM usuario WHERE email='externo.demo@correo.test'),
   (SELECT id_instalacion FROM instalacion WHERE nombre='Campo de fútbol 7'),
   (SELECT id_entidad FROM entidad_convenio WHERE cif='G00000000'),
   DATE '2026-12-18', TIME '18:00', TIME '20:00', 'PENDIENTE');
INSERT INTO reserva (id_usuario, id_instalacion, id_entidad, id_autorizador, fecha, hora_inicio, hora_fin,
                     estado, fecha_resolucion) VALUES
  ((SELECT id_usuario FROM usuario WHERE email='externo.demo@correo.test'),
   (SELECT id_instalacion FROM instalacion WHERE nombre='Pabellón cubierto'),
   (SELECT id_entidad FROM entidad_convenio WHERE cif='G00000000'),
   (SELECT id_usuario FROM usuario WHERE email='admin.deportes@ual.es'),
   DATE '2026-12-16', TIME '19:00', TIME '21:00', 'CONFIRMADA', TIMESTAMP '2026-12-01 09:00');

COMMIT;
