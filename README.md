# Sprint 0 — Proyecto Aplicaciones de Biometría y Medio Ambiente

Sistema completo sensor → móvil → servidor → base de datos → web.
Un emisor Bluetooth LE (Arduino nRF52) anuncia un iBeacon; una app
Android lo detecta y envía su código numérico por REST a un servidor
PHP que lo guarda en MySQL; una página web consulta el último código
recibido.

## Arquitectura

```text
Arduino (iBeacon "Andreutale", código 1234)
   ↓ Bluetooth LE
Móvil Android (escanea, valida y envía el código)
   ↓ HTTP POST /rest/guardarCodigo.php
Servidor PHP (rest/ -> logica/)
   ↓ INSERT
MySQL (bd_codigos.CODIGO)
   ↓ HTTP GET /rest/consultarCodigo.php
Web (ux/Aplicacion.html)
```

Regla de dependencias: la comunicación (REST) llama a la lógica de
negocio (`logica/`), y esta a la base de datos (`bd/`); la lógica de
negocio no conoce HTTP. En el móvil y en la web, la interfaz solo
habla con su lógica fake, que es quien encapsula la comunicación.

## Estructura

| Carpeta | Contenido |
|---|---|
| `bd/` | `crear_bd.sql` — base de datos `bd_codigos`, tabla `CODIGO` |
| `logica/` | Lógica de negocio PHP: `guardarCodigo()`, `consultarUltimoCodigo()`, `config.php` |
| `rest/` | Endpoints REST: `guardarCodigo.php` (POST), `consultarCodigo.php` (GET) |
| `ux/` | Página web de consulta + `logicaFake/consultarUltimoCodigo.js` |
| `logicaFakeTelefono/` | Máquina de estados y fakes del móvil (Java): `LogicaFake`, `EstadoLogica`, interfaces |
| `android/` | App Android (proyecto Gradle): 3 botones, textos según especificación |
| `arduino/HolaMundoIBeacon/` | Emisor iBeacon para placa nRF52 (Feather nRF52840 / nRF52840 Mini) |
| `tests/` | Tests automáticos de las 4 áreas (sin hardware ni navegador) |
| `doc/` | Diseños de los componentes (`*_design.md`) |
| `author.md` | Autor del proyecto |

## Requisitos

- XAMPP con PHP 8 y MySQL en el PC que hace de servidor.
- Placa nRF52 + Arduino IDE (o arduino-cli) para el beacon.
- Android Studio (SDK 34) para compilar la app.
- PC y móvil en la misma red Wi-Fi.

## Puesta en marcha

1. **Base de datos:** ejecutar `bd/crear_bd.sql` en phpMyAdmin
   (o `mysql -u root < bd/crear_bd.sql`).
2. **Servidor PHP:** desde la carpeta `Proyecto/`:
   `php -S 0.0.0.0:8080`
   (en XAMPP: `C:\xampp\php\php.exe -S 0.0.0.0:8080`).
   Abrir firewall para el puerto 8080 si el móvil no conecta.
3. **Beacon:** abrir `arduino/HolaMundoIBeacon/HolaMundoIBeacon.ino`
   y subirlo a la placa.
4. **App móvil:** compilar `android/` (Gradle) e instalar el APK, o
   copiar `app/build/outputs/apk/debug/app-debug.apk`.
   Ajustar `MainActivity.URL_SERVIDOR` a la IP del PC:
   `http://<IP_PC>:8080/rest/guardarCodigo.php`.
5. **Web:** abrir `http://<IP_PC>:8080/ux/Aplicacion.html`.

## Contratos REST

```text
POST rest/guardarCodigo.php   cuerpo {"valor":1234}
  -> {"resultado":true}  o  {"resultado":false,"error":"..."}

GET  rest/consultarCodigo.php
  -> {"valor":1234,"fecha":"2026-10-15 13:21:11","error":0}
  o  {"error":"no hay códigos guardados"}
```

## Tests

Todos se ejecutan sin hardware ni navegador y notifican `OK:`/`FALLO:`
con un resumen `X/Y tests correctos`:

| Comando | Qué prueba | Tests |
|---|---|---|
| `php tests/test_bd.php` | Esquema, CHECK e inserciones de `CODIGO` | 7 |
| `php tests/test_logica.php` | `guardarCodigo()` y `consultarUltimoCodigo()` | 6 |
| `php tests/test_rest.php` | Contratos JSON de los endpoints (servidor en 8080) | 5 |
| Ejecutar `tests/TestLogicaFakeTelefono.java` (Java 17) | Máquina de estados del móvil con dobles | 8 |
| `node tests/test_logica_fake_web.js` | Fake web con doble del XHR | 3 |
| Abrir `tests/test_ux.html` en el navegador | Textos y visibilidades de la web | 4 |
