package logicaFakeTelefono;

//---------------------------------------------------------
//---------------------------------------------------------
//
// estados posibles de la logica del telefono
//
// INACTIVO    - sin escanear, pantalla en blanco
// ESCANEANDO  - buscando el beacon
// DETECTADO   - hay un codigo pendiente de enviar
// ENVIADO     - el servidor ha confirmado el envio
//
//---------------------------------------------------------
//---------------------------------------------------------
public enum EstadoLogica {
  INACTIVO,
  ESCANEANDO,
  DETECTADO,
  ENVIADO
}
