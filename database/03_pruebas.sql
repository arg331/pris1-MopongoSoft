-- =====================================================================
--  SAD-UAL · Pruebas de verificación del modelo de datos
--  MopongoSoft S.L. · PRIS1 Grupo 07
--  Ejecutar tras 01_schema.sql y 02_seed.sql. No deja datos residuales.
--  Cada prueba muestra OK o FALLO mediante NOTICE.
-- =====================================================================

-- P1. Doble reserva solapada en la misma pista -> debe rechazarse
DO $$
BEGIN
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (3, 1, DATE '2026-10-27', TIME '10:00', TIME '11:30');
    RAISE NOTICE 'P1 FALLO: se ha permitido una doble reserva';
EXCEPTION WHEN exclusion_violation THEN
    RAISE NOTICE 'P1 OK: doble reserva rechazada por ex_reserva_sin_solape';
END $$;

-- P2. Franja contigua (11:00-12:30) en la misma pista -> debe aceptarse
DO $$
BEGIN
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (3, 1, DATE '2026-10-27', TIME '11:00', TIME '12:30');
    RAISE NOTICE 'P2 OK: franja contigua aceptada';
    RAISE EXCEPTION 'rollback';
EXCEPTION WHEN raise_exception THEN NULL;
END $$;

-- P3. Una reserva cancelada libera la franja
DO $$
BEGIN
    UPDATE reserva SET estado = 'CANCELADA', fecha_cancelacion = now()
     WHERE id_instalacion = 1 AND fecha = DATE '2026-10-27';
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (3, 1, DATE '2026-10-27', TIME '09:30', TIME '11:00');
    RAISE NOTICE 'P3 OK: franja liberada tras cancelación';
    RAISE EXCEPTION 'rollback';
EXCEPTION WHEN raise_exception THEN NULL;
END $$;

-- P4. Reserva sobre una franja bloqueada -> debe rechazarse
DO $$
BEGIN
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (2, 6, DATE '2026-10-26', TIME '10:00', TIME '11:00');
    RAISE NOTICE 'P4 FALLO: se ha reservado una franja bloqueada';
EXCEPTION WHEN SQLSTATE 'P0001' THEN
    RAISE NOTICE 'P4 OK: %', SQLERRM;
END $$;

-- P5. Reserva fuera del horario de apertura (domingo) -> debe rechazarse
DO $$
BEGIN
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (2, 3, DATE '2026-11-01', TIME '10:00', TIME '11:00');
    RAISE NOTICE 'P5 FALLO: se ha reservado fuera de horario';
EXCEPTION WHEN SQLSTATE 'P0002' THEN
    RAISE NOTICE 'P5 OK: %', SQLERRM;
END $$;

-- P6. Hora de fin anterior a la de inicio -> debe rechazarse
DO $$
BEGIN
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (2, 3, DATE '2026-10-27', TIME '12:00', TIME '11:00');
    RAISE NOTICE 'P6 FALLO';
EXCEPTION WHEN check_violation THEN
    RAISE NOTICE 'P6 OK: ck_reserva_horas impide franjas incoherentes';
END $$;

-- P7. Email duplicado -> debe rechazarse
DO $$
BEGIN
    INSERT INTO usuario (id_rol, nombre, apellidos, email, password_hash)
    VALUES (1, 'X', 'Y', 'alumna.demo@ual.es', 'x');
    RAISE NOTICE 'P7 FALLO';
EXCEPTION WHEN unique_violation THEN
    RAISE NOTICE 'P7 OK: email único';
END $$;

-- P8. Consulta de disponibilidad (funcionalidad Must de la V1)
SELECT 'P8 franjas pista de pádel 1, 27/10/2026' AS prueba;
SELECT * FROM fn_franjas_disponibles(1, DATE '2026-10-27');

-- P9. Vista de reservas
SELECT 'P9 v_reservas_detalle' AS prueba;
SELECT id_reserva, fecha, hora_inicio, instalacion, usuario, rol, estado
FROM v_reservas_detalle ORDER BY fecha, hora_inicio;
