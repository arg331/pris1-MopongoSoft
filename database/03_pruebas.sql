-- =====================================================================
--  SAD-UAL · Pruebas de verificación del modelo de datos
--  MopongoSoft S.L. · PRIS1 Grupo 07
--  Ejecutar tras 01_schema.sql y 02_seed.sql. No deja datos residuales.
--  Cada prueba (T1–T16) muestra OK o FALLO mediante NOTICE.
--  Ojo: T = prueba; P1–P10 son las preguntas al cliente (G2).
-- =====================================================================

-- T1. Doble reserva solapada en la misma pista -> debe rechazarse
DO $$
BEGIN
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (1, 1, DATE '2026-10-27', TIME '10:00', TIME '11:30');
    RAISE NOTICE 'T1 FALLO: se ha permitido una doble reserva';
EXCEPTION WHEN exclusion_violation THEN
    RAISE NOTICE 'T1 OK: doble reserva rechazada por ex_reserva_sin_solape';
END $$;

-- T2. Franja contigua (11:00-12:30) en la misma pista -> debe aceptarse
DO $$
BEGIN
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (1, 1, DATE '2026-10-27', TIME '11:00', TIME '12:30');
    RAISE NOTICE 'T2 OK: franja contigua aceptada';
    RAISE EXCEPTION 'rollback';
EXCEPTION WHEN raise_exception THEN NULL;
END $$;

-- T3. Una reserva cancelada libera la franja
DO $$
BEGIN
    UPDATE reserva SET estado = 'CANCELADA', fecha_cancelacion = now()
     WHERE id_instalacion = 1 AND fecha = DATE '2026-10-27';
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (1, 1, DATE '2026-10-27', TIME '09:30', TIME '11:00');
    RAISE NOTICE 'T3 OK: franja liberada tras cancelación';
    RAISE EXCEPTION 'rollback';
EXCEPTION WHEN raise_exception THEN NULL;
END $$;

-- T4. Reserva sobre una franja bloqueada -> debe rechazarse
DO $$
BEGIN
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (2, 6, DATE '2026-10-26', TIME '10:00', TIME '11:00');
    RAISE NOTICE 'T4 FALLO: se ha reservado una franja bloqueada';
EXCEPTION WHEN SQLSTATE 'P0001' THEN
    RAISE NOTICE 'T4 OK: %', SQLERRM;
END $$;

-- T5. Reserva fuera del horario de apertura (domingo) -> debe rechazarse
DO $$
BEGIN
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (2, 3, DATE '2026-11-01', TIME '10:00', TIME '11:00');
    RAISE NOTICE 'T5 FALLO: se ha reservado fuera de horario';
EXCEPTION WHEN SQLSTATE 'P0002' THEN
    RAISE NOTICE 'T5 OK: %', SQLERRM;
END $$;

-- T6. Hora de fin anterior a la de inicio -> debe rechazarse
DO $$
BEGIN
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (2, 3, DATE '2026-10-27', TIME '12:00', TIME '11:00');
    RAISE NOTICE 'T6 FALLO';
EXCEPTION WHEN check_violation THEN
    RAISE NOTICE 'T6 OK: ck_reserva_horas impide franjas incoherentes';
END $$;

-- T7. Email duplicado -> debe rechazarse
DO $$
BEGIN
    INSERT INTO usuario (id_rol, nombre, apellidos, email, password_hash)
    VALUES (1, 'X', 'Y', 'alumna.demo@ual.es', 'x');
    RAISE NOTICE 'T7 FALLO';
EXCEPTION WHEN unique_violation THEN
    RAISE NOTICE 'T7 OK: email único';
END $$;

-- T10. Una reserva origina como máximo una sanción (relación "origina" 1:1 del MER)
DO $$
BEGIN
    INSERT INTO sancion (id_usuario, id_reserva, motivo, fecha_inicio, fecha_fin)
    VALUES (2, 1, 'No presentado', DATE '2026-10-28', DATE '2026-11-04');
    INSERT INTO sancion (id_usuario, id_reserva, motivo, fecha_inicio, fecha_fin)
    VALUES (2, 1, 'No presentado', DATE '2026-10-28', DATE '2026-11-04');
    RAISE NOTICE 'T10 FALLO: una reserva ha originado dos sanciones';
EXCEPTION WHEN unique_violation THEN
    RAISE NOTICE 'T10 OK: una reserva origina como máximo una sanción';
END $$;

-- T11. P1: un usuario no puede tener dos reservas activas el mismo día
DO $$
BEGIN
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (2, 3, DATE '2026-10-27', TIME '12:00', TIME '13:00');   -- alumna ya reservó el 27/10
    RAISE NOTICE 'T11 FALLO: dos reservas el mismo día';
EXCEPTION WHEN unique_violation THEN
    RAISE NOTICE 'T11 OK: una sola reserva por usuario y día';
END $$;

-- T12. P3: no presentarse genera la sanción de esa semana (martes 27 → domingo 01/11)
DO $$
DECLARE v_inicio DATE; v_fin DATE;
BEGIN
    UPDATE reserva SET estado = 'NO_PRESENTADO' WHERE id_reserva = 1;   -- martes 27/10
    SELECT fecha_inicio, fecha_fin INTO v_inicio, v_fin FROM sancion WHERE id_reserva = 1;
    IF v_inicio = DATE '2026-10-27' AND v_fin = DATE '2026-11-01' THEN
        RAISE NOTICE 'T12 OK: sanción del % al %', v_inicio, v_fin;
    ELSE
        RAISE NOTICE 'T12 FALLO: sanción % - %', v_inicio, v_fin;
    END IF;
    RAISE EXCEPTION 'rollback';
EXCEPTION WHEN raise_exception THEN NULL;
END $$;

-- T13. P7: un bloqueo del administrador cancela las reservas que pisa
DO $$
DECLARE v_estado VARCHAR;
BEGIN
    INSERT INTO bloqueo (id_instalacion, id_creador, inicio, fin, motivo)
    VALUES (1, 1, TIMESTAMP '2026-10-27 09:00', TIMESTAMP '2026-10-27 12:00', 'MANTENIMIENTO');
    SELECT estado INTO v_estado FROM reserva WHERE id_reserva = 1;
    IF v_estado = 'CANCELADA' THEN
        RAISE NOTICE 'T13 OK: la reserva solapada queda cancelada';
    ELSE
        RAISE NOTICE 'T13 FALLO: la reserva sigue %', v_estado;
    END IF;
    RAISE EXCEPTION 'rollback';
EXCEPTION WHEN raise_exception THEN NULL;
END $$;

-- T14. P11: el festivo del seed cierra todas las instalaciones y no se puede reservar
DO $$
BEGIN
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (2, 3, DATE '2026-12-08', TIME '10:00', TIME '11:00');
    RAISE NOTICE 'T14 FALLO: se ha reservado en un festivo';
EXCEPTION WHEN SQLSTATE 'P0001' THEN
    RAISE NOTICE 'T14 OK: festivo cerrado en % instalaciones; reserva rechazada',
        (SELECT count(*) FROM bloqueo WHERE motivo = 'CIERRE' AND inicio = TIMESTAMP '2026-12-08 00:00');
END $$;

-- T15. P11: un cierre cancela las reservas existentes y omite instalaciones ya bloqueadas
DO $$
DECLARE v_creados INTEGER; v_estado VARCHAR;
BEGIN
    v_creados := fn_cerrar_instalaciones(TIMESTAMP '2026-10-26 00:00', TIMESTAMP '2026-10-28 00:00',
                                         'Cierre extraordinario de prueba', 1);
    SELECT estado INTO v_estado FROM reserva WHERE id_reserva = 1;     -- pádel 1, 27/10
    IF v_creados = 8 AND v_estado = 'CANCELADA' THEN                  -- 9 activas - pabellón (docencia)
        RAISE NOTICE 'T15 OK: % cierres creados, la reserva afectada queda cancelada', v_creados;
    ELSE
        RAISE NOTICE 'T15 FALLO: % cierres, reserva %', v_creados, v_estado;
    END IF;
    RAISE EXCEPTION 'rollback';
EXCEPTION WHEN raise_exception THEN NULL;
END $$;

-- T16. P17: solo un usuario EXTERNO puede representar a una entidad con convenio
DO $$
BEGIN
    UPDATE usuario SET id_entidad = 1 WHERE email = 'alumna.demo@ual.es';
    RAISE NOTICE 'T16 FALLO: una alumna representa a una entidad';
EXCEPTION WHEN SQLSTATE 'P0006' THEN
    RAISE NOTICE 'T16 OK: %', SQLERRM;
END $$;

-- T8. Consulta de disponibilidad (funcionalidad Must de la V1)
SELECT 'T8 franjas pista de pádel 1, 27/10/2026' AS prueba;
SELECT * FROM fn_franjas_disponibles(1, DATE '2026-10-27');

-- T9. Vista de reservas
SELECT 'T9 v_reservas_detalle' AS prueba;
SELECT id_reserva, fecha, hora_inicio, instalacion, usuario, rol, estado
FROM v_reservas_detalle ORDER BY fecha, hora_inicio;
