<?php

//---------------------------------------------------------
//---------------------------------------------------------
//
// tests de la logica de negocio: validacion, almacenamiento,
// consulta del ultimo codigo y registro de la fecha
//
// borra sus datos al empezar para poder repetirse
//
//---------------------------------------------------------
//---------------------------------------------------------

require_once( __DIR__ . "/../logica/guardarCodigo.php" );
require_once( __DIR__ . "/../logica/consultarUltimoCodigo.php" );

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
// tests
//
//---------------------------------------------------------

try {
  $conexion = conexionBD();
} catch ( PDOException $e ) {
  echo "FALLO: conexion a MySQL - no se pudo conectar al servidor\n";
  echo "0/" . $total . " tests correctos\n";
  exit( 1 );
}

// estado inicial reproducible: sin filas
$conexion->exec( "DELETE FROM CODIGO" );

// 1- recepcion de un codigo valido
$guardado = guardarCodigo( 1234 );
comprobar( $guardado === true, "codigo valido 1234 devuelvo true", "devolvio false" );

// 2- rechazo de codigos invalidos, sin insertar ninguna fila
$filasAntes = (int) $conexion->query( "SELECT COUNT(*) FROM CODIGO" )->fetchColumn();
$invalidos = array( -1, 65536, "texto", "" );
$rechazosOk = true;
foreach ( $invalidos as $invalido ) {
  if ( guardarCodigo( $invalido ) !== false ) {
    $rechazosOk = false;
  }
}
$filasDespues = (int) $conexion->query( "SELECT COUNT(*) FROM CODIGO" )->fetchColumn();
comprobar(
  $rechazosOk && $filasAntes === $filasDespues,
  "codigos invalidos rechazados sin insertar filas",
  "alguno no se rechazo o se inserto una fila"
);

// 3- almacenamiento correcto: tras guardar, la fila aparece en la tabla
$conexion->exec( "DELETE FROM CODIGO" );
guardarCodigo( 4321 );
$consulta = $conexion->query( "SELECT valor FROM CODIGO ORDER BY id DESC LIMIT 1" );
$ultimo = $consulta->fetchColumn();
comprobar(
  (int) $ultimo === 4321,
  "codigo 4321 almacenado y recuperable de la tabla",
  "devolvio " . var_export( $ultimo, true )
);

// 4- consulta correcta: guardo 111 y luego 222, debe devolver 222
$conexion->exec( "DELETE FROM CODIGO" );
guardarCodigo( 111 );
guardarCodigo( 222 );
$resultado = consultarUltimoCodigo();
comprobar(
  isset( $resultado->valor ) && (int) $resultado->valor === 222,
  "la consulta devuelve el ultimo codigo guardado (222)",
  "devolvio " . ( isset( $resultado->valor ) ? $resultado->valor : $resultado->error )
);

// 5- registro de la fecha de recepcion: formato correcto y cercana a ahora
$fecha = isset( $resultado->fecha ) ? $resultado->fecha : "";
$formatoOk = preg_match( "/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/", $fecha ) === 1;
$momento = $formatoOk ? strtotime( $fecha ) : false;
$cercanaOk = $momento !== false && abs( time() - $momento ) <= 10;
comprobar(
  $formatoOk && $cercanaOk,
  "la fecha de recepcion es valida y cercana a la hora actual",
  "fecha devuelta: " . var_export( $fecha, true )
);

// consulta con la BD vacia -> error de texto
$conexion->exec( "DELETE FROM CODIGO" );
$vacia = consultarUltimoCodigo();
comprobar(
  isset( $vacia->error ) && $vacia->error === "no hay códigos guardados",
  "consulta con la BD vacia devuelve el error de texto",
  "devolvio " . var_export( isset( $vacia->error ) ? $vacia->error : $vacia, true )
);

// recuento final y codigo de salida
echo $aciertos . "/" . $total . " tests correctos\n";
exit( ( $aciertos === $total ) ? 0 : 1 );
