package es.gti3a.beacon;

import android.Manifest;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import logicaFakeTelefono.ClienteRESTHttp;
import logicaFakeTelefono.EstadoLogica;
import logicaFakeTelefono.LogicaFake;

//---------------------------------------------------------
//---------------------------------------------------------
//
// pantalla principal del telefono: 3 botones (Escanear,
// Detener y Enviar) y un texto con el estado del proceso
//
// toda la decision (estados, validacion, envio) la toma la
// logica de la spec 04; aqui solo se pinta y se le pasa
// las pulsaciones
//
//---------------------------------------------------------
//---------------------------------------------------------
public class MainActivity extends AppCompatActivity {

  private static final String ETIQUETA_LOG = "Beacon";

  private static final int CODIGO_PETICION_PERMISOS = 11223344;

  // nombre que el beacon del sensor pone en su anuncio
  private static final String NOMBRE_BEACON = "Andreutale";

  // en el emulador 10.0.2.2 apunta al PC; con movil fisico
  // hay que poner la IP del PC en la misma red
  private static final String URL_SERVIDOR =
    "http://10.0.2.2:8080/rest/guardarCodigo.php";

  private TextView elTexto = null;
  private Button botonEscanear = null;
  private Button botonDetener = null;
  private Button botonEnviar = null;

  private LogicaFake laLogica = null;

  //---------------------------------------------------------
  //
  // -> onCreate() -> void
  //
  // monta la pantalla y conecta la logica con la UI
  //
  //---------------------------------------------------------
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    this.elTexto = (TextView) findViewById(R.id.texto);
    this.botonEscanear = (Button) findViewById(R.id.botonEscanear);
    this.botonDetener = (Button) findViewById(R.id.botonDetener);
    this.botonEnviar = (Button) findViewById(R.id.botonEnviar);

    this.laLogica = new LogicaFake(
      new EscannerBeaconBluetooth(this, NOMBRE_BEACON),
      new ClienteRESTHttp(URL_SERVIDOR),
      new LogicaFake.CallbacksUI() {
        @Override
        public void estadoCambiado(final EstadoLogica estado) {
          runOnUiThread(new Runnable() {
            public void run() {
              alCambiarEstado(estado);
            }
          });
        }

        @Override
        public void codigoDetectado(final int codigo) {
          runOnUiThread(new Runnable() {
            public void run() {
              alDetectarCodigo(codigo);
            }
          });
        }

        @Override
        public void envioTerminado(final boolean correcto) {
          runOnUiThread(new Runnable() {
            public void run() {
              alTerminarEnvio(correcto);
            }
          });
        }
      }
    );

    pintarSegunEstado(EstadoLogica.INACTIVO);
    pedirPermisos();

    Log.d(ETIQUETA_LOG, " onCreate(): aplicacion lista");
  }

  //---------------------------------------------------------
  //
  // EstadoLogica -> alCambiarEstado() -> void
  //
  // el texto de pantalla y los botones segun el estado
  //
  //---------------------------------------------------------
  private void alCambiarEstado(EstadoLogica estado) {
    // DETECTADO y ENVIADO lo pintan sus propios callbacks
    if (estado == EstadoLogica.INACTIVO) {
      this.elTexto.setText("");
    } else if (estado == EstadoLogica.ESCANEANDO) {
      this.elTexto.setText("Escaneando...");
    }
    pintarSegunEstado(estado);
  }

  //---------------------------------------------------------
  //
  // Entero -> alDetectarCodigo() -> void
  //
  // muestra el codigo recien detectado en pantalla
  //
  //---------------------------------------------------------
  private void alDetectarCodigo(int codigo) {
    this.elTexto.setText("Código: " + codigo);
  }

  //---------------------------------------------------------
  //
  // bool -> alTerminarEnvio() -> void
  //
  // confirma en pantalla que el servidor ha guardado el codigo
  //
  //---------------------------------------------------------
  private void alTerminarEnvio(boolean correcto) {
    if (correcto) {
      this.elTexto.setText("¡Enviado con éxito!");
    }
    // si falla, el texto del codigo se queda para reintentar
  }

  //---------------------------------------------------------
  //
  // EstadoLogica -> pintarSegunEstado() -> void
  //
  // Escanear en INACTIVO/ENVIADO, Detener mientras busca,
  // Enviar solo con un codigo detectado
  //
  //---------------------------------------------------------
  private void pintarSegunEstado(EstadoLogica estado) {
    boolean enInactivo = (estado == EstadoLogica.INACTIVO);
    boolean escaneando = (estado == EstadoLogica.ESCANEANDO);
    boolean detectado = (estado == EstadoLogica.DETECTADO);
    boolean enviado = (estado == EstadoLogica.ENVIADO);

    this.botonEscanear.setVisibility((enInactivo || enviado) ? View.VISIBLE : View.GONE);
    this.botonDetener.setVisibility((escaneando || detectado) ? View.VISIBLE : View.GONE);
    this.botonEnviar.setVisibility(detectado ? View.VISIBLE : View.GONE);
  }

  //---------------------------------------------------------
  //
  // View -> botonEscanearPulsado() -> void
  //
  // empieza la deteccion del beacon
  //
  //---------------------------------------------------------
  public void botonEscanearPulsado(View v) {
    this.laLogica.empezarEscaneo();
  }

  //---------------------------------------------------------
  //
  // View -> botonDetenerPulsado() -> void
  //
  // detiene la deteccion del beacon
  //
  //---------------------------------------------------------
  public void botonDetenerPulsado(View v) {
    this.laLogica.detenerEscaneo();
  }

  //---------------------------------------------------------
  //
  // View -> botonEnviarPulsado() -> void
  //
  // envia el codigo detectado al servidor
  //
  //---------------------------------------------------------
  public void botonEnviarPulsado(View v) {
    this.laLogica.enviarCodigo();
  }

  //---------------------------------------------------------
  //
  // -> pedirPermisos() -> void
  //
  // pide los permisos de bluetooth y ubicacion en arranque
  //
  //---------------------------------------------------------
  private void pedirPermisos() {
    requestPermissions(new String[]{
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
      },
      CODIGO_PETICION_PERMISOS);
  }

  //---------------------------------------------------------
  //
  // con el resultado de la peticion de permisos, se
  // comprueba si el escaneo podra funcionar
  //
  //---------------------------------------------------------
  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                         int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == CODIGO_PETICION_PERMISOS) {
      if (grantResults.length > 0
        && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
        Log.d(ETIQUETA_LOG, " onRequestPermissionsResult(): permisos concedidos");
      } else {
        Log.d(ETIQUETA_LOG, " onRequestPermissionsResult(): permisos NO concedidos");
      }
    }
  }
}
