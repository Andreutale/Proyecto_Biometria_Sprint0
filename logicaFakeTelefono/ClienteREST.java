package logicaFakeTelefono;

//---------------------------------------------------------
//---------------------------------------------------------
//
// interfaz del cliente REST del telefono
//
// la implementacion real hace la peticion HTTP; en los
// tests se usa una doble con respuestas preparadas
//
//---------------------------------------------------------
//---------------------------------------------------------
public interface ClienteREST {

  //---------------------------------------------------------
  //
  // int, Texto -> callback() -> void
  //
  // devuelve el codigo HTTP y el cuerpo de la respuesta;
  // codigo -1 significa fallo de red
  //
  //---------------------------------------------------------
  public interface Callback {
    void callback( int codigoHTTP, String cuerpo );
  }

  //---------------------------------------------------------
  //
  // int, Callback -> guardarCodigo() -> void
  //
  // envia el codigo al servidor y avisa por callback al terminar
  //
  //---------------------------------------------------------
  void guardarCodigo( int valor, Callback callback );
}
