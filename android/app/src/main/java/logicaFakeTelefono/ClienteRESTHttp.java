package logicaFakeTelefono;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

//---------------------------------------------------------
//---------------------------------------------------------
//
// implementacion real del cliente REST: peticion POST con
// cuerpo {"valor":N} a la url que se le pase en el constructor
//
// corre en cualquier PC (sin dependencias de Android)
//
//---------------------------------------------------------
//---------------------------------------------------------
public class ClienteRESTHttp implements ClienteREST {

  private final String urlDestino;

  //---------------------------------------------------------
  //
  // Texto -> ClienteRESTHttp() -> ClienteRESTHttp
  //
  // urlDestino: url completa del endpoint guardarCodigo.php
  //
  //---------------------------------------------------------
  public ClienteRESTHttp( String urlDestino ) {
    this.urlDestino = urlDestino;
  }

  //---------------------------------------------------------
  //
  // int, Callback -> guardarCodigo() -> void
  //
  // lanza la peticion en otro hilo y devuelve la respuesta
  // por callback (codigo -1 si falla la red)
  //
  //---------------------------------------------------------
  public void guardarCodigo( final int valor, final Callback callback ) {
    Thread hilo = new Thread( new Runnable() {
      public void run() {
        int codigoHTTP = -1;
        String cuerpo = "";
        try {
          URL url = new URL( urlDestino );
          HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
          conexion.setRequestMethod( "POST" );
          conexion.setRequestProperty( "Content-Type", "application/json" );
          conexion.setDoOutput( true );

          DataOutputStream salida = new DataOutputStream( conexion.getOutputStream() );
          salida.writeBytes( "{\"valor\":" + valor + "}" );
          salida.flush();
          salida.close();

          codigoHTTP = conexion.getResponseCode();
          cuerpo = leerCuerpo( conexion );
          conexion.disconnect();

        } catch ( IOException e ) {
          codigoHTTP = -1; // fallo de red
        }
        callback.callback( codigoHTTP, cuerpo );
      }
    } );
    hilo.start();
  }

  //---------------------------------------------------------
  //
  // HttpURLConnection -> leerCuerpo() -> Texto
  //
  // acumula el cuerpo de la respuesta en un texto
  //
  //---------------------------------------------------------
  private static String leerCuerpo( HttpURLConnection conexion ) {
    try {
      BufferedReader lector = new BufferedReader(
        new InputStreamReader( conexion.getInputStream() )
      );
      StringBuilder acumulador = new StringBuilder();
      String linea;
      while ( ( linea = lector.readLine() ) != null ) {
        acumulador.append( linea );
      }
      lector.close();
      return acumulador.toString();
    } catch ( IOException e ) {
      return "";
    }
  }
}
