package tests;

import logicaFakeTelefono.ClienteREST;
import logicaFakeTelefono.EscannerBeaconFake;
import logicaFakeTelefono.EstadoLogica;
import logicaFakeTelefono.LogicaFake;

//---------------------------------------------------------
//---------------------------------------------------------
//
// tests de la logica fake del telefono: deteccion simulada
// del beacon, obtencion del codigo, envio simulado y errores
//
// usa dobles del escaner y del cliente REST, sin red ni
// bluetooth, para que sea reproducible en cualquier PC
//
//---------------------------------------------------------
//---------------------------------------------------------
public class TestLogicaFakeTelefono {

  static int aciertos = 0;
  static int total = 0;

  //---------------------------------------------------------
  //
  // bool, Texto, Texto -> comprobar() -> void
  //
  // imprime OK:/FALLO: de cada test y lleva el recuento
  //
  //---------------------------------------------------------
  static void comprobar( boolean cond, String nombre, String motivo ) {
    total++;
    if ( cond ) {
      aciertos++;
      System.out.println( "OK: " + nombre );
    } else {
      System.out.println( "FALLO: " + nombre + " - " + motivo );
    }
  }

  //---------------------------------------------------------
  //
  // doble del cliente REST: respuestas preparadas y registro
  // del cuerpo que se le envio
  //
  //---------------------------------------------------------
  static class ClienteRESTFake implements ClienteREST {
    int codigoHTTP = 200;
    String cuerpoRespuesta = "{\"resultado\":true}";
    String ultimoCuerpo = null;

    public void guardarCodigo( int valor, Callback callback ) {
      this.ultimoCuerpo = "{\"valor\":" + valor + "}";
      callback.callback( this.codigoHTTP, this.cuerpoRespuesta );
    }
  }

  //---------------------------------------------------------
  //
  // doble de la UI: registra lo que la logica le notifica
  //
  //---------------------------------------------------------
  static class UIFake implements LogicaFake.CallbacksUI {
    EstadoLogica ultimoEstado = null;
    Integer codigoNotificado = null;
    Boolean envioCorrecto = null;

    public void estadoCambiado( EstadoLogica estado ) {
      this.ultimoEstado = estado;
    }
    public void codigoDetectado( int codigo ) {
      this.codigoNotificado = codigo;
    }
    public void envioTerminado( boolean correcto ) {
      this.envioCorrecto = correcto;
    }
  }

  //---------------------------------------------------------
  //
  // crea una logica nueva con sus dobles para cada test
  //
  //---------------------------------------------------------
  static class Escenario {
    final EscannerBeaconFake escanner = new EscannerBeaconFake();
    final ClienteRESTFake cliente = new ClienteRESTFake();
    final UIFake ui = new UIFake();
    final LogicaFake logica = new LogicaFake( this.escanner, this.cliente, this.ui );
  }

  //---------------------------------------------------------
  //
  // tests
  //
  //---------------------------------------------------------

  // 1- deteccion simulada del beacon
  static void testDeteccionSimulada() {
    Escenario e = new Escenario();
    boolean empezado = e.logica.empezarEscaneo();
    boolean beacon = e.escanner.simularLlegadaBeacon( 1234 );
    comprobar(
      empezado && beacon
        && e.logica.obtenerEstado() == EstadoLogica.DETECTADO
        && Integer.valueOf( 1234 ).equals( e.ui.codigoNotificado ),
      "deteccion simulada: el beacon 1234 llega y se notifica a la UI",
      "empezar=" + empezado + " beacon=" + beacon
        + " estado=" + e.logica.obtenerEstado()
        + " codigo notificado=" + e.ui.codigoNotificado
    );
  }

  // 2- obtencion del codigo
  static void testObtencionCodigo() {
    Escenario e = new Escenario();
    Object antes = e.logica.obtenerCodigoDetectado();
    e.logica.empezarEscaneo();
    e.escanner.simularLlegadaBeacon( 1234 );
    Object despues = e.logica.obtenerCodigoDetectado();
    comprobar(
      antes instanceof String && Integer.valueOf( 1234 ).equals( despues ),
      "obtencion del codigo: error antes y 1234 despues de detectar",
      "antes=" + antes + " despues=" + despues
    );
  }

  // 3- envio simulado con respuesta correcta del servidor
  static void testEnvioSimulado() {
    Escenario e = new Escenario();
    e.logica.empezarEscaneo();
    e.escanner.simularLlegadaBeacon( 1234 );
    Object lanzado = e.logica.enviarCodigo();
    comprobar(
      Boolean.TRUE.equals( lanzado )
        && e.logica.obtenerEstado() == EstadoLogica.ENVIADO
        && Boolean.TRUE.equals( e.ui.envioCorrecto )
        && "{\"valor\":1234}".equals( e.cliente.ultimoCuerpo ),
      "envio simulado: pasa a ENVIADO y el cuerpo enviado era {\"valor\":1234}",
      "lanzado=" + lanzado + " estado=" + e.logica.obtenerEstado()
        + " envio=" + e.ui.envioCorrecto + " cuerpo=" + e.cliente.ultimoCuerpo
    );
  }

  // 4- errores
  static void testErrores() {
    // enviar sin codigo detectado (en INACTIVO)
    Escenario e1 = new Escenario();
    Object sinCodigo = e1.logica.enviarCodigo();
    comprobar(
      sinCodigo instanceof String && e1.logica.obtenerEstado() == EstadoLogica.INACTIVO,
      "error: enviar sin codigo detectado devuelve texto de error",
      "devolvio " + sinCodigo
    );

    // enviar estando escaneando pero sin haber detectado nada
    Escenario e2 = new Escenario();
    e2.logica.empezarEscaneo();
    Object escaneando = e2.logica.enviarCodigo();
    comprobar(
      escaneando instanceof String && e2.logica.obtenerEstado() == EstadoLogica.ESCANEANDO,
      "error: enviar en ESCANEANDO sin codigo devuelve texto de error",
      "devolvio " + escaneando
    );

    // el servidor rechaza el codigo: sigue pendiente
    Escenario e3 = new Escenario();
    e3.cliente.cuerpoRespuesta = "{\"resultado\":false,\"error\":\"codigo invalido\"}";
    e3.logica.empezarEscaneo();
    e3.escanner.simularLlegadaBeacon( 1234 );
    e3.logica.enviarCodigo();
    comprobar(
      Boolean.FALSE.equals( e3.ui.envioCorrecto )
        && e3.logica.obtenerEstado() == EstadoLogica.DETECTADO,
      "error: el servidor rechaza el codigo y se queda en DETECTADO",
      "envio=" + e3.ui.envioCorrecto + " estado=" + e3.logica.obtenerEstado()
    );

    // fallo de red: el cliente devuelve codigo -1
    Escenario e4 = new Escenario();
    e4.cliente.codigoHTTP = -1;
    e4.cliente.cuerpoRespuesta = "";
    e4.logica.empezarEscaneo();
    e4.escanner.simularLlegadaBeacon( 1234 );
    e4.logica.enviarCodigo();
    comprobar(
      Boolean.FALSE.equals( e4.ui.envioCorrecto )
        && e4.logica.obtenerEstado() == EstadoLogica.DETECTADO,
      "error: fallo de red y se queda en DETECTADO",
      "envio=" + e4.ui.envioCorrecto + " estado=" + e4.logica.obtenerEstado()
    );

    // detener estando ya inactivo
    Escenario e5 = new Escenario();
    boolean detenido = e5.logica.detenerEscaneo();
    comprobar(
      ! detenido && e5.logica.obtenerEstado() == EstadoLogica.INACTIVO,
      "error: detener estando INACTIVO devuelve false",
      "detener devolvio " + detenido
    );
  }

  //---------------------------------------------------------
  //
  // recuento final y codigo de salida
  //
  //---------------------------------------------------------
  public static void main( String[] args ) {
    testDeteccionSimulada();
    testObtencionCodigo();
    testEnvioSimulado();
    testErrores();
    System.out.println( aciertos + "/" + total + " tests correctos" );
    System.exit( ( aciertos == total ) ? 0 : 1 );
  }
}
