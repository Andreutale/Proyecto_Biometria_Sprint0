# communication_design.md

## Component Design

Capa REST del servidor. Traduce peticiones HTTP a llamadas de la logica
de negocio (`business_logic_design.md`) y sus resultados a JSON.
Transporte: HTTP sobre puerto 8080; cuerpos JSON UTF-8.

### Endpoint 1 — guardar codigo

```text
POST /rest/guardarCodigo.php
Content-Type: application/json

entrada:  { "valor": Z }          (cuerpo de la peticion)
          → guardarCodigo( valor: Z ) --> B     [logica de negocio]

salida:   { "resultado": true }                 si la logica devuelve true
          { "resultado": false, "error": Text } si la logica devuelve false
```

### Endpoint 2 — consultar ultimo codigo

```text
GET /rest/consultarCodigo.php
(sin parametros)

entrada:  → consultarUltimoCodigo() --> (valor: Z, fecha: Text) | error: Text
                                                             [logica de negocio]

salida:   { "valor": Z, "fecha": Text, "error": 0 }   si hay codigo guardado
          { "error": Text }                           si no hay codigos
```

`error: 0` es el marcador numerico de "sin error"; `error: Text` es el
texto de error que devuelve la logica.

Ficheros: `rest/guardarCodigo.php`, `rest/consultarCodigo.php`
(infraestructura de conexion: `logica/config.php`).

## Design Clarifications

- La capa de comunicacion solo hace de traductor: lee `php://input`,
  valida que el JSON tenga el campo `valor` numerico, llama a la logica de
  negocio y escribe la respuesta JSON. No contiene reglas de negocio ni
  consulta la base de datos directamente.
- Direccion de flujo unica: REST → logica de negocio → base de datos
  (la comunicacion jamas se invoca desde la logica).
- Cualquier entrada malformada (sin campo `valor`, valor no numerico) se
  responde con `{"resultado":false,"error":"..."}` o `{"error":"..."}`,
  sin llegar a la logica de negocio.

## General Rules

- Programming Language: PHP 8.
- Function/Method Headers: cabeceras de comentario con la firma logica
  invocada y la forma del JSON de entrada/salida, delimitadas por lineas
  de guiones.
- Code Readability: sin comentarios obvios; la logica de traduccion
  (entrada → logica → salida) se lee de arriba abajo en cada endpoint.
- Automated Testing: `tests/test_rest.php` levanta el servidor y verifica
  los contratos JSON de ambos endpoints con `curl`
  (5 tests: guardar valido, guardar invalido, consultar con datos,
  consultar sin datos, error de JSON malformado).
