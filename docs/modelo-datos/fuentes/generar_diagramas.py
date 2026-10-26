"""
Genera el MER y el modelo relacional de SAD-UAL (Grupo 07) con Graphviz.
Uso:  python3 generar_diagramas.py   (requiere `dot` y `neato` de Graphviz)
Salida: MER.dot, ModeloRelacional.dot y sus .png/.pdf en la carpeta superior.
Fuente de verdad: database/01_schema.sql. Si cambia el esquema, actualizar aquí.
"""
import subprocess, pathlib, html

AQUI = pathlib.Path(__file__).parent
SALIDA = AQUI.parent

V1 = dict(cab='#1F5FA8', cuerpo='#E8F0FA', borde='#1F5FA8')   # Release V1
V7 = dict(cab='#1A8A87', cuerpo='#E3F4F2', borde='#1A8A87')   # Incremento 2 / Variación 7
NUEVO = '#D9822B'                                              # cambio tras la validación del cliente (25/10/2026)
FUENTE = 'Helvetica'

# ------------------------------------------------------------------ MER
# Entidad: (nombre, color, [atributos], pos(x,y) en px de arriba-izquierda, débil)
ENTIDADES = {
    'EVENTO': (V7, ['id_evento', 'nombre', 'descripcion', 'inicio', 'fin', 'aforo_maximo', 'estado'], (690, 150), False),
    'BLOQUEO': (V1, ['id_bloqueo', 'inicio', 'fin', 'motivo', 'observaciones'], (1940, 150), False),
    'ROL': (V1, ['id_rol', 'codigo', 'nombre', 'max_reservas_semana', 'antelacion_max_dias'], (110, 700), False),
    'USUARIO': (V1, ['id_usuario', 'nombre', 'apellidos', 'email', 'password_hash', 'telefono', 'activo', 'fecha_alta'], (690, 700), False),
    'MENOR': (V7, ['id_menor', 'nombre', 'apellidos', 'fecha_nacimiento'], (1030, 760), False),
    'ESCUELA': (V7, ['id_escuela', 'nombre', 'temporada', 'edad_min', 'edad_max', 'plazas', 'dias_horario'], (1430, 760), False),
    'SANCION': (V7, ['id_sancion', 'motivo', 'fecha_inicio', 'fecha_fin'], (110, 1000), False),
    'ENTIDAD_CONVENIO': (V7, ['id_entidad', 'nombre', 'cif', 'email_contacto', 'telefono', 'fecha_inicio', 'fecha_fin', 'activa'], (110, 1400), False),
    'RESERVA': (V1, ['id_reserva', 'fecha', 'hora_inicio', 'hora_fin', 'estado', 'fecha_creacion', 'fecha_cancelacion'], (690, 1400), False),
    'INSTALACION': (V1, ['id_instalacion', 'nombre', 'ubicacion', 'aforo', 'duracion_franja_min', 'activa', 'descripcion'], (1430, 1400), False),
    'TIPO_INSTALACION': (V1, ['id_tipo', 'nombre', 'descripcion'], (1960, 1400), False),
    'HORARIO_INSTALACION': (V1, ['id_horario', 'dia_semana', 'hora_apertura', 'hora_cierre'], (1430, 1760), True),
}
# Relación: (id, nombre, tipo, (entidadA, cardA), (entidadB, cardB), pos, atributos, color, débil)
# cardX = participación anotada junto a la entidad X (misma notación que la entrega G4).
RELACIONES = [
    ('genera', 'genera', '1:N', ('EVENTO', '(0,1)'), ('BLOQUEO', '(0,n)'), (1315, 150), [], V7, False),
    ('organiza', 'organiza', '1:N', ('EVENTO', '(0,n)'), ('USUARIO', '(1,1)'), (600, 420), [], V7, False),
    ('inscribe', 'se inscribe', 'N:M', ('EVENTO', '(0,n)'), ('USUARIO', '(0,n)'), (790, 420), ['fecha_inscripcion'], V7, False),
    ('crea', 'crea', '1:N', ('BLOQUEO', '(0,n)'), ('USUARIO', '(1,1)'), (1315, 420), [], V1, False),
    ('pertenece', 'pertenece', 'N:1', ('ROL', '(1,1)'), ('USUARIO', '(0,n)'), (400, 700), [], V1, False),
    ('tutela', 'tutela', '1:N', ('USUARIO', '(1,1)'), ('MENOR', '(0,n)'), (860, 730), [], V7, False),
    ('matricula', 'se matricula', 'N:M', ('MENOR', '(0,n)'), ('ESCUELA', '(0,n)'), (1230, 760), ['fecha_inscripcion', 'estado'], V7, False),
    ('bloquea', 'bloquea', '1:N', ('BLOQUEO', '(0,n)'), ('INSTALACION', '(1,1)'), (1830, 890), [], V1, False),
    ('recibe', 'recibe', '1:N', ('USUARIO', '(1,1)'), ('SANCION', '(0,n)'), (400, 860), [], V7, False),
    ('realiza', 'realiza', '1:N', ('USUARIO', '(1,1)'), ('RESERVA', '(0,n)'), (600, 1050), [], V1, False),
    ('autoriza', 'autoriza', '1:N', ('USUARIO', '(0,1)'), ('RESERVA', '(0,n)'), (790, 1050), [], V7, False),
    ('imparte', 'se imparte en', 'N:1', ('ESCUELA', '(0,n)'), ('INSTALACION', '(1,1)'), (1430, 1080), [], V7, False),
    ('origina', 'origina', '1:1', ('SANCION', '(0,1)'), ('RESERVA', '(0,1)'), (400, 1170), [], V7, False),
    ('representa', 'representa', 'N:1', ('USUARIO', '(0,n)'), ('ENTIDAD_CONVENIO', '(0,1)'), (110, 1215), [], 'NUEVO', False),
    ('nombre_de', 'en nombre de', '1:N', ('ENTIDAD_CONVENIO', '(0,1)'), ('RESERVA', '(0,n)'), (400, 1400), [], V7, False),
    ('se_reserva', 'se reserva', 'N:1', ('RESERVA', '(0,n)'), ('INSTALACION', '(1,1)'), (1060, 1400), [], V1, False),
    ('es_tipo', 'es de tipo', 'N:1', ('INSTALACION', '(0,n)'), ('TIPO_INSTALACION', '(1,1)'), (1720, 1400), [], V1, False),
    ('abre', 'abre en', '1:N', ('INSTALACION', '(1,1)'), ('HORARIO_INSTALACION', '(0,n)'), (1430, 1590), [], V1, True),
]
ALTO = 1900


def pos(x, y):
    return f'"{x},{ALTO - y}!"'  # puntos; neato -n respeta estas posiciones


def entidad_html(nombre, color, attrs, debil):
    filas = ''.join(
        f'<tr><td align="left" balign="left"><font point-size="11">'
        + (f'<b><u>{a}</u></b>' if i == 0 else a) + '</font></td></tr>' for i, a in enumerate(attrs))
    borde = 3 if debil else 1
    return (f'<<table border="{borde}" cellborder="0" cellspacing="0" cellpadding="3" style="rounded" '
            f'color="{color["borde"]}" bgcolor="{color["cuerpo"]}">'
            f'<tr><td bgcolor="{color["cab"]}"><font color="white" point-size="12"><b>{nombre}</b></font></td></tr>'
            f'{filas}</table>>')


def mer():
    l = ['graph MER {', f'  graph [splines=line, outputorder=edgesfirst, pad=0.3, fontname="{FUENTE}", '
         'label=<<font point-size="22" color="#1F3D6B">SAD-UAL · Modelo Entidad-Relación · MopongoSoft (Grupo 07)'
         '</font><br/><font point-size="14" color="#555555">azul: Release V1 · verde: Incremento 2 / Variación 7 · '
         'naranja: añadido tras la validación del cliente (25/10/2026) · borde doble: entidad débil</font>>, '
         'labelloc=t];',
         f'  node [fontname="{FUENTE}"]; edge [color="#5B6472", fontname="{FUENTE}", fontsize=11, fontcolor="#1F5FA8"];']
    for n, (color, attrs, (x, y), debil) in ENTIDADES.items():
        l.append(f'  {n} [shape=plain, pos={pos(x, y)}, label={entidad_html(n, color, attrs, debil)}];')
    for rid, nombre, tipo, (a, ca), (b, cb), (x, y), attrs, color, debil in RELACIONES:
        borde = NUEVO if color == 'NUEVO' else '#1F3D6B'
        ancho = 2.2 if color == 'NUEVO' else 1.2
        forma = 'diamond'
        per = 2 if debil else 1
        etiqueta = (f'<<b>{html.escape(nombre)}</b><br/><font point-size="10" color="#666666">{tipo}</font>'
                    + ('<br/><font point-size="9" color="#D9822B">nuevo</font>' if color == 'NUEVO' else '') + '>')
        l.append(f'  R_{rid} [shape={forma}, peripheries={per}, style=filled, fillcolor=white, color="{borde}", '
                 f'penwidth={ancho}, width=1.9, height=0.95, fixedsize=true, fontsize=12, pos={pos(x, y)}, label={etiqueta}];')
        estilo = f' color="{NUEVO}", penwidth=2' if color == 'NUEVO' else ''
        l.append(f'  R_{rid} -- {a} [headlabel="{ca}", labeldistance=2.2, labelangle=25{"," if estilo else ""}{estilo}];')
        l.append(f'  R_{rid} -- {b} [headlabel="{cb}", labeldistance=2.2, labelangle=25{"," if estilo else ""}{estilo}];')
        for i, at in enumerate(attrs):
            ax, ay = x - 60 + 160 * i if len(attrs) > 1 else x + 180, y + (115 if len(attrs) > 1 else 0) + 55 * i
            l.append(f'  A_{rid}_{i} [shape=ellipse, fontsize=11, width=1.7, height=0.5, fixedsize=true, '
                     f'color="#5B6472", pos={pos(ax, ay)}, label="{at}"];')
            l.append(f'  R_{rid} -- A_{rid}_{i} [style=dashed];')
    l.append('}')
    return '\n'.join(l)


# ------------------------------------------------------------------ Modelo relacional
# columna: (nombre, tipo, rol) rol ∈ PK, FK, PKFK, UQ, '' ; '?' en el tipo = admite nulo
TABLAS = {
    'rol': (V1, [('id_rol', 'SMALLINT', 'PK'), ('codigo', 'VARCHAR(20)', 'UQ'), ('nombre', 'VARCHAR(60)', ''),
                 ('max_reservas_semana', 'SMALLINT', ''), ('antelacion_max_dias', 'SMALLINT', '')]),
    'usuario': (V1, [('id_usuario', 'INTEGER', 'PK'), ('id_rol', 'SMALLINT', 'FK'), ('id_entidad', 'INTEGER ?', 'FKN'),
                     ('nombre', 'VARCHAR(60)', ''), ('apellidos', 'VARCHAR(100)', ''), ('email', 'VARCHAR(120)', 'UQ'),
                     ('password_hash', 'VARCHAR(255)', ''), ('telefono', 'VARCHAR(20) ?', ''), ('activo', 'BOOLEAN', ''),
                     ('fecha_alta', 'TIMESTAMP', '')]),
    'entidad_convenio': (V7, [('id_entidad', 'INTEGER', 'PK'), ('nombre', 'VARCHAR(120)', ''), ('cif', 'VARCHAR(9)', 'UQ'),
                              ('email_contacto', 'VARCHAR(120) ?', ''), ('telefono', 'VARCHAR(20) ?', ''),
                              ('fecha_inicio', 'DATE', ''), ('fecha_fin', 'DATE ?', ''), ('activa', 'BOOLEAN', '')]),
    'tipo_instalacion': (V1, [('id_tipo', 'SMALLINT', 'PK'), ('nombre', 'VARCHAR(40)', 'UQ'), ('descripcion', 'VARCHAR(255) ?', '')]),
    'instalacion': (V1, [('id_instalacion', 'INTEGER', 'PK'), ('id_tipo', 'SMALLINT', 'FK'), ('nombre', 'VARCHAR(80)', 'UQ'),
                         ('ubicacion', 'VARCHAR(120) ?', ''), ('aforo', 'SMALLINT', ''), ('duracion_franja_min', 'SMALLINT', ''),
                         ('activa', 'BOOLEAN', ''), ('descripcion', 'VARCHAR(255) ?', '')]),
    'horario_instalacion': (V1, [('id_horario', 'INTEGER', 'PK'), ('id_instalacion', 'INTEGER', 'FK'), ('dia_semana', 'SMALLINT', ''),
                                 ('hora_apertura', 'TIME', ''), ('hora_cierre', 'TIME', '')]),
    'reserva': (V1, [('id_reserva', 'INTEGER', 'PK'), ('id_usuario', 'INTEGER', 'FK'), ('id_instalacion', 'INTEGER', 'FK'),
                     ('id_entidad', 'INTEGER ?', 'FKN'), ('id_autorizador', 'INTEGER ?', 'FKN'), ('fecha', 'DATE', ''),
                     ('hora_inicio', 'TIME', ''), ('hora_fin', 'TIME', ''), ('estado', 'VARCHAR(15)', ''),
                     ('fecha_creacion', 'TIMESTAMP', ''), ('fecha_cancelacion', 'TIMESTAMP ?', '')]),
    'bloqueo': (V1, [('id_bloqueo', 'INTEGER', 'PK'), ('id_instalacion', 'INTEGER', 'FK'), ('id_creador', 'INTEGER', 'FK'),
                     ('id_evento', 'INTEGER ?', 'FKN'), ('inicio', 'TIMESTAMP', ''), ('fin', 'TIMESTAMP', ''),
                     ('motivo', 'VARCHAR(15)', ''), ('observaciones', 'VARCHAR(255) ?', '')]),
    'evento': (V7, [('id_evento', 'INTEGER', 'PK'), ('id_organizador', 'INTEGER', 'FK'), ('nombre', 'VARCHAR(120)', ''),
                    ('descripcion', 'TEXT ?', ''), ('inicio', 'TIMESTAMP', ''), ('fin', 'TIMESTAMP', ''),
                    ('aforo_maximo', 'INTEGER', ''), ('estado', 'VARCHAR(12)', '')]),
    'inscripcion_evento': (V7, [('id_evento', 'INTEGER', 'PKFK'), ('id_usuario', 'INTEGER', 'PKFK'), ('fecha_inscripcion', 'TIMESTAMP', '')]),
    'escuela': (V7, [('id_escuela', 'INTEGER', 'PK'), ('id_instalacion', 'INTEGER', 'FK'), ('nombre', 'VARCHAR(120)', ''),
                     ('temporada', 'VARCHAR(9)', ''), ('edad_min', 'SMALLINT', ''), ('edad_max', 'SMALLINT', ''),
                     ('plazas', 'SMALLINT', ''), ('dias_horario', 'VARCHAR(100) ?', '')]),
    'menor': (V7, [('id_menor', 'INTEGER', 'PK'), ('id_tutor', 'INTEGER', 'FK'), ('nombre', 'VARCHAR(60)', ''),
                   ('apellidos', 'VARCHAR(100)', ''), ('fecha_nacimiento', 'DATE', '')]),
    'inscripcion_escuela': (V7, [('id_escuela', 'INTEGER', 'PKFK'), ('id_menor', 'INTEGER', 'PKFK'),
                                 ('fecha_inscripcion', 'TIMESTAMP', ''), ('estado', 'VARCHAR(10)', '')]),
    'sancion': (V7, [('id_sancion', 'INTEGER', 'PK'), ('id_usuario', 'INTEGER', 'FK'), ('id_reserva', 'INTEGER ?', 'FKNUQ'),
                     ('motivo', 'VARCHAR(255)', ''), ('fecha_inicio', 'DATE', ''), ('fecha_fin', 'DATE', '')]),
}
# (tabla_fk, columna_fk, tabla_pk, opcional, nuevo)
FKS = [
    ('usuario', 'id_rol', 'rol', False, False),
    ('usuario', 'id_entidad', 'entidad_convenio', True, True),
    ('instalacion', 'id_tipo', 'tipo_instalacion', False, False),
    ('horario_instalacion', 'id_instalacion', 'instalacion', False, False),
    ('reserva', 'id_usuario', 'usuario', False, False),
    ('reserva', 'id_instalacion', 'instalacion', False, False),
    ('reserva', 'id_entidad', 'entidad_convenio', True, False),
    ('reserva', 'id_autorizador', 'usuario', True, False),
    ('bloqueo', 'id_instalacion', 'instalacion', False, False),
    ('bloqueo', 'id_creador', 'usuario', False, False),
    ('bloqueo', 'id_evento', 'evento', True, False),
    ('evento', 'id_organizador', 'usuario', False, False),
    ('inscripcion_evento', 'id_evento', 'evento', False, False),
    ('inscripcion_evento', 'id_usuario', 'usuario', False, False),
    ('escuela', 'id_instalacion', 'instalacion', False, False),
    ('menor', 'id_tutor', 'usuario', False, False),
    ('inscripcion_escuela', 'id_escuela', 'escuela', False, False),
    ('inscripcion_escuela', 'id_menor', 'menor', False, False),
    ('sancion', 'id_usuario', 'usuario', False, False),
    ('sancion', 'id_reserva', 'reserva', True, False),
]
RANGOS = [['rol', 'entidad_convenio', 'tipo_instalacion'], ['usuario', 'instalacion'],
          ['evento', 'menor', 'reserva', 'escuela', 'horario_instalacion'],
          ['inscripcion_evento', 'bloqueo', 'sancion', 'inscripcion_escuela']]


def tabla_html(nombre, color, cols):
    filas = []
    nuevas = {c for t, c, _, _, nuevo in FKS if t == nombre and nuevo}
    for col, tipo, rol in cols:
        etiqueta_rol = {'PK': 'PK', 'FK': 'FK', 'FKN': 'FK', 'PKFK': 'PK FK', 'UQ': 'UQ', 'FKNUQ': 'FK UQ'}.get(rol, '')
        nombre_col = col
        if 'PK' in rol:
            nombre_col = f'<b><u>{col}</u></b>'
        elif 'FK' in rol:
            nombre_col = f'<i>{col}</i>'
        tinta = NUEVO if col in nuevas else '#000000'
        filas.append(
            f'<tr><td align="left" port="{col}_w"><font color="#1F5FA8" point-size="11">{etiqueta_rol or " "}</font></td>'
            f'<td align="left"><font color="{tinta}" point-size="12">{nombre_col}</font></td>'
            f'<td align="left" port="{col}_e"><font color="#666666" point-size="11">{tipo}</font></td></tr>')
    return (f'<<table border="1" cellborder="0" cellspacing="0" cellpadding="4" color="{color["borde"]}" bgcolor="{color["cuerpo"]}">'
            f'<tr><td colspan="3" bgcolor="{color["cab"]}"><font color="white" point-size="13"><b>{nombre}</b></font></td></tr>'
            + ''.join(filas) + '</table>>')


def relacional():
    l = ['digraph ModeloRelacional {',
         f'  graph [rankdir=LR, splines=spline, nodesep=0.45, ranksep=1.3, pad=0.3, fontname="{FUENTE}", labelloc=t, '
         'label=<<font point-size="22" color="#1F3D6B">SAD-UAL · Modelo Relacional (PostgreSQL) · MopongoSoft (Grupo 07)</font>'
         '<br/><font point-size="14" color="#555555">PK subrayada · FK en cursiva · ? admite nulo · discontinua: FK opcional · '
         'naranja: añadido tras la validación del cliente (25/10/2026)</font>>];',
         f'  node [shape=plain, fontname="{FUENTE}"]; edge [color="#5B6472", arrowsize=0.9];']
    for n, (color, cols) in TABLAS.items():
        l.append(f'  {n} [label={tabla_html(n, color, cols)}];')
    for r in RANGOS:
        l.append('  { rank=same; ' + '; '.join(r) + '; }')
    for t, c, pk, opcional, nuevo in FKS:
        pk_col = TABLAS[pk][1][0][0]
        estilo = ['dir=both', 'arrowhead=crow', 'arrowtail=' + ('teeodot' if opcional else 'teetee')]
        if opcional:
            estilo.append('style=dashed')
        if nuevo:
            estilo += [f'color="{NUEVO}"', 'penwidth=2']
        l.append(f'  {pk}:{pk_col}_e -> {t}:{c}_w [{", ".join(estilo)}];')
    l.append('}')
    return '\n'.join(l)


def render(nombre, fuente, motor):
    (AQUI / f'{nombre}.dot').write_text(fuente, encoding='utf-8')
    for fmt in ('png', 'pdf'):
        extra = ['-Gdpi=110'] if fmt == 'png' else []
        subprocess.run([motor, '-n' if motor == 'neato' else '-q', f'-T{fmt}', *extra, str(AQUI / f'{nombre}.dot'),
                        '-o', str(SALIDA / f'{nombre}.{fmt}')], check=True)


if __name__ == '__main__':
    render('MER', mer(), 'neato')
    render('ModeloRelacional', relacional(), 'dot')
    print('Generados MER y ModeloRelacional (.dot, .png, .pdf)')
