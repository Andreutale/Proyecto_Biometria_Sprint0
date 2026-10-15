# business_logic_design.md

## Component Design

Tipos logicos: `Z` = entero, `Text` = texto, `B` = booleano,
`(valor: Z, fecha: Text) | error: Text` = tupla resultado o texto de error.

```text
valor: Z --> guardarCodigo() --> B

-> consultarUltimoCodigo() --> (valor: Z, fecha: Text) | error: Text

-> conexionBD() --> conexion | error: Text
```

`guardarCodigo( valor )`:

- Recibe el codigo detectado por el beacon.
- Si no es un entero entre 0 y 65535 → `false` (sin tocar la base de datos).
- Si es valido → inserta una fila en `CODIGO` con `fecha` = ahora
  (zona `Europe/Madrid`) → `true`.
- No valida el rango en el INSERT por duplicidad: la restriccion CHECK de
  `database_design.md` es la garantia de base de datos; la logica la aplica
  antes para no escribir de mas.

`consultarUltimoCodigo()`:

- Devuelve el `valor` y la `fecha` de la fila de mayor `id` de la tabla
  `CODIGO` (tabla definida en `database_design.md`).
- Si la tabla esta vacia (o falla la consulta) → texto de error
  `"no hay codigos guardados"`.

`conexionBD()` (apoyo):

- Devuelve la unica conexion PDO a la base de datos `bd_codigos`
  (parametros definidos una sola vez en `logica/config.php`).

Base de datos usada: tabla `CODIGO` de `database_design.md`
(`bd_codigos.CODIGO(id, valor, fecha)`; CHECK valor entre 0 y 65535).

## Design Clarifications

- La logica de negocio no conoce HTTP, JSON, ni cabeceras: solo recibe
  enteros y devuelve booleanos, tuplas o textos de error.
- El mensaje de error `"no hay codigos guardados"` se devuelve identico
  tanto si la tabla esta vacia como si falla la conexion (el REST lo
  traslada tal cual al cliente).
- Los codigos invalidos se rechazan antes de escribir: no se inserta
  ninguna fila (cubierto por tests automaticos).

## General Rules

- Programming Language: PHP 8 (servidor local XAMPP; sin frameworks).
- Function/Method Headers: cada funcion lleva su firma logica en un bloque
  de comentario delimitado por lineas de guiones, con su proposito en una
  frase, sin detalles internos:

  ```text
  //---------------------------------------------------------
  //
  // valor:Entero -> guardarCodigo() -> bool
  //
  // guarda el codigo si es valido...
  //
  //---------------------------------------------------------
  ```

- Code Readability: nombres en castellano sin abreviar, sin comentarios
  innecesarios dentro del cuerpo, sin codigo muerto; las reglas de negocio
  solo se escriben una vez.
- Automated Testing: `tests/test_logica.php` prueba directamente las
  funciones de este componente (6 tests: rechazo de invalidos, insercion
  valida, consulta de la ultima fila, error con tabla vacia) ejecutando
  `php tests/test_logica.php` sin hardware ni navegador.
