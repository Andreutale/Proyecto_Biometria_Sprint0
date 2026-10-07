<?php

//---------------------------------------------------------
//---------------------------------------------------------
//
// guardarCodigo.php
//
// valida el codigo recibido del movil y lo guarda en la tabla CODIGO
//
//---------------------------------------------------------
//---------------------------------------------------------

require_once( __DIR__ . "/config.php" );

//---------------------------------------------------------
//
// valor:Entero -> guardarCodigo() -> bool
//
// solo acepta enteros reales entre 0 y 65535; la fecha la fija el servidor
//
//---------------------------------------------------------
function guardarCodigo( $valor ) {

  // regla de negocio: tiene que ser un entero de verdad, sin excepciones
  if ( ! is_int( $valor ) ) {
    return false;
  }

  if ( $valor < 0 || $valor > 65535 ) {
    return false;
  }

  try {
    $conexion = conexionBD();
    $consulta = $conexion->prepare(
      "INSERT INTO CODIGO ( valor, fecha ) VALUES ( ?, ? )"
    );
    return $consulta->execute( array( $valor, date( "Y-m-d H:i:s" ) ) );

  } catch ( PDOException $e ) {
    return false;
  }
}
?>
