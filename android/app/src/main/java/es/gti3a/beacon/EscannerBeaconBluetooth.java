package es.gti3a.beacon;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;

import logicaFakeTelefono.EscannerBeacon;

//---------------------------------------------------------
//---------------------------------------------------------
//
// implementacion real del escaner: busca el beacon por
// nombre y devuelve su minor como codigo detectado
//
// reemplaza a EscannerBeaconFake en el movil
//
//---------------------------------------------------------
//---------------------------------------------------------
public class EscannerBeaconBluetooth implements EscannerBeacon {

  private static final String ETIQUETA_LOG = "Beacon";

  private final Activity laActividad;
  private final String elNombreBuscado;

  private BluetoothLeScanner elEscanner = null;
  private ScanCallback elCallbackScan = null;
  private CallbackBeacon elCallbackBeacon = null;

  //---------------------------------------------------------
  //
  // Activity, Texto -> EscannerBeaconBluetooth() -> EscannerBeaconBluetooth
  //
  // nombreBuscado: nombre del beacon que hay que detectar
  //
  //---------------------------------------------------------
  public EscannerBeaconBluetooth( Activity actividad, String nombreBuscado ) {
    this.laActividad = actividad;
    this.elNombreBuscado = nombreBuscado;
  }

  //---------------------------------------------------------
  //
  // CallbackBeacon -> empezarEscaneo() -> bool
  //
  // empieza a escanear beacons; false si ya se escaneaba
  // o no hay permisos/bluetooth
  //
  //---------------------------------------------------------
  public boolean empezarEscaneo( CallbackBeacon callback ) {
    if ( this.elCallbackScan != null ) {
      return false;
    }
    if ( ! tienePermisoScan() ) {
      Log.d( ETIQUETA_LOG, " empezarEscaneo(): sin permiso BLUETOOTH_SCAN" );
      return false;
    }

    BluetoothManager gestor = (BluetoothManager)
      this.laActividad.getSystemService( Context.BLUETOOTH_SERVICE );
    BluetoothAdapter adaptador = ( gestor == null ) ? null : gestor.getAdapter();
    if ( adaptador == null ) {
      return false;
    }
    this.elEscanner = adaptador.getBluetoothLeScanner();
    if ( this.elEscanner == null ) {
      return false;
    }

    this.elCallbackBeacon = callback;
    this.elCallbackScan = new ScanCallback() {
      @Override
      public void onScanResult( int callbackType, ScanResult resultado ) {
        super.onScanResult( callbackType, resultado );
        alRecibirAnuncio( resultado );
      }

      @Override
      public void onScanFailed( int errorCode ) {
        super.onScanFailed( errorCode );
        Log.d( ETIQUETA_LOG, " empezarEscaneo(): fallo de escaneo " + errorCode );
      }
    };

    // escanea todo y filtra al recibir: el anuncio iBeacon y el
    // nombre del dispositivo pueden llegar en paquetes distintos
    ScanSettings ajustes = new ScanSettings.Builder()
      .setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY )
      .build();

    this.elEscanner.startScan( new ArrayList<ScanFilter>(), ajustes, this.elCallbackScan );
    Log.d( ETIQUETA_LOG, " empezarEscaneo(): escaneando " + this.elNombreBuscado );
    return true;
  }

  //---------------------------------------------------------
  //
  // -> detenerEscaneo() -> bool
  //
  // deja de escanear; false si no se estaba escaneando
  //
  //---------------------------------------------------------
  public boolean detenerEscaneo() {
    if ( this.elCallbackScan == null || this.elEscanner == null ) {
      return false;
    }
    if ( tienePermisoScan() ) {
      this.elEscanner.stopScan( this.elCallbackScan );
    }
    this.elCallbackScan = null;
    this.elCallbackBeacon = null;
    Log.d( ETIQUETA_LOG, " detenerEscaneo(): escaneo detenido" );
    return true;
  }

  //---------------------------------------------------------
  //
  // ScanResult -> alRecibirAnuncio() -> void
  //
  // si el anuncio es un iBeacon del beacon buscado, extrae
  // el minor (el codigo) y se lo avisa a la logica
  //
  //---------------------------------------------------------
  private void alRecibirAnuncio( ScanResult resultado ) {
    if ( this.elCallbackBeacon == null || resultado.getScanRecord() == null ) {
      return;
    }

    // si el nombre ya se conoce, solo sirve el beacon buscado
    String nombre = resultado.getDevice().getName();
    if ( nombre != null && ! nombre.equals( this.elNombreBuscado ) ) {
      return;
    }

    byte[] bytes = resultado.getScanRecord().getBytes();
    if ( bytes == null || bytes.length < 30 ) {
      return; // anuncio corto, sin trama iBeacon
    }

    TramaIBeacon laTrama = new TramaIBeacon( bytes );

    // companyID 0x004c (Apple) en little endian y tipo 0x02 = iBeacon
    byte[] companyID = laTrama.getCompanyID();
    if ( companyID.length < 2
      || companyID[0] != 0x4c || companyID[1] != 0x00
      || laTrama.getiBeaconType() != 0x02 ) {
      return;
    }

    int codigo = bytesAEntero( laTrama.getMinor() );
    Log.d( ETIQUETA_LOG, " alRecibirAnuncio(): beacon " + nombre + " codigo=" + codigo );

    // la logica es la que decide el estado y para el escaneo
    this.elCallbackBeacon.beaconDetectado( codigo );
  }

  //---------------------------------------------------------
  //
  // byte[] -> bytesAEntero() -> Entero
  //
  // major/minor del iBeacon van en big endian
  //
  //---------------------------------------------------------
  private static int bytesAEntero( byte[] bytes ) {
    if ( bytes == null || bytes.length < 2 ) {
      return -1;
    }
    return ( ( bytes[0] & 0xff ) << 8 ) | ( bytes[1] & 0xff );
  }

  //---------------------------------------------------------
  //
  // -> tienePermisoScan() -> bool
  //
  //---------------------------------------------------------
  private boolean tienePermisoScan() {
    return laActividad.checkSelfPermission( Manifest.permission.BLUETOOTH_SCAN )
      == PackageManager.PERMISSION_GRANTED;
  }
}
