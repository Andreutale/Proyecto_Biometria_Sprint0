// -*- mode: c++ -*-

#ifndef PUERTO_SERIE_H_INCLUIDO
#define PUERTO_SERIE_H_INCLUIDO
// ============================================================================
// Autor: Raúl Sanabria Rodado
//
// Fecha:1/10/26
// ============================================================================


// ============================================================================
// CLASE: PuertoSerie

// ============================================================================
// Autor: Raúl Sanabria Rodado
//
// Fecha:1/10/26
// ============================================================================

// ============================================================================
// Proposito: Envolver y gestionar la comunicacion serie USB (UART) con el PC.
// Responsabilidades (segun PDF Seccion 3):
//   - Inicializar el canal serie con una velocidad en baudios dada.
//   - Bloquear hasta que el bus USB nativo del nRF52 este sincronizado.
//   - Imprimir mensajes de depuracion de cualquier tipo generico.
// ============================================================================
class PuertoSerie  {

public:
    // Constructor: Inicia el puerto serie sin bloquear en el arranque
    PuertoSerie (long baudios) {
        Serial.begin( baudios );
    }

    // Bloquea el flujo hasta que el monitor serie o el USB nativo se reconozca
    void esperarDisponible() {
        while ( !Serial ) {
            delay(10);
        }
    }

    // Metodo generico (template) para enviar texto o numeros al terminal
    template<typename T>
    void escribir (T mensaje) {
        Serial.print( mensaje );
    }

};

#endif  