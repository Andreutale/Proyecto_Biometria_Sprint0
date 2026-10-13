<?php

//---------------------------------------------------------
//---------------------------------------------------------
//
// GET ../rest/consultarCodigo.php
// (sin parámetros)
//
// -> consultarUltimoCodigo() -> (valor:Entero, fecha:Fecha) | error:Texto
//
// @return
//  valor:Entero
//  fecha:Fecha
//  error:0      (solo cuando hay código guardado)
//  error:Texto  (solo cuando no hay códigos guardados)
//
//---------------------------------------------------------
//---------------------------------------------------------

require_once( __DIR__ . "/../logica/consultarUltimoCodigo.php" );

// llamo a la verdadera función de la lógica de negocio
$resultado = consultarUltimoCodigo();

$objetoResultado = new stdClass;

// traduzco el resultado de la lógica al formato de respuesta del REST
if ( isset( $resultado->error ) ) {
  $objetoResultado->error = $resultado->error;
} else {
  $objetoResultado->valor = $resultado->valor;
  $objetoResultado->fecha = $resultado->fecha;
  $objetoResultado->error = 0;
}

echo json_encode( $objetoResultado );
?>
