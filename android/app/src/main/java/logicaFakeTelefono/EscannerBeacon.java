package logicaFakeTelefono;

//---------------------------------------------------------
//---------------------------------------------------------
//
// interfaz del escaner del beacon
//
// la implementacion real usa bluetooth; la fake simula
// la llegada del beacon para poder testear sin hardware
//
//---------------------------------------------------------
//---------------------------------------------------------
public interface EscannerBeacon {

  //---------------------------------------------------------
  //
  // int -> beaconDetectado() -> void
  //
  // avisa a quien escuche de que ha llegado un beacon
  //
  //---------------------------------------------------------
  public interface CallbackBeacon {
    void beaconDetectado( int codigo );
  }

  //---------------------------------------------------------
  //
  // CallbackBeacon -> empezarEscaneo() -> bool
  //
  // empieza a detectar beacons y devuelve true si ha sido posible
  //
  //---------------------------------------------------------
  boolean empezarEscaneo( CallbackBeacon callback );

  //---------------------------------------------------------
  //
  // -> detenerEscaneo() -> bool
  //
  // deja de detectar beacons y devuelve true si ha sido posible
  //
  //---------------------------------------------------------
  boolean detenerEscaneo();
}
