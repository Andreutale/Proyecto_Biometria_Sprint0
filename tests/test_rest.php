<?php

//---------------------------------------------------------
//---------------------------------------------------------
//
// tests del servidor REST: guardar por POST (válido, fuera de
// rango y cuerpo mal formado) y consultar por GET (con código
// y con la BD vacía)
//
// requiere el servidor arrancado en otra terminal:
//   php -S localhost:8080 -t .
//
// borra sus datos al empezar para poder repetirse
//
//---------------------------------------------------------
//---------------------------------------------------------

require_once( __DIR__ . "/../logica/config.php" );

$aciertos = 0;
$total = 0;

//---------------------------------------------------------
//
// bool, Texto, Texto -> comprobar() -> void
//
// imprime OK:/FALLO: de cada test y lleva el recuento
//
//---------------------------------------------------------
function comprobar( $cond, $nombre, $motivo = "" ) {
  global $aciertos, $total;
  $total++;
  if ( $cond ) {
    $aciertos++;
    echo "OK: " . $nombre . "\n";
  } else {
    echo "FALLO: " . $nombre . " - " . $motivo . "\n";
  }
}

//---------------------------------------------------------
//
// Texto, Texto|null -> pedir() -> objeto | null
//
// hace una petición HTTP real al servidor y devuelve el
// JSON de respuesta interpretado, o null si no hubo respuesta
//
//---------------------------------------------------------
function pedir( $ruta, $cuerpo = null ) {
  $opciones = array(
    "http" => array(
      "method"  => "GET",
      "header"  => "Content-Type: application/json\r\n",
      "timeout" => 5
    )
  );
  if ( $cuerpo !== null ) {
    $opciones["http"]["method"] = "POST";
    $opciones["http"]["content"] = $cuerpo;
  }
  $contexto = stream_context_create( $opciones );
  $respuesta = @file_get_contents( "http://localhost:8080" . $ruta, false, $contexto );
  if ( $respuesta === false ) {
    return null;
  }
  $resultado = json_decode( $respuesta );
  return is_object( $resultado ) ? $resultado : null;
}

//---------------------------------------------------------
//
// tests
//
//---------------------------------------------------------

// estado inicial reproducible: sin filas
try {
  $conexion = conexionBD();
  $conexion->exec( "DELETE FROM CODIGO" );
} catch ( PDOException $e ) {
  echo "FALLO: conexion a MySQL - no se pudo conectar al servidor\n";
  echo "0/" . $total . " tests correctos\n";
  exit( 1 );
}

// 1- guardar por POST con un valor válido
$respuesta = pedir( "/rest/guardarCodigo.php", json_encode( array( "valor" => 1234 ) ) );
comprobar(
  $respuesta !== null && isset( $respuesta->resultado ) && $respuesta->resultado === true,
  "POST con valor valido 1234 devuelve resultado:true",
  "respuesta: " . var_export( $respuesta, true )
);

// 2- guardar por POST con un valor fuera de rango
$respuesta = pedir( "/rest/guardarCodigo.php", json_encode( array( "valor" => 65536 ) ) );
comprobar(
  $respuesta !== null && isset( $respuesta->resultado ) && $respuesta->resultado === false,
  "POST con valor fuera de rango 65536 devuelve resultado:false",
  "respuesta: " . var_export( $respuesta, true )
);

// 3- guardar con el cuerpo mal formado
$respuesta = pedir( "/rest/guardarCodigo.php", "esto no es json" );
comprobar(
  $respuesta !== null && isset( $respuesta->resultado ) && $respuesta->resultado === false
    && isset( $respuesta->error ),
  "POST con cuerpo mal formado devuelve resultado:false con error",
  "respuesta: " . var_export( $respuesta, true )
);

// 4- consultar: devuelve valor, fecha y error
$respuesta = pedir( "/rest/consultarCodigo.php" );
comprobar(
  $respuesta !== null && isset( $respuesta->valor ) && (int) $respuesta->valor === 1234
    && isset( $respuesta->fecha ) && $respuesta->fecha !== ""
    && isset( $respuesta->error ) && $respuesta->error === 0,
  "GET devuelve el valor guardado, la fecha y error:0",
  "respuesta: " . var_export( $respuesta, true )
);

// 5- consultar con la BD vacía -> error de texto
$conexion->exec( "DELETE FROM CODIGO" );
$respuesta = pedir( "/rest/consultarCodigo.php" );
comprobar(
  $respuesta !== null && isset( $respuesta->error )
    && $respuesta->error === "no hay códigos guardados",
  "GET con la BD vacia devuelve el error de texto",
  "respuesta: " . var_export( $respuesta, true )
);

// recuento final y codigo de salida
echo $aciertos . "/" . $total . " tests correctos\n";
exit( ( $aciertos === $total ) ? 0 : 1 );
