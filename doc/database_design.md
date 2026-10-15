# database_design.md

## Component Design

================================================================================
TABLE: CODIGO
DESCRIPTION: Almacena los codigos recibidos del beacon por parte del movil.
             Cada envio valido crea una fila nueva; no se borran ni se
             actualizan filas.

COLUMNS:

+ id     | INT      | NOT NULL | Auto-Increment
+ valor  | INT      | NOT NULL
+ fecha  | DATETIME | NOT NULL

PRIMARY KEY: id

FOREIGN KEYS:

(ninguna)

CONSTRAINTS:

+ CHECK (valor >= 0 AND valor <= 65535)
================================================================================

Base de datos contenedora: `bd_codigos` (creada por `bd/crear_bd.sql`).

Descripcion de columnas:

- `id` — entero autoincremental, identificador unico de cada recepcion.
- `valor` — entero con el codigo numérico que traia el beacon (minor del
  beacon Bluetooth LE). Un codigo valido es un entero entre 0 y 65535;
  la restriccion CHECK lo garantiza a nivel de base de datos.
- `fecha` — marca temporal (Europe/Madrid) del momento en que el servidor
  ha recibido el codigo.

## Design Clarifications

- Escrituras y lecturas en la tabla CODIGO solo se realizan desde la capa
  de logica de negocio (`logica/guardarCodigo.php`, `logica/consultarUltimoCodigo.php`),
  nunca desde la capa de comunicacion REST.
- La consulta del ultimo codigo se resuelve con `SELECT ... ORDER BY id DESC LIMIT 1`
  sobre la clave primaria `id`.
- No hay borrados ni actualizaciones: el flujo del sistema solo inserta.

## General Rules

- Programming Language: SQL (MySQL / MariaDB via XAMPP).
- Code Readability: nombres en mayusculas para tablas y columnas como en
  el enunciado SSD; todo el script con comentarios breves en castellano.
- Automated Testing: `tests/test_bd.php` crea una BD aislada `bd_codigos_test`
  con la misma tabla, inserta filas reales y verifica restricciones
  (7 tests: esquema, CHECK, inserciones, ultimo codigo).
