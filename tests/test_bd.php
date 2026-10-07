<?php

//---------------------------------------------------------
//---------------------------------------------------------
//
// tests de la base de datos bd_codigos: creacion, insercion,
// consulta del ultimo codigo y restricciones
//
// se conecta a MySQL (localhost, root, sin contrasena)
//
//---------------------------------------------------------
//---------------------------------------------------------

$SERVIDOR = "localhost";
$USUARIO  = "root";
$CONTRASENA = "";

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
// bool, Texto -> seRechaza() -> bool
//
// comprueba que una insercion invalida falla en la BD
//
//---------------------------------------------------------
function seRechaza( $conexion, $sql, $parametros ) {
  try {
    $consulta = $conexion->prepare( $sql );
    $resultado = $consulta->execute( $parametros );
    return $resultado === false;
  } catch ( PDOException $e ) {
    return true;
  }
}

//---------------------------------------------------------
//
// Texto -> ejecutarScript() -> bool
//
// ejecuta las sentencias del fichero SQL de creacion
//
//---------------------------------------------------------
function ejecutarScript( $conexion, $fichero ) {
  $contenido = file_get_contents( $fichero );
  if ( $contenido === false ) {
    return false;
  }
  $lineas = array();
  foreach ( explode( "\n", $contenido ) as $linea ) {
    if ( substr( trim( $linea ), 0, 2 ) !== "--" ) {
      $lineas[] = $linea;
    }
  }
  $sentencias = explode( ";", implode( "\n", $lineas ) );
  foreach ( $sentencias as $sentencia ) {
    if ( trim( $sentencia ) !== "" ) {
      $conexion->exec( $sentencia );
    }
  }
  return true;
}

//---------------------------------------------------------
//
// tests
//
//---------------------------------------------------------

// conexion inicial sin base de datos seleccionada
try {
  $conexion = new PDO( "mysql:host=" . $SERVIDOR, $USUARIO, $CONTRASENA );
  $conexion->setAttribute( PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION );
} catch ( PDOException $e ) {
  echo "FALLO: conexion a MySQL - no se pudo conectar al servidor\n";
  echo "0/" . $total . " tests correctos\n";
  exit( 1 );
}

// la prueba parte siempre del mismo estado: borra la BD si ya existia
$conexion->exec( "DROP DATABASE IF EXISTS bd_codigos" );

// 1- creacion de la tabla
$scriptCreado = ejecutarScript( $conexion, __DIR__ . "/../bd/crear_bd.sql" );
$existeTabla = false;
if ( $scriptCreado ) {
  $consulta = $conexion->prepare(
    "SELECT COUNT(*) FROM information_schema.tables
     WHERE table_schema = 'bd_codigos' AND table_name = 'CODIGO'"
  );
  $consulta->execute();
  $existeTabla = $consulta->fetchColumn() == 1;
}
comprobar( $existeTabla, "creacion de la tabla CODIGO", "el script no creo la tabla" );

$conexion->exec( "USE bd_codigos" );

// 2- insercion de un codigo
$idInsertado = 0;
$consulta = $conexion->prepare(
  "INSERT INTO CODIGO ( valor, fecha ) VALUES ( 1234, '2026-01-01 10:00:00' )"
);
if ( $consulta->execute() ) {
  $idInsertado = (int) $conexion->lastInsertId();
}
$fila = null;
if ( $idInsertado > 0 ) {
  $consulta = $conexion->prepare( "SELECT valor, fecha FROM CODIGO WHERE id = ?" );
  $consulta->execute( array( $idInsertado ) );
  $fila = $consulta->fetch( PDO::FETCH_ASSOC );
}
comprobar(
  $idInsertado > 0 && $fila !== false
    && (int) $fila["valor"] === 1234 && $fila["fecha"] === "2026-01-01 10:00:00",
  "insercion de un codigo",
  "la fila no se guardo igual que la enviada"
);

// 3- consulta: debe devolver el ultimo codigo guardado
$conexion->prepare( "INSERT INTO CODIGO ( valor, fecha ) VALUES ( 111, '2026-01-01 11:00:00' )" )->execute();
$conexion->prepare( "INSERT INTO CODIGO ( valor, fecha ) VALUES ( 222, '2026-01-01 12:00:00' )" )->execute();
$consulta = $conexion->query( "SELECT valor FROM CODIGO ORDER BY id DESC LIMIT 1" );
$ultimo = $consulta->fetchColumn();
comprobar( (int) $ultimo === 222, "consulta del ultimo guardado", "devolvio " . var_export( $ultimo, true ) );

// 4- restricciones: cada caso invalido debe ser rechazado por la BD
comprobar(
  seRechaza( $conexion, "INSERT INTO CODIGO ( valor, fecha ) VALUES ( NULL, '2026-01-01 10:00:00' )", array() ),
  "restricciones: valor NULL rechazado",
  "la BD acepto valor NULL"
);
comprobar(
  seRechaza( $conexion, "INSERT INTO CODIGO ( valor, fecha ) VALUES ( 1234, NULL )", array() ),
  "restricciones: fecha NULL rechazado",
  "la BD acepto fecha NULL"
);
comprobar(
  seRechaza( $conexion, "INSERT INTO CODIGO ( valor, fecha ) VALUES ( 65536, '2026-01-01 10:00:00' )", array() ),
  "restricciones: valor 65536 fuera de rango rechazado",
  "la BD acepto un valor fuera de rango"
);
comprobar(
  seRechaza( $conexion, "INSERT INTO CODIGO ( valor, fecha ) VALUES ( -1, '2026-01-01 10:00:00' )", array() ),
  "restricciones: valor -1 fuera de rango rechazado",
  "la BD acepto un valor fuera de rango"
);

// recuento final y codigo de salida
echo $aciertos . "/" . $total . " tests correctos\n";
exit( ( $aciertos === $total ) ? 0 : 1 );
