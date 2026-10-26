-- =====================================================================
--  SAD-UAL · Sistema de Reservas de Áreas Deportivas de la UAL
--  Script SQL inicial · Esquema de base de datos
-- ---------------------------------------------------------------------
--  MopongoSoft S.L. (PRIS1 · Grupo 07 · Variación 7)
--  SGBD objetivo : PostgreSQL 15 o superior
--  Versión       : 1.1 (Release V1 · Hito 1, 26/10/2026)
--  Reglas        : validadas por el cliente el 25/10/2026 (docs/requisitos-cliente.md)
--  Responsable BD: Javier Zúñiga Carmona
--
--  Ejecución:
--     createdb sadual
--     psql -d sadual -f 01_schema.sql
--     psql -d sadual -f 02_seed.sql
--
--  Organización del script
--     0. Preparación
--     1. Núcleo (Incremento 1 · Release V1)
--     2. Variación 7 (Incremento 2 · tablas ya preparadas)
--     3. Reglas de negocio en BD (triggers)
--     4. Funciones y vistas de apoyo a la Release V1
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. PREPARACIÓN
-- ---------------------------------------------------------------------
-- btree_gist permite combinar "=" y "&&" (solapamiento de rangos) en una
-- misma restricción EXCLUDE: es la garantía física de "cero dobles reservas".
CREATE EXTENSION IF NOT EXISTS btree_gist;

DROP TABLE IF EXISTS inscripcion_escuela, menor, escuela,
                     inscripcion_evento, sancion, bloqueo, evento,
                     reserva, entidad_convenio, horario_instalacion,
                     instalacion, tipo_instalacion, usuario, rol CASCADE;

-- ---------------------------------------------------------------------
-- 1. NÚCLEO · INCREMENTO 1 (RELEASE V1)
-- ---------------------------------------------------------------------

-- Perfiles de acceso (RBAC). max_reservas_semana y antelacion_max_dias quedan
-- como parámetros por perfil para el futuro: con P1/P2 (validadas por el cliente
-- el 25/10/2026) el límite vigente es 1 reserva al día (uq_reserva_usuario_dia)
-- y sin antelación máxima. ADMIN incluye las tareas de Recepción (P15).
CREATE TABLE rol (
    id_rol               SMALLINT    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo               VARCHAR(20) NOT NULL UNIQUE,
    nombre               VARCHAR(60) NOT NULL,
    max_reservas_semana  SMALLINT    NOT NULL DEFAULT 3  CHECK (max_reservas_semana >= 0),
    antelacion_max_dias  SMALLINT    NOT NULL DEFAULT 7  CHECK (antelacion_max_dias  >= 0),
    CONSTRAINT ck_rol_codigo CHECK (codigo IN ('ALUMNADO','PDI','PAS','EXTERNO','ADMIN'))
);

-- Entidades con convenio. Se crea en V1 porque Usuario y Reserva la referencian
-- (representantes y reservas institucionales); su gestión funcional llega en el
-- Incremento 2. Va antes de usuario por la clave foránea usuario.id_entidad.
CREATE TABLE entidad_convenio (
    id_entidad      INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(120) NOT NULL,
    cif             VARCHAR(9)   NOT NULL UNIQUE,
    email_contacto  VARCHAR(120),
    telefono        VARCHAR(20),
    fecha_inicio    DATE         NOT NULL,
    fecha_fin       DATE,
    activa          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_convenio_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

CREATE TABLE usuario (
    id_usuario     INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_rol         SMALLINT     NOT NULL REFERENCES rol (id_rol),
    nombre         VARCHAR(60)  NOT NULL,
    apellidos      VARCHAR(100) NOT NULL,
    email          VARCHAR(120) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,          -- bcrypt; nunca texto plano
    telefono       VARCHAR(20),
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_alta     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- P17: un usuario EXTERNO puede representar a una entidad con convenio
    -- (lo garantiza tg_validar_representante). Los tutores no la necesitan.
    id_entidad     INTEGER      REFERENCES entidad_convenio (id_entidad),
    CONSTRAINT ck_usuario_email CHECK (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$')
);

CREATE TABLE tipo_instalacion (
    id_tipo      SMALLINT    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre       VARCHAR(40) NOT NULL UNIQUE,
    descripcion  VARCHAR(255)
);

-- Cada fila es un espacio reservable de forma independiente
-- (p. ej. "Pista de pádel 1", "Piscina - calle 3").
CREATE TABLE instalacion (
    id_instalacion       INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tipo              SMALLINT     NOT NULL REFERENCES tipo_instalacion (id_tipo),
    nombre               VARCHAR(80)  NOT NULL UNIQUE,
    ubicacion            VARCHAR(120),
    aforo                SMALLINT     NOT NULL CHECK (aforo > 0),
    duracion_franja_min  SMALLINT     NOT NULL DEFAULT 60
                                      CHECK (duracion_franja_min BETWEEN 15 AND 240),
    activa               BOOLEAN      NOT NULL DEFAULT TRUE,
    descripcion          VARCHAR(255)
);

-- Horario semanal de apertura (dia_semana ISO: 1 = lunes ... 7 = domingo).
CREATE TABLE horario_instalacion (
    id_horario      INTEGER  GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_instalacion  INTEGER  NOT NULL REFERENCES instalacion (id_instalacion) ON DELETE CASCADE,
    dia_semana      SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    hora_apertura   TIME     NOT NULL,
    hora_cierre     TIME     NOT NULL,
    CONSTRAINT ck_horario_rango CHECK (hora_cierre > hora_apertura),
    CONSTRAINT uq_horario UNIQUE (id_instalacion, dia_semana, hora_apertura)
);

CREATE TABLE reserva (
    id_reserva         INTEGER     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario         INTEGER     NOT NULL REFERENCES usuario (id_usuario),
    id_instalacion     INTEGER     NOT NULL REFERENCES instalacion (id_instalacion),
    id_entidad         INTEGER     REFERENCES entidad_convenio (id_entidad),  -- solo reservas institucionales
    id_autorizador     INTEGER     REFERENCES usuario (id_usuario),           -- admin que autoriza
    fecha              DATE        NOT NULL,
    hora_inicio        TIME        NOT NULL,
    hora_fin           TIME        NOT NULL,
    estado             VARCHAR(15) NOT NULL DEFAULT 'CONFIRMADA',
    fecha_creacion     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cancelacion  TIMESTAMP,
    CONSTRAINT ck_reserva_horas  CHECK (hora_fin > hora_inicio),
    CONSTRAINT ck_reserva_estado CHECK (estado IN
        ('PENDIENTE','CONFIRMADA','CANCELADA','NO_PRESENTADO','FINALIZADA')),
    CONSTRAINT ck_reserva_cancelacion CHECK
        (estado <> 'CANCELADA' OR fecha_cancelacion IS NOT NULL),
    -- REGLA CRÍTICA (criterio de aceptación G2): cero dobles reservas.
    -- Dos reservas activas de la misma instalación no pueden solaparse en el
    -- tiempo. Lo garantiza el propio SGBD, también ante peticiones concurrentes.
    CONSTRAINT ex_reserva_sin_solape EXCLUDE USING gist (
        id_instalacion WITH =,
        tsrange(fecha + hora_inicio, fecha + hora_fin) WITH &&
    ) WHERE (estado IN ('PENDIENTE','CONFIRMADA'))
);

CREATE INDEX ix_reserva_usuario     ON reserva (id_usuario, fecha);

-- P1: una sola reserva activa por usuario y día. Las reservas institucionales
-- (Variación 7, id_entidad no nulo) quedan fuera de este límite.
CREATE UNIQUE INDEX uq_reserva_usuario_dia ON reserva (id_usuario, fecha)
    WHERE estado IN ('PENDIENTE','CONFIRMADA') AND id_entidad IS NULL;
CREATE INDEX ix_reserva_instalacion ON reserva (id_instalacion, fecha);

-- ---------------------------------------------------------------------
-- 2. VARIACIÓN 7 · INCREMENTO 2 (estructura preparada desde V1)
-- ---------------------------------------------------------------------

CREATE TABLE evento (
    id_evento       INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_organizador  INTEGER      NOT NULL REFERENCES usuario (id_usuario),
    nombre          VARCHAR(120) NOT NULL,
    descripcion     TEXT,
    inicio          TIMESTAMP    NOT NULL,
    fin             TIMESTAMP    NOT NULL,
    aforo_maximo    INTEGER      NOT NULL CHECK (aforo_maximo > 0),
    estado          VARCHAR(12)  NOT NULL DEFAULT 'PLANIFICADO',
    CONSTRAINT ck_evento_fechas CHECK (fin > inicio),
    CONSTRAINT ck_evento_estado CHECK (estado IN ('PLANIFICADO','PUBLICADO','CANCELADO','FINALIZADO'))
);

-- Bloqueo de una instalación (docencia, mantenimiento, evento, cierre...).
-- P7: solo lo crea la administración y tiene prioridad sobre las reservas.
-- Se usa ya en V1 para que la consulta de disponibilidad sea real.
CREATE TABLE bloqueo (
    id_bloqueo      INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_instalacion  INTEGER      NOT NULL REFERENCES instalacion (id_instalacion) ON DELETE CASCADE,
    id_creador      INTEGER      NOT NULL REFERENCES usuario (id_usuario),
    id_evento       INTEGER      REFERENCES evento (id_evento) ON DELETE CASCADE,
    inicio          TIMESTAMP    NOT NULL,
    fin             TIMESTAMP    NOT NULL,
    motivo          VARCHAR(15)  NOT NULL,
    observaciones   VARCHAR(255),
    CONSTRAINT ck_bloqueo_fechas CHECK (fin > inicio),
    -- CIERRE (P11): festivos, periodos vacacionales y cierres extraordinarios.
    CONSTRAINT ck_bloqueo_motivo CHECK (motivo IN
        ('DOCENCIA','MANTENIMIENTO','EVENTO','COMPETICION','CIERRE','OTRO')),
    CONSTRAINT ck_bloqueo_evento CHECK ((motivo = 'EVENTO') = (id_evento IS NOT NULL)),
    CONSTRAINT ex_bloqueo_sin_solape EXCLUDE USING gist (
        id_instalacion WITH =, tsrange(inicio, fin) WITH &&)
);

CREATE TABLE inscripcion_evento (
    id_evento          INTEGER   NOT NULL REFERENCES evento (id_evento) ON DELETE CASCADE,
    id_usuario         INTEGER   NOT NULL REFERENCES usuario (id_usuario),
    fecha_inscripcion  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_evento, id_usuario)
);

CREATE TABLE escuela (
    id_escuela      INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_instalacion  INTEGER      NOT NULL REFERENCES instalacion (id_instalacion),
    nombre          VARCHAR(120) NOT NULL,
    temporada       VARCHAR(9)   NOT NULL CHECK (temporada ~ '^\d{4}-\d{4}$'),
    edad_min        SMALLINT     NOT NULL CHECK (edad_min >= 3),
    edad_max        SMALLINT     NOT NULL CHECK (edad_max <= 17),
    plazas          SMALLINT     NOT NULL CHECK (plazas > 0),
    dias_horario    VARCHAR(100),
    CONSTRAINT ck_escuela_edades CHECK (edad_max >= edad_min),
    CONSTRAINT uq_escuela UNIQUE (nombre, temporada)
);

-- Menor inscrito en una escuela; siempre vinculado a un tutor responsable.
CREATE TABLE menor (
    id_menor          INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tutor          INTEGER      NOT NULL REFERENCES usuario (id_usuario),
    nombre            VARCHAR(60)  NOT NULL,
    apellidos         VARCHAR(100) NOT NULL,
    fecha_nacimiento  DATE         NOT NULL
);

CREATE TABLE inscripcion_escuela (
    id_escuela         INTEGER     NOT NULL REFERENCES escuela (id_escuela) ON DELETE CASCADE,
    id_menor           INTEGER     NOT NULL REFERENCES menor (id_menor) ON DELETE CASCADE,
    fecha_inscripcion  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado             VARCHAR(10) NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA','BAJA')),
    PRIMARY KEY (id_escuela, id_menor)
);

-- P3 (validada 25/10/2026): quien no se presenta a una reserva confirmada no puede
-- hacer reservas nuevas el resto de esa semana. La sanción la crea el trigger tg_sancionar_no_presentado.
CREATE TABLE sancion (
    id_sancion    INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario    INTEGER      NOT NULL REFERENCES usuario (id_usuario),
    id_reserva    INTEGER      UNIQUE REFERENCES reserva (id_reserva),  -- MER "origina" 1:1
    motivo        VARCHAR(255) NOT NULL,
    fecha_inicio  DATE         NOT NULL,
    fecha_fin     DATE         NOT NULL,
    CONSTRAINT ck_sancion_fechas CHECK (fecha_fin >= fecha_inicio)
);

-- ---------------------------------------------------------------------
-- 3. REGLAS DE NEGOCIO EN BASE DE DATOS (TRIGGERS)
-- ---------------------------------------------------------------------

-- R-01. No se puede reservar una franja bloqueada ni fuera del horario
--       de apertura de la instalación.
CREATE OR REPLACE FUNCTION fn_validar_reserva() RETURNS trigger AS $$
BEGIN
    -- Reservas no activas o franjas incoherentes: decide la restricción CHECK.
    IF NEW.estado NOT IN ('PENDIENTE','CONFIRMADA') OR NEW.hora_fin <= NEW.hora_inicio THEN
        RETURN NEW;
    END IF;

    IF EXISTS (SELECT 1 FROM bloqueo b
               WHERE b.id_instalacion = NEW.id_instalacion
                 AND tsrange(b.inicio, b.fin) &&
                     tsrange(NEW.fecha + NEW.hora_inicio, NEW.fecha + NEW.hora_fin)) THEN
        RAISE EXCEPTION 'La instalación % está bloqueada en esa franja', NEW.id_instalacion
              USING ERRCODE = 'P0001';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM horario_instalacion h
                   WHERE h.id_instalacion = NEW.id_instalacion
                     AND h.dia_semana = EXTRACT(ISODOW FROM NEW.fecha)
                     AND NEW.hora_inicio >= h.hora_apertura
                     AND NEW.hora_fin    <= h.hora_cierre) THEN
        RAISE EXCEPTION 'La franja solicitada está fuera del horario de apertura'
              USING ERRCODE = 'P0002';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_validar_reserva
    BEFORE INSERT OR UPDATE OF fecha, hora_inicio, hora_fin, estado, id_instalacion
    ON reserva FOR EACH ROW EXECUTE FUNCTION fn_validar_reserva();

-- R-04 (P3). Marcar una reserva como NO_PRESENTADO genera la sanción: desde el día
--       de la reserva hasta el domingo de esa semana. Una reserva → una sanción.
CREATE OR REPLACE FUNCTION fn_sancionar_no_presentado() RETURNS trigger AS $$
BEGIN
    IF NEW.estado = 'NO_PRESENTADO' AND OLD.estado IS DISTINCT FROM 'NO_PRESENTADO' THEN
        INSERT INTO sancion (id_usuario, id_reserva, motivo, fecha_inicio, fecha_fin)
        VALUES (NEW.id_usuario, NEW.id_reserva,
                'No presentado a la reserva ' || NEW.id_reserva,
                NEW.fecha,
                (date_trunc('week', NEW.fecha::TIMESTAMP) + INTERVAL '6 days')::DATE)
        ON CONFLICT (id_reserva) DO NOTHING;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_sancionar_no_presentado
    AFTER UPDATE OF estado ON reserva
    FOR EACH ROW EXECUTE FUNCTION fn_sancionar_no_presentado();

-- R-05 (P7). Un bloqueo del administrador tiene prioridad: cancela las reservas
--       activas que se solapan con él.
CREATE OR REPLACE FUNCTION fn_bloqueo_cancela_reservas() RETURNS trigger AS $$
BEGIN
    UPDATE reserva
       SET estado = 'CANCELADA', fecha_cancelacion = CURRENT_TIMESTAMP
     WHERE id_instalacion = NEW.id_instalacion
       AND estado IN ('PENDIENTE','CONFIRMADA')
       AND tsrange(fecha + hora_inicio, fecha + hora_fin) && tsrange(NEW.inicio, NEW.fin);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_bloqueo_cancela_reservas
    AFTER INSERT OR UPDATE OF inicio, fin, id_instalacion ON bloqueo
    FOR EACH ROW EXECUTE FUNCTION fn_bloqueo_cancela_reservas();

-- R-06 (P17). Solo un usuario EXTERNO puede estar vinculado a una entidad con
--       convenio (representante). Tutores y comunidad UAL no tienen entidad.
CREATE OR REPLACE FUNCTION fn_validar_representante() RETURNS trigger AS $$
BEGIN
    IF NEW.id_entidad IS NOT NULL AND
       (SELECT codigo FROM rol WHERE id_rol = NEW.id_rol) <> 'EXTERNO' THEN
        RAISE EXCEPTION 'Solo un usuario externo puede representar a una entidad con convenio'
              USING ERRCODE = 'P0006';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_validar_representante
    BEFORE INSERT OR UPDATE OF id_entidad, id_rol ON usuario
    FOR EACH ROW EXECUTE FUNCTION fn_validar_representante();

-- R-02. Control de aforo de eventos masivos (Variación 7).
CREATE OR REPLACE FUNCTION fn_aforo_evento() RETURNS trigger AS $$
BEGIN
    PERFORM 1 FROM evento WHERE id_evento = NEW.id_evento FOR UPDATE;  -- serializa inscripciones
    IF (SELECT COUNT(*) FROM inscripcion_evento WHERE id_evento = NEW.id_evento)
       >= (SELECT aforo_maximo FROM evento WHERE id_evento = NEW.id_evento) THEN
        RAISE EXCEPTION 'Aforo completo para el evento %', NEW.id_evento USING ERRCODE = 'P0003';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_aforo_evento
    BEFORE INSERT ON inscripcion_evento
    FOR EACH ROW EXECUTE FUNCTION fn_aforo_evento();

-- R-03. Escuelas: la edad del menor debe estar en el tramo y no se
--       pueden superar las plazas (Variación 7).
CREATE OR REPLACE FUNCTION fn_validar_inscripcion_escuela() RETURNS trigger AS $$
DECLARE
    v_edad   INTEGER;
    v_esc    escuela%ROWTYPE;
BEGIN
    SELECT * INTO v_esc FROM escuela WHERE id_escuela = NEW.id_escuela FOR UPDATE;
    SELECT EXTRACT(YEAR FROM age(CURRENT_DATE, fecha_nacimiento))::INT INTO v_edad
      FROM menor WHERE id_menor = NEW.id_menor;

    IF v_edad NOT BETWEEN v_esc.edad_min AND v_esc.edad_max THEN
        RAISE EXCEPTION 'Edad (%) fuera del tramo de la escuela (%-%)',
              v_edad, v_esc.edad_min, v_esc.edad_max USING ERRCODE = 'P0004';
    END IF;

    IF NEW.estado = 'ACTIVA' AND
       (SELECT COUNT(*) FROM inscripcion_escuela
         WHERE id_escuela = NEW.id_escuela AND estado = 'ACTIVA') >= v_esc.plazas THEN
        RAISE EXCEPTION 'No quedan plazas en la escuela %', v_esc.nombre USING ERRCODE = 'P0005';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_validar_inscripcion_escuela
    BEFORE INSERT ON inscripcion_escuela
    FOR EACH ROW EXECUTE FUNCTION fn_validar_inscripcion_escuela();

-- ---------------------------------------------------------------------
-- 4. FUNCIONES Y VISTAS DE APOYO A LA RELEASE V1
-- ---------------------------------------------------------------------

-- Franjas de una instalación en una fecha y si están libres.
-- Uso: SELECT * FROM fn_franjas_disponibles(1, DATE '2026-10-27');
CREATE OR REPLACE FUNCTION fn_franjas_disponibles(p_instalacion INTEGER, p_fecha DATE)
RETURNS TABLE (hora_inicio TIME, hora_fin TIME, disponible BOOLEAN) AS $$
    SELECT f.ini::TIME,
           (f.ini + make_interval(mins => i.duracion_franja_min))::TIME,
           NOT EXISTS (SELECT 1 FROM reserva r
                        WHERE r.id_instalacion = i.id_instalacion
                          AND r.estado IN ('PENDIENTE','CONFIRMADA')
                          AND tsrange(r.fecha + r.hora_inicio, r.fecha + r.hora_fin)
                              && tsrange(f.ini, f.ini + make_interval(mins => i.duracion_franja_min)))
       AND NOT EXISTS (SELECT 1 FROM bloqueo b
                        WHERE b.id_instalacion = i.id_instalacion
                          AND tsrange(b.inicio, b.fin)
                              && tsrange(f.ini, f.ini + make_interval(mins => i.duracion_franja_min)))
    FROM instalacion i
    JOIN horario_instalacion h
      ON h.id_instalacion = i.id_instalacion
     AND h.dia_semana = EXTRACT(ISODOW FROM p_fecha)
    CROSS JOIN LATERAL generate_series(
          p_fecha + h.hora_apertura,
          p_fecha + h.hora_cierre - make_interval(mins => i.duracion_franja_min),
          make_interval(mins => i.duracion_franja_min)) AS f(ini)
    WHERE i.id_instalacion = p_instalacion
      AND i.activa
    ORDER BY 1;
$$ LANGUAGE sql STABLE;

-- P11: cierre de TODAS las instalaciones activas (festivo, vacaciones, cierre
-- extraordinario). Crea un bloqueo CIERRE por instalación; las reservas afectadas
-- se cancelan (tg_bloqueo_cancela_reservas, P7). Las instalaciones que ya tienen un
-- bloqueo solapado en ese periodo se omiten (siguen bloqueadas por el existente).
-- Uso: SELECT fn_cerrar_instalaciones(TIMESTAMP '2026-12-08 00:00', TIMESTAMP '2026-12-09 00:00',
--                                      'Festivo: Inmaculada Concepción', 1);
CREATE OR REPLACE FUNCTION fn_cerrar_instalaciones(p_inicio TIMESTAMP, p_fin TIMESTAMP,
                                                   p_observaciones VARCHAR, p_id_creador INTEGER)
RETURNS INTEGER AS $$
DECLARE v_creados INTEGER;
BEGIN
    INSERT INTO bloqueo (id_instalacion, id_creador, inicio, fin, motivo, observaciones)
    SELECT i.id_instalacion, p_id_creador, p_inicio, p_fin, 'CIERRE', p_observaciones
      FROM instalacion i
     WHERE i.activa
       AND NOT EXISTS (SELECT 1 FROM bloqueo b
                        WHERE b.id_instalacion = i.id_instalacion
                          AND tsrange(b.inicio, b.fin) && tsrange(p_inicio, p_fin));
    GET DIAGNOSTICS v_creados = ROW_COUNT;
    RETURN v_creados;
END;
$$ LANGUAGE plpgsql;

-- Listado de reservas con datos legibles (pantalla "Mis reservas" y panel admin).
CREATE OR REPLACE VIEW v_reservas_detalle AS
SELECT r.id_reserva,
       r.fecha,
       r.hora_inicio,
       r.hora_fin,
       r.estado,
       i.id_instalacion,
       i.nombre                          AS instalacion,
       t.nombre                          AS tipo,
       u.id_usuario,
       u.nombre || ' ' || u.apellidos    AS usuario,
       ro.codigo                         AS rol,
       e.nombre                          AS entidad
FROM reserva r
JOIN instalacion      i  ON i.id_instalacion = r.id_instalacion
JOIN tipo_instalacion t  ON t.id_tipo        = i.id_tipo
JOIN usuario          u  ON u.id_usuario     = r.id_usuario
JOIN rol              ro ON ro.id_rol        = u.id_rol
LEFT JOIN entidad_convenio e ON e.id_entidad = r.id_entidad;

-- Fin del esquema
