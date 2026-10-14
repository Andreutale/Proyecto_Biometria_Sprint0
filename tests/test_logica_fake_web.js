// ---------------------------------------------------
//
// tests de la logica fake del navegador: obtencion de
// codigo, ausencia de codigo y simulacion de error
//
// sustituye XMLHttpRequest por un doble con respuestas
// preparadas, asi no depende de la red ni del servidor
//
// se ejecuta con: node tests/test_logica_fake_web.js
//
// ---------------------------------------------------

const fs = require( "fs" )
const path = require( "path" )

let aciertos = 0
let total = 0

//---------------------------------------------------------
//
// bool, Texto, Texto -> comprobar() -> void
//
// imprime OK:/FALLO: de cada test y lleva el recuento
//
//---------------------------------------------------------
function comprobar( cond, nombre, motivo ) {
  total++
  if ( cond ) {
    aciertos++
    console.log( "OK: " + nombre )
  } else {
    console.log( "FALLO: " + nombre + " - " + motivo )
  }
}

// carga el script clasico de navegador (no es un modulo)
const fuente = fs.readFileSync(
  path.join( __dirname, "..", "ux", "logicaFake", "consultarUltimoCodigo.js" ),
  "utf8"
)
eval( fuente )

//---------------------------------------------------------
//
// doble del XHR: la respuesta sale preparada y se lanza
// de forma sincrona al llamar a send()
//
//---------------------------------------------------------
function crearXHRDoble( status, cuerpo ) {
  return {
    readyState: 0,
    status: 0,
    responseText: "",
    metodo: null,
    url: null,
    onreadystatechange: null,
    open: function( metodo, url ) {
      this.metodo = metodo
      this.url = url
    },
    send: function() {
      this.readyState = 4
      this.status = status
      this.responseText = cuerpo
      this.onreadystatechange()
    }
  }
}

//---------------------------------------------------------
//
// tests
//
//---------------------------------------------------------

// 1- obtencion de codigo: el servidor responde con el valor
let error1 = "sin llamar"
let resultado1 = null
crearXHR = function() {
  return crearXHRDoble(
    200,
    JSON.stringify( { valor: 1234, fecha: "2026-01-01 10:00:00", error: 0 } )
  )
}
consultarUltimoCodigo( function( err, res ) {
  error1 = err
  resultado1 = res
} )
comprobar(
  error1 === null && resultado1 !== null && resultado1.valor === 1234,
  "obtencion de codigo: cb(null, res) con valor 1234",
  "err=" + error1 + " res=" + JSON.stringify( resultado1 )
)

// 2- ausencia de codigo: el servidor devuelve su texto de error
let error2 = "sin llamar"
let resultado2 = "sin llamar"
crearXHR = function() {
  return crearXHRDoble( 200, JSON.stringify( { error: "no hay códigos guardados" } ) )
}
consultarUltimoCodigo( function( err, res ) {
  error2 = err
  resultado2 = res
} )
comprobar(
  error2 === "no hay códigos guardados" && resultado2 === null,
  "ausencia de codigo: cb(err, null) con el texto del servidor",
  "err=" + error2 + " res=" + JSON.stringify( resultado2 )
)

// 3- simulacion de error: fallo HTTP y cuerpo que no es JSON
let errorHttp = "sin llamar"
let resultadoHttp = "sin llamar"
crearXHR = function() {
  return crearXHRDoble( 500, "" )
}
consultarUltimoCodigo( function( err, res ) {
  errorHttp = err
  resultadoHttp = res
} )

let errorJson = "sin llamar"
let resultadoJson = "sin llamar"
crearXHR = function() {
  return crearXHRDoble( 200, "esto no es json" )
}
consultarUltimoCodigo( function( err, res ) {
  errorJson = err
  resultadoJson = res
} )
comprobar(
  typeof errorHttp === "string" && errorHttp !== "" && resultadoHttp === null
    && typeof errorJson === "string" && errorJson !== "" && resultadoJson === null,
  "simulacion de error: fallo HTTP y JSON invalido devuelven cb(err, null)",
  "http=" + errorHttp + "/" + JSON.stringify( resultadoHttp )
    + " json=" + errorJson + "/" + JSON.stringify( resultadoJson )
)

// recuento final y codigo de salida
console.log( aciertos + "/" + total + " tests correctos" )
process.exit( ( aciertos === total ) ? 0 : 1 )
