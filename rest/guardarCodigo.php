<?php

//---------------------------------------------------------
//---------------------------------------------------------
//
// POST ../rest/guardarCodigo.php
// cuerpo JSON: {"valor":Entero}
//
// valor:Entero -> guardarCodigo() -> bool | error:Texto
//
// @return
//  resultado:bool
//  error:Texto   (solo cuando resultado = false)
//
//---------------------------------------------------------
//---------------------------------------------------------

require_once( __DIR__ . "/../logica/guardarCodigo.php" );

$objetoResultado = new stdClass;

// recojo el cuerpo de la peticion y lo interpreto como JSON
$cuerpo = file_get_contents( "php://input" );
$datos = json_decode( $cuerpo );

// si el cuerpo no es un JSON con el campo valor, se rechaza aqui mismo
if ( ! is_object( $datos ) || ! isset( $datos->valor ) ) {
  $objetoResultado->resultado = false;
  $objetoResultado->error = "cuerpo mal formado";
  echo json_encode( $objetoResultado );
  return;
}

// json_decode convierte los enteros del JSON en enteros de PHP,
// con lo que la logica de negocio puede aplicar su regla de validacion
$guardado = guardarCodigo( $datos->valor );

// la logica decide si el codigo es valido y lo almacena
if ( $guardado ) {
  $objetoResultado->resultado = true;
} else {
  $objetoResultado->resultado = false;
  $objetoResultado->error = "código inválido";
}

echo json_encode( $objetoResultado );
?>
