// ---------------------------------------------------
//
// versión fake de una función de la lógica
//
// -> consultarUltimoCodigo() -> (valor:Entero, fecha:Fecha) | error:Texto
//
// (valor:Entero, fecha:Fecha) | error:Texto : devuelto via callback( err, res )
//
// solo hace la petición y traduce la respuesta,
// no contiene ninguna regla de negocio
//
// ---------------------------------------------------

// los tests sustituyen esta función por un doble del XHR
function crearXHR() {
  return new XMLHttpRequest()
}

function consultarUltimoCodigo( cb ) {

  var peticion = crearXHR()

  peticion.onreadystatechange = function() {

    // solo interesa la respuesta completa
    if ( this.readyState != 4 ) {
      return
    }

    // fallo HTTP: no hubo respuesta utilizable
    if ( this.status != 200 ) {
      cb( "error en la petición", null )
      return
    }

    // si el cuerpo no es JSON, tambien es un error
    var resultado
    try {
      resultado = JSON.parse( this.responseText )
    } catch ( e ) {
      cb( "respuesta no válida", null )
      return
    }

    // error 0 significa que si hay código guardado
    if ( resultado && resultado.error === 0 ) {
      cb( null, resultado )
      return
    }

    // el servidor ha devuelto un texto de error
    cb( ( resultado && resultado.error ) ? resultado.error : "respuesta no válida", null )
  }

  // la url es relativa al HTML que llama a esta función
  peticion.open( "GET", "../rest/consultarCodigo.php", true )
  peticion.send()

} // ()
