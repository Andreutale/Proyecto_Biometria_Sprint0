<?php

//---------------------------------------------------------
//---------------------------------------------------------
//
// consultarUltimoCodigo.php
//
// consulta el ultimo codigo guardado en la tabla CODIGO
//
//---------------------------------------------------------
//---------------------------------------------------------

require_once( __DIR__ . "/config.php" );

//---------------------------------------------------------
//
// -> consultarUltimoCodigo() -> (valor:Entero, fecha:Fecha) | error:Texto
//
// devuelve el valor y la fecha de la fila de mayor id; texto de error si esta vacia
//
//---------------------------------------------------------
function consultarUltimoCodigo() {

  try {
    $conexion = conexionBD();
    $consulta = $conexion->query(
      "SELECT valor, fecha FROM CODIGO ORDER BY id DESC LIMIT 1"
    );
    $fila = $consulta->fetch( PDO::FETCH_OBJ );

    if ( $fila === false ) {
      return (object) array( "error" => "no hay códigos guardados" );
    }

    return (object) array( "valor" => (int) $fila->valor, "fecha" => $fila->fecha );

  } catch ( PDOException $e ) {
    return (object) array( "error" => "no hay códigos guardados" );
  }
}
?>
