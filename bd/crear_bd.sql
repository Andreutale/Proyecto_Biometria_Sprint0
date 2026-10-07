-- ---------------------------------------------------------
-- ---------------------------------------------------------
--
-- id:Entero, valor:Entero, fecha:Fecha -> tabla CODIGO() -> bool
--
-- crea la base de datos bd_codigos y la tabla CODIGO con sus restricciones
--
-- ---------------------------------------------------------
-- ---------------------------------------------------------

-- base de datos donde el servidor guarda los codigos recibidos
CREATE DATABASE IF NOT EXISTS bd_codigos;

USE bd_codigos;

-- tabla CODIGO: cada envio del movil es una fila nueva (sin borrar ni actualizar)
CREATE TABLE IF NOT EXISTS CODIGO (
  id     INT      NOT NULL AUTO_INCREMENT,
  valor  INT      NOT NULL,
  fecha  DATETIME NOT NULL,
  PRIMARY KEY ( id ),
  CONSTRAINT chk_valor CHECK ( valor >= 0 AND valor <= 65535 )
);
