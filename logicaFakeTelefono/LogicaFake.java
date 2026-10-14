package logicaFakeTelefono;

//---------------------------------------------------------
//---------------------------------------------------------
//
// maquina de estados de la logica del telefono
//
// INACTIVO --empezarEscaneo()--> ESCANEANDO --beacon--> DETECTADO
//                                  ^                      |
//                     detenerEscaneo()          enviarCodigo() -> ENVIADO
//
// el escaner y el cliente REST se inyectan en el constructor,
// de modo que en los tests se usan dobles sin red ni bluetooth
//
//---------------------------------------------------------
//---------------------------------------------------------
public class LogicaFake {

  //---------------------------------------------------------
  //
  // callbacks para que la UI se entere de lo que pasa
  //
  //---------------------------------------------------------
  public interface CallbacksUI {
    void estadoCambiado( EstadoLogica estado );
    void codigoDetectado( int codigo );
    void envioTerminado( boolean correcto );
  }

  private final EscannerBeacon elEscanner;
  private final ClienteREST elCliente;
  private final CallbacksUI laUI;

  private EstadoLogica elEstado = EstadoLogica.INACTIVO;
  private Integer elCodigo = null;

  //---------------------------------------------------------
  //
  // EscannerBeacon, ClienteREST, CallbacksUI -> LogicaFake() -> LogicaFake
  //
  //---------------------------------------------------------
  public LogicaFake( EscannerBeacon escanner, ClienteREST cliente, CallbacksUI ui ) {
    this.elEscanner = escanner;
    this.elCliente = cliente;
    this.laUI = ui;
  }

  //---------------------------------------------------------
  //
  // -> obtenerEstado() -> EstadoLogica
  //
  //---------------------------------------------------------
  public EstadoLogica obtenerEstado() {
    return this.elEstado;
  }

  //---------------------------------------------------------
  //
  // -> empezarEscaneo() -> bool
  //
  // valido desde INACTIVO y desde ENVIADO; false si ya se
  // esta escaneando o hay un codigo sin enviar
  //
  //---------------------------------------------------------
  public boolean empezarEscaneo() {
    if ( this.elEstado != EstadoLogica.INACTIVO
      && this.elEstado != EstadoLogica.ENVIADO ) {
      return false;
    }
    boolean empezado = this.elEscanner.empezarEscaneo( new EscannerBeacon.CallbackBeacon() {
      public void beaconDetectado( int codigo ) {
        alLlegarBeacon( codigo );
      }
    } );
    if ( ! empezado ) {
      return false;
    }
    this.elCodigo = null;
    cambiarEstado( EstadoLogica.ESCANEANDO );
    return true;
  }

  //---------------------------------------------------------
  //
  // -> detenerEscaneo() -> bool
  //
  // corta la deteccion y vuelve a INACTIVO; false si ya
  // se estaba inactivo
  //
  //---------------------------------------------------------
  public boolean detenerEscaneo() {
    if ( this.elEstado == EstadoLogica.INACTIVO ) {
      return false;
    }
    this.elEscanner.detenerEscaneo();
    this.elCodigo = null;
    cambiarEstado( EstadoLogica.INACTIVO );
    return true;
  }

  //---------------------------------------------------------
  //
  // -> obtenerCodigoDetectado() -> Entero | error:Texto
  //
  // devuelve el codigo detectado, o un texto de error si
  // aun no hay ninguno
  //
  //---------------------------------------------------------
  public Object obtenerCodigoDetectado() {
    if ( this.elCodigo == null ) {
      return "no hay codigo detectado";
    }
    return this.elCodigo;
  }

  //---------------------------------------------------------
  //
  // -> enviarCodigo() -> bool | error:Texto
  //
  // solo vale con un codigo detectado: lanza el envio al
  // servidor; el resultado llega por el callback de la UI
  //
  //---------------------------------------------------------
  public Object enviarCodigo() {
    if ( this.elEstado != EstadoLogica.DETECTADO || this.elCodigo == null ) {
      return "no hay codigo detectado";
    }
    final int valor = this.elCodigo;
    this.elCliente.guardarCodigo( valor, new ClienteREST.Callback() {
      public void callback( int codigoHTTP, String cuerpo ) {
        alRecibirRespuestaServidor( codigoHTTP, cuerpo );
      }
    } );
    return Boolean.TRUE;
  }

  //---------------------------------------------------------
  //
  // int -> alLlegarBeacon() -> void
  //
  // al llegar el beacon se para el escaneo, se guarda el
  // codigo y se pasa a DETECTADO
  //
  //---------------------------------------------------------
  private void alLlegarBeacon( int codigo ) {
    if ( this.elEstado != EstadoLogica.ESCANEANDO ) {
      return;
    }
    this.elEscanner.detenerEscaneo();
    this.elCodigo = codigo;
    cambiarEstado( EstadoLogica.DETECTADO );
    this.laUI.codigoDetectado( codigo );
  }

  //---------------------------------------------------------
  //
  // int, Texto -> alRecibirRespuestaServidor() -> void
  //
  // HTTP 200 con {"resultado":true} -> ENVIADO; cualquier
  // otra cosa (rechazo o fallo de red) deja el codigo
  // pendiente de reintentar
  //
  //---------------------------------------------------------
  private void alRecibirRespuestaServidor( int codigoHTTP, String cuerpo ) {
    boolean correcto = codigoHTTP == 200
      && cuerpo != null
      && cuerpo.contains( "\"resultado\":true" );
    if ( correcto ) {
      cambiarEstado( EstadoLogica.ENVIADO );
    }
    this.laUI.envioTerminado( correcto );
  }

  //---------------------------------------------------------
  //
  // EstadoLogica -> cambiarEstado() -> void
  //
  // actualiza el estado y avisa a la UI
  //
  //---------------------------------------------------------
  private void cambiarEstado( EstadoLogica nuevo ) {
    this.elEstado = nuevo;
    this.laUI.estadoCambiado( nuevo );
  }
}
