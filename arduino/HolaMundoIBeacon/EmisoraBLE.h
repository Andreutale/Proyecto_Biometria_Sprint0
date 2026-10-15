// -*- mode: c++ -*-

#ifndef EMISORA_H_INCLUIDO
#define EMISORA_H_INCLUIDO

#include "ServicioEnEmisora.h"

// ============================================================================
// Autor: Raúl Sanabria Rodado
//
// Fecha:1/10/26
// ============================================================================

// ============================================================================
// CLASE: EmisoraBLE
// Proposito: Gestionar la capa de radio BLE (anuncios y beacons) con Bluefruit.
// Responsabilidades (segun PDF Seccion 6):
//   - Inicializar el modulo Bluefruit y configurar potencia de emision.
//   - Detener e iniciar anuncios BLE.
//   - Empaquetar y radiar tramas iBeacon estandar de 30 bytes.
// ============================================================================
class EmisoraBLE {
private:
    const char * nombreEmisora;   // Nombre anunciado por BLE (ej: "GTI3A-2025")
    const uint16_t fabricanteID;  // Identificador de compania (0x004C = Apple)
    const int8_t txPower;         // Potencia de salida en dBm

public:
    using CallbackConexionEstablecida = void ( uint16_t connHandle );
    using CallbackConexionTerminada = void ( uint16_t connHandle, uint8_t reason);

    // Constructor: Asigna parametros pero NO inicializa la radio aqui
    EmisoraBLE( const char * nombreEmisora_, const uint16_t fabricanteID_,
                const int8_t txPower_ )
            :
            nombreEmisora( nombreEmisora_ ) ,
            fabricanteID( fabricanteID_ ) ,
            txPower( txPower_ )
    {
    }

    // Enciende la pila de radio Bluefruit y detiene anuncios residuales
    void encenderEmisora() {
        // --- diagnostico: comprobar si la pila BLE arranca ---
        bool ok = Bluefruit.begin();
        Globales::elPuerto.escribir( "diagnostico: Bluefruit.begin() = " );
        Globales::elPuerto.escribir( ok ? "true" : "false" );
        Globales::elPuerto.escribir( "\n" );
        (*this).detenerAnuncio();
    }

    // Enciende la emisora registrando callbacks de conexion/desconexion
    void encenderEmisora( CallbackConexionEstablecida cbce,
                          CallbackConexionTerminada cbct ) {
        encenderEmisora();
        instalarCallbackConexionEstablecida( cbce );
        instalarCallbackConexionTerminada( cbct );
    }

    // Detiene la transmision de anuncios si esta actualmente activa
    void detenerAnuncio() {
        if ( (*this).estaAnunciando() ) {
            Bluefruit.Advertising.stop();
        }
    }

    // Consulta si la radio esta transmitiendo publicidad
    bool estaAnunciando() {
        return Bluefruit.Advertising.isRunning();
    }

    // --------------------------------------------------------------------------
    // emitirAnuncioIBeacon(): Configura y emite la trama iBeacon estandar
    // Campos transmitidos (PDF Seccion 5 y 10):
    //   - UUID: 16 bytes fijos
    //   - Major: 2 bytes (Tipo de medida + Contador)
    //   - Minor: 2 bytes (Dato que enviamos a dedo)
    //   - RSSI: 1 byte (Potencia de calibracion a 1m)
    // --------------------------------------------------------------------------
    void emitirAnuncioIBeacon( uint8_t * beaconUUID, int16_t major, int16_t minor, int8_t rssi ) {
        // 1. Detener anuncio previo para no solapar paquetes
        (*this).detenerAnuncio();

        // 2. Instanciar el objeto beacon con formato Apple
        BLEBeacon elBeacon( beaconUUID, major, minor, rssi );
        elBeacon.setManufacturer( (*this).fabricanteID );

        // 3. Configurar potencia y nombre visible en el escaneo
        Bluefruit.setTxPower( (*this).txPower );
        Bluefruit.setName( (*this).nombreEmisora );

        // 3bis. Limpiar anuncios previos para poder repetir el anuncio en cada vuelta
        Bluefruit.Advertising.clearData();
        Bluefruit.ScanResponse.clearData();
        Bluefruit.ScanResponse.addName();

        // 4. Cargar el beacon en la carga util del anuncio
        Bluefruit.Advertising.setBeacon( elBeacon );
        Bluefruit.Advertising.restartOnDisconnect(true);
        Bluefruit.Advertising.setInterval(100, 100); // Intervalo de 62.5 ms

        // 5. Iniciar la emision continua sin tiempo limite (0 = indefinido)
        bool okStart = Bluefruit.Advertising.start( 0 );

        // --- diagnostico: comprobar si start() arranca de verdad ---
        Globales::elPuerto.escribir( "diagnostico: start() = " );
        Globales::elPuerto.escribir( okStart ? "true" : "false" );
        Globales::elPuerto.escribir( "\n" );
    }

    // Emite un iBeacon con una carga de bytes personalizada (para pruebas libres)
    void emitirAnuncioIBeaconLibre( const char * carga, const uint8_t tamanyoCarga ) {
        (*this).detenerAnuncio();

        Bluefruit.Advertising.clearData();
        Bluefruit.ScanResponse.clearData();

        Bluefruit.setName( (*this).nombreEmisora );
        Bluefruit.ScanResponse.addName();

        Bluefruit.Advertising.addFlags(BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE);

        uint8_t restoPrefijoYCarga[4+21] = {
                0x4c, 0x00,
                0x02,
                21,
                '-', '-', '-', '-',
                '-', '-', '-', '-',
                '-', '-', '-', '-',
                '-', '-', '-', '-',
                '-', '-', '-', '-',
                '-'
        };

        memcpy( &restoPrefijoYCarga[4], &carga[0], ( tamanyoCarga > 21 ? 21 : tamanyoCarga ) );

        Bluefruit.Advertising.addData( BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA,
                                       &restoPrefijoYCarga[0],
                                       4+21 );

        Bluefruit.Advertising.restartOnDisconnect(true);
        Bluefruit.Advertising.setInterval(100, 100);
        Bluefruit.Advertising.setFastTimeout( 1 );

        Bluefruit.Advertising.start( 0 );

        Globales::elPuerto.escribir( "emitiriBeacon libre  Bluefruit.Advertising.start( 0 );  \n");
    }

    // Metodos de gestion de servicios GATT (no utilizados en el flujo iBeacon)
    bool anyadirServicio( ServicioEnEmisora & servicio ) {
        Globales::elPuerto.escribir( " Bluefruit.Advertising.addService( servicio ); \n");
        bool r = Bluefruit.Advertising.addService( servicio );
        if ( ! r ) {
            Serial.println( " SERVICION NO ANADIDO \n");
        }
        return r;
    }

    bool anyadirServicioConSusCaracteristicas( ServicioEnEmisora & servicio ) {
        return (*this).anyadirServicio( servicio );
    }

    template <typename ... T>
    bool anyadirServicioConSusCaracteristicas( ServicioEnEmisora & servicio,
                                               ServicioEnEmisora::Caracteristica & caracteristica,
                                               T& ... restoCaracteristicas) {
        servicio.anyadirCaracteristica( caracteristica );
        return anyadirServicioConSusCaracteristicas( servicio, restoCaracteristicas... );
    }

    template <typename ... T>
    bool anyadirServicioConSusCaracteristicasYActivar( ServicioEnEmisora & servicio,
                                                       T& ... restoCaracteristicas) {
        bool r = anyadirServicioConSusCaracteristicas( servicio, restoCaracteristicas... );
        servicio.activarServicio();
        return r;
    }

    void instalarCallbackConexionEstablecida( CallbackConexionEstablecida cb ) {
        Bluefruit.Periph.setConnectCallback( cb );
    }

    void instalarCallbackConexionTerminada( CallbackConexionTerminada cb ) {
        Bluefruit.Periph.setDisconnectCallback( cb );
    }

    BLEConnection * getConexion( uint16_t connHandle ) {
        return Bluefruit.Connection( connHandle );
    }

};

#endif