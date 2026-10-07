<?php

//---------------------------------------------------------
//---------------------------------------------------------
//
// config.php
//
// parametros de conexion a la base de datos, definidos una sola vez
//
//---------------------------------------------------------
//---------------------------------------------------------

define( "BD_SERVIDOR", "localhost" );
define( "BD_USUARIO", "root" );
define( "BD_CONTRASENA", "" );
define( "BD_NOMBRE", "bd_codigos" );

// zona horaria fija para que la fecha de recepcion sea estable
date_default_timezone_set( "Europe/Madrid" );

//---------------------------------------------------------
//
// -> conexionBD() -> conexion:Conexion | error:Texto
//
// devuelve la conexion PDO a la base de datos bd_codigos
//
//---------------------------------------------------------
function conexionBD() {
  return new PDO(
    "mysql:host=" . BD_SERVIDOR . ";dbname=" . BD_NOMBRE,
    BD_USUARIO,
    BD_CONTRASENA,
    array( PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION )
  );
}
?>
