// -*- mode: c++ -*-

#ifndef LED_H_INCLUIDO
#define LED_H_INCLUIDO


// ============================================================================
// Autor: Andreu Aliaga Machí
// ============================================================================

// ============================================================================
// FUNCION GLOBAL: esperar()
// Proposito: Abstraer el bloqueo temporal delay() de Arduino para su uso
//            en cualquier punto del proyecto.
// ============================================================================
void esperar (long tiempo) {
    delay (tiempo);
}

// ============================================================================
// CLASE: LED
// Proposito: Controlar el estado de un pin digital conectado a un diodo LED.
// Responsabilidades (segun PDF Seccion 3):
//   - Mantener el pin asignado y su estado logico (encendido / apagado).
//   - Ofrecer metodos para encender, apagar, alternar y generar destellos.
// ============================================================================
class LED {
private:
    int numeroLED;    // Pin fisico de la placa conectado al anodo del LED
    bool encendido;   // Estado logico actual (true = HIGH / false = LOW)

public:
    // Constructor: Configura el pin digital como salida y lo apaga por defecto
    LED (int numero)
            : numeroLED (numero), encendido(false)
    {
        pinMode(numeroLED, OUTPUT);
        apagar ();
    }

    // Pone el pin en nivel ALTO (3.3V / 5V) para encender el LED
    void encender () {
        digitalWrite(numeroLED, HIGH);
        encendido = true;
    }

    // Pone el pin en nivel BAJO (0V) para apagar el LED
    void apagar () {
        digitalWrite(numeroLED, LOW);
        encendido = false;
    }

    // Invierte el estado actual del LED (si estaba ON pasa a OFF y viceversa)
    void alternar () {
        if (encendido) {
            apagar();
        } else {
            encender ();
        }
    }

    // Enciende el LED, bloquea el procesador durante 'tiempo' ms y lo apaga
    void brillar (long tiempo) {
        encender ();
        esperar(tiempo);
        apagar ();
    }
};

#endif