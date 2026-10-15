# frontend_business_logic_design.md

## Component Design

Logica del cliente (telefono Android y navegador), organizada como un proxy:
la GUI unicamente llama a este componente; este encapsula toda la
comunicacion (Bluetooth LE en el telefono, XHR en el navegador).
Los servicios de infraestructura (escaner, cliente REST) se inyectan,
de modo que en los tests se sustituyen por dobles sin hardware ni red.

### Maquina de estados del telefono

```text
EstadoLogica = { INACTIVO, ESCANEANDO, DETECTADO, ENVIADO }

INACTIVO --empezarEscaneo()--> ESCANEANDO --beacon--> DETECTADO
                                 ^                      |
                    detenerEscaneo()          enviarCodigo() --> ENVIADO
```

### Clase LogicaFake (telefono)

```text
------------------------------------
|          LogicaFake              |
------------------------------------
| - elEscanner: EscannerBeacon     |
| - elCliente: ClienteREST         |
| - laUI: CallbacksUI              |
| - elEstado: EstadoLogica         |
| - elCodigo: Z                    |
------------------------------------
| + obtenerEstado(): EstadoLogica  |
| + empezarEscaneo(): B            |
| + detenerEscaneo(): B            |
| + obtenerCodigoDetectado(): Z    |
|                                  |
|   | error: Text                  |
| + enviarCodigo(): B              |
|                                  |
|   | error: Text                  |
------------------------------------
```

Comportamiento:

- `empezarEscaneo()` — valido desde `INACTIVO` y desde `ENVIADO`; `false`
  si ya se escanea o hay un codigo sin enviar.
- `detenerEscaneo()` — corta la deteccion y vuelve a `INACTIVO`; `false`
  si ya se estaba inactivo.
- `obtenerCodigoDetectado()` — devuelve el codigo o `"no hay codigo detectado"`.
- `enviarCodigo()` — solo con estado `DETECTADO`; lanza el envio asincrono;
  el resultado llega a la UI por callback (`envioTerminado(correcto: B)`).
  Respuesta aceptada = HTTP 200 con cuerpo `{"resultado":true}`
  (contrato de `communication_design.md`).
- Al llegar el beacon se para el escaneo, se guarda el codigo y se pasa
  a `DETECTADO`; la UI se entera por `estadoCambiado` / `codigoDetectado`.

Interfaces de inyeccion:

```text
------------------------------------
|        EscannerBeacon            |
------------------------------------
| + empezarEscaneo(cb): B          |
| + detenerEscaneo(): B            |
------------------------------------
  CallbackBeacon: beaconDetectado(codigo: Z) -> void
  (real: bluetooth LE; fake: simularLlegadaBeacon())

------------------------------------
|          ClienteREST             |
------------------------------------
| + guardarCodigo(valor: Z, cb)    |
------------------------------------
  Callback: callback(codigoHTTP: Z, cuerpo: Text) -> void
  (real: peticion HTTP; fake en tests: respuestas preparadas)
```

### Fake web (navegador)

```text
-> crearXHR() --> xhr
( valor: Z ) --> consultarUltimoCodigo( cb ) --> void
     resultado entregado por cb( err: Text | null, res: object | null )
```

- `crearXHR()` — factoria sustituible: los tests la cambian por un doble
  del XHR.
- `consultarUltimoCodigo( cb )` — hace `GET ../rest/consultarCodigo.php`
  y traduce la respuesta: `error 0` → `cb( null, {valor, fecha} )`;
  cualquier otra cosa → `cb( textoDeError, null )`.

### Paridad con la logica de negocio del servidor

- `ClienteREST.guardarCodigo( valor: Z, cb )` traslada por HTTP la misma
  operacion que `guardarCodigo( valor: Z ) --> B` del servidor
  (`business_logic_design.md`); la respuesta `B` llega traducida como
  HTTP 200 + `{"resultado":true}`.
- `consultarUltimoCodigo( cb )` del navegador equivale a
  `consultarUltimoCodigo() --> (valor: Z, fecha: Text) | error: Text`
  del servidor; se entrega por callback porque el navegador es asincrono.
- El resto de metodos (`empezarEscaneo`, `detenerEscaneo`,
  `obtenerCodigoDetectado`, `enviarCodigo`) son propios del telefono y no
  tienen contrapartida en el servidor.

## Design Clarifications

- La GUI (y la pagina web) no conocen HTTP ni Bluetooth: solo invocan este
  componente y pintan el estado que este le notifica.
- En pruebas sin servidor se sustituye `ClienteREST` por un doble cuyo
  callback recibe respuestas preparadas; en pruebas sin navegador se
  sustituye `crearXHR()` por un doble identico.
- Los textos de error (`"no hay codigo detectado"`, `"error en la
  peticion"`, `"respuesta no valida"`) se devuelven tal cual a la capa de
  presentacion.

## General Rules

- Programming Language: Java 17 (telefono, paquete `logicaFakeTelefono`)
  y JavaScript clasico de navegador, sin import/export (web,
  `ux/logicaFake/`).
- Function/Method Headers: cada interfaz, clase y metodo con cabecera de
  comentario (lineas de guiones) y firma logica, sin detalles internos.
- Code Readability: nombres en castellano; estados y callbacks con
  nombres explicitos; sin comentarios obvios en el cuerpo.
- Automated Testing: `tests/TestLogicaFakeTelefono.java` (8 tests: doble
  del escaner, doble del servidor, maquina de estados, errores) y
  `tests/test_logica_fake_web.js` (3 tests: exito, error HTTP, cuerpo no
  JSON) — reproducibles sin hardware ni navegador.
