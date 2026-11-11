-- =====================================================================
--  SAD-UAL · Pruebas de verificación del modelo de datos
--  MopongoSoft S.L. · PRIS1 Grupo 07
--  Ejecutar tras 01_schema.sql y 02_seed.sql. No deja datos residuales.
--  Cada prueba (T1–T25) muestra OK o FALLO mediante NOTICE.
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

-- T15. P11 / INC-04: un cierre cancela las reservas y bloquea también los huecos libres de
--      las instalaciones que ya tenían algún bloqueo (pabellón: docencia el 26/10 de 8 a 14)
DO $$
DECLARE v_creados INTEGER; v_estado VARCHAR; v_pabellon_cerrado BOOLEAN;
BEGIN
    v_creados := fn_cerrar_instalaciones(TIMESTAMP '2026-10-26 00:00', TIMESTAMP '2026-10-28 00:00',
                                         'Cierre extraordinario de prueba', 1);
    SELECT estado INTO v_estado FROM reserva WHERE id_reserva = 1;     -- pádel 1, 27/10
    SELECT EXISTS (SELECT 1 FROM bloqueo WHERE id_instalacion = 6 AND motivo = 'CIERRE'
                    AND tsrange(inicio, fin) @> TIMESTAMP '2026-10-26 16:00'
                 ) INTO v_pabellon_cerrado;
    -- 8 instalaciones con el periodo completo + 2 huecos del pabellón (antes y después de la docencia)
    IF v_creados = 10 AND v_estado = 'CANCELADA' AND v_pabellon_cerrado THEN
        RAISE NOTICE 'T15 OK: % cierres creados; reserva cancelada; el pabellón también se cierra fuera de la docencia', v_creados;
    ELSE
        RAISE NOTICE 'T15 FALLO: % cierres, reserva %, pabellón cerrado %', v_creados, v_estado, v_pabellon_cerrado;
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

-- ---------------------------------------------------------------------
-- Variación 7 · Incremento 2 (T17–T23)
-- ---------------------------------------------------------------------

-- T17. P18: aforo de un evento; las inscripciones ANULADAS no cuentan
DO $$
DECLARE v_evento INTEGER;
BEGIN
    INSERT INTO evento (id_organizador, nombre, inicio, fin, aforo_maximo, estado)
    VALUES (1, 'Evento de prueba T17', TIMESTAMP '2027-01-16 09:00', TIMESTAMP '2027-01-16 12:00', 1, 'PUBLICADO')
    RETURNING id_evento INTO v_evento;
    INSERT INTO inscripcion_evento (id_evento, id_usuario) VALUES (v_evento, 2);
    BEGIN
        INSERT INTO inscripcion_evento (id_evento, id_usuario) VALUES (v_evento, 3);
        RAISE NOTICE 'T17 FALLO: se ha superado el aforo';
    EXCEPTION WHEN SQLSTATE 'P0003' THEN
        UPDATE inscripcion_evento SET estado = 'ANULADA' WHERE id_evento = v_evento AND id_usuario = 2;
        INSERT INTO inscripcion_evento (id_evento, id_usuario) VALUES (v_evento, 3);
        RAISE NOTICE 'T17 OK: aforo completo rechazado; la plaza anulada vuelve a estar libre';
    END;
    RAISE EXCEPTION 'rollback';
EXCEPTION WHEN raise_exception THEN NULL;
END $$;

-- T18. P19: la escuela del seed bloquea la calle 2 de la piscina los lunes de 17 a 18
DO $$
BEGIN
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (2, 8, DATE '2026-11-09', TIME '17:00', TIME '18:00');
    RAISE NOTICE 'T18 FALLO: se ha reservado durante una sesión de la escuela';
EXCEPTION WHEN SQLSTATE 'P0001' THEN
    RAISE NOTICE 'T18 OK: % sesiones bloqueadas; reserva rechazada',
        (SELECT count(*) FROM bloqueo WHERE motivo = 'ESCUELA');
END $$;

-- T19. P20: la edad se calcula a la fecha de inscripción (menor de 4 años en un tramo 6-12)
DO $$
DECLARE v_menor INTEGER;
BEGIN
    INSERT INTO menor (id_tutor, nombre, apellidos, fecha_nacimiento, consentimiento_rgpd, fecha_consentimiento,
                       telefono_tutor, contacto_emergencia_nombre, contacto_emergencia_telefono)
    VALUES (6, 'Prueba', 'T19', DATE '2022-05-01', TRUE, CURRENT_TIMESTAMP, '600', 'Contacto', '601')
    RETURNING id_menor INTO v_menor;
    INSERT INTO inscripcion_escuela (id_escuela, id_menor, fecha_inscripcion)
    VALUES (1, v_menor, TIMESTAMP '2026-11-10 10:00');
    RAISE NOTICE 'T19 FALLO: menor fuera de tramo inscrito';
EXCEPTION WHEN SQLSTATE 'P0004' THEN
    RAISE NOTICE 'T19 OK: %', SQLERRM;
END $$;

-- T20. P22: no se registra un menor sin el consentimiento del tutor
DO $$
BEGIN
    INSERT INTO menor (id_tutor, nombre, apellidos, fecha_nacimiento, consentimiento_rgpd, fecha_consentimiento,
                       telefono_tutor, contacto_emergencia_nombre, contacto_emergencia_telefono)
    VALUES (6, 'Prueba', 'T20', DATE '2016-01-01', FALSE, CURRENT_TIMESTAMP, '600', 'Contacto', '601');
    RAISE NOTICE 'T20 FALLO: menor sin consentimiento registrado';
EXCEPTION WHEN check_violation THEN
    RAISE NOTICE 'T20 OK: ck_menor_consentimiento exige el consentimiento';
END $$;

-- T21. P21: un rechazo exige autorizador; la reserva RECHAZADA libera la franja
DO $$
BEGIN
    BEGIN
        UPDATE reserva SET estado = 'RECHAZADA' WHERE id_reserva = 4;
        RAISE NOTICE 'T21 FALLO: rechazo sin autorizador';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    UPDATE reserva SET estado = 'RECHAZADA', id_autorizador = 1, fecha_resolucion = CURRENT_TIMESTAMP,
                       motivo_rechazo = 'Prueba T21'
     WHERE id_reserva = 4;
    INSERT INTO reserva (id_usuario, id_instalacion, fecha, hora_inicio, hora_fin)
    VALUES (3, 5, DATE '2026-10-29', TIME '19:00', TIME '20:00');
    RAISE NOTICE 'T21 OK: rechazo con autorizador; la franja queda libre';
    RAISE EXCEPTION 'rollback';
EXCEPTION WHEN raise_exception THEN NULL;
END $$;

-- T22. P19: una sesión de escuela no se genera sobre un bloqueo existente (festivo, docencia...)
DO $$
DECLARE v_escuela INTEGER; v_creados INTEGER;
BEGIN
    INSERT INTO escuela (id_instalacion, nombre, temporada, fecha_inicio, fecha_fin, edad_min, edad_max, plazas)
    VALUES (3, 'Escuela T22', '2026-2027', DATE '2026-12-07', DATE '2026-12-13', 8, 14, 10)
    RETURNING id_escuela INTO v_escuela;
    INSERT INTO horario_escuela (id_escuela, dia_semana, hora_inicio, hora_fin)
    VALUES (v_escuela, 1, TIME '17:00', TIME '18:00'), (v_escuela, 2, TIME '17:00', TIME '18:00');
    v_creados := fn_generar_bloqueos_escuela(v_escuela, 1);   -- lunes 7/12 sí; martes 8/12 es festivo
    IF v_creados = 1 THEN
        RAISE NOTICE 'T22 OK: 1 sesión creada, la del festivo se omite';
    ELSE
        RAISE NOTICE 'T22 FALLO: % sesiones creadas', v_creados;
    END IF;
    RAISE EXCEPTION 'rollback';
EXCEPTION WHEN raise_exception THEN NULL;
END $$;

-- T23. P18: cancelar un evento libera sus instalaciones (sus bloqueos se borran)
DO $$
DECLARE v_libre BOOLEAN;
BEGIN
    UPDATE evento SET estado = 'CANCELADO' WHERE id_evento = 1;
    UPDATE inscripcion_evento SET estado = 'ANULADA' WHERE id_evento = 1;
    DELETE FROM bloqueo WHERE id_evento = 1;
    SELECT disponible INTO v_libre FROM fn_franjas_disponibles(6, DATE '2026-12-19') WHERE hora_inicio = TIME '10:00';
    IF v_libre THEN
        RAISE NOTICE 'T23 OK: el pabellón vuelve a estar libre el 19/12';
    ELSE
        RAISE NOTICE 'T23 FALLO: el pabellón sigue ocupado';
    END IF;
    RAISE EXCEPTION 'rollback';
EXCEPTION WHEN raise_exception THEN NULL;
END $$;

-- T24. P42: un cierre declarado después de crear la escuela cancela su sesión (lunes 9/11, 17-18)
DO $$
DECLARE v_creados INTEGER; v_sesion INTEGER; v_cierre INTEGER;
BEGIN
    v_creados := fn_cerrar_instalaciones(TIMESTAMP '2026-11-09 00:00', TIMESTAMP '2026-11-10 00:00',
                                         'Cierre de prueba T24', 1);
    SELECT count(*) INTO v_sesion FROM bloqueo
     WHERE motivo = 'ESCUELA' AND inicio = TIMESTAMP '2026-11-09 17:00';
    SELECT count(*) INTO v_cierre FROM bloqueo
     WHERE id_instalacion = 8 AND motivo = 'CIERRE' AND tsrange(inicio, fin) @> TIMESTAMP '2026-11-09 17:30';
    IF v_sesion = 0 AND v_cierre = 1 THEN
        RAISE NOTICE 'T24 OK: la sesión de la escuela se cancela y la calle 2 queda cerrada todo el día';
    ELSE
        RAISE NOTICE 'T24 FALLO: sesiones %, cierre %', v_sesion, v_cierre;
    END IF;
    RAISE EXCEPTION 'rollback';
EXCEPTION WHEN raise_exception THEN NULL;
END $$;

-- T25. P32: no presentarse a una reserva institucional no sanciona al representante
DO $$
DECLARE v_sanciones INTEGER;
BEGIN
    UPDATE reserva SET estado = 'CONFIRMADA', id_autorizador = 1, fecha_resolucion = CURRENT_TIMESTAMP
     WHERE id_reserva = 4;
    UPDATE reserva SET estado = 'NO_PRESENTADO' WHERE id_reserva = 4;
    SELECT count(*) INTO v_sanciones FROM sancion WHERE id_reserva = 4;
    IF v_sanciones = 0 THEN
        RAISE NOTICE 'T25 OK: la ausencia queda registrada para la entidad, sin sancionar al representante';
    ELSE
        RAISE NOTICE 'T25 FALLO: % sanciones', v_sanciones;
    END IF;
    RAISE EXCEPTION 'rollback';
EXCEPTION WHEN raise_exception THEN NULL;
END $$;

-- T8. Consulta de disponibilidad (funcionalidad Must de la V1)
SELECT 'T8 franjas pista de pádel 1, 27/10/2026' AS prueba;
SELECT * FROM fn_franjas_disponibles(1, DATE '2026-10-27');

-- T9. Vista de reservas
SELECT 'T9 v_reservas_detalle' AS prueba;
SELECT id_reserva, fecha, hora_inicio, instalacion, usuario, rol, estado
FROM v_reservas_detalle ORDER BY fecha, hora_inicio;
