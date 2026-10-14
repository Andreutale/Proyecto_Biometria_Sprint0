package logicaFakeTelefono;

//---------------------------------------------------------
//---------------------------------------------------------
//
// implementacion fake del escaner: no usa bluetooth,
// la llegada del beacon se dispara con simularLlegadaBeacon()
//
//---------------------------------------------------------
//---------------------------------------------------------
public class EscannerBeaconFake implements EscannerBeacon {

  private boolean escaneando = false;
  private CallbackBeacon elCallback = null;

  //---------------------------------------------------------
  //
  // CallbackBeacon -> empezarEscaneo() -> bool
  //
  // guarda el callback y marca que ya se esta escaneando
  //
  //---------------------------------------------------------
  public boolean empezarEscaneo( CallbackBeacon callback ) {
    if ( this.escaneando ) {
      return false;
    }
    this.escaneando = true;
    this.elCallback = callback;
    return true;
  }

  //---------------------------------------------------------
  //
  // -> detenerEscaneo() -> bool
  //
  // corta la deteccion; false si no se estaba escaneando
  //
  //---------------------------------------------------------
  public boolean detenerEscaneo() {
    if ( ! this.escaneando ) {
      return false;
    }
    this.escaneando = false;
    this.elCallback = null;
    return true;
  }

  //---------------------------------------------------------
  //
  // int -> simularLlegadaBeacon() -> bool
  //
  // hace de beacon: solo avisa si se esta escaneando
  //
  //---------------------------------------------------------
  public boolean simularLlegadaBeacon( int codigo ) {
    if ( ! this.escaneando || this.elCallback == null ) {
      return false;
    }
    this.elCallback.beaconDetectado( codigo );
    return true;
  }
}
