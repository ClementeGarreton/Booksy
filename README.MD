# Booksy: Biblioteca Virtual Personal

---

## El flujo del programa se conecta a la api brindada por el profesor para insertar datos e inciar sesion activa hay 5 libros disponibles en una fila deslizable y abajo 2 inputs para insertar el titulo del libro y el autor.

---

## Los datos de libros guardados persisten en el database local del emulador y estan asociados a la cuenta de quien realizo el insert, nada de datos guardados sin importar la cuenta del usuario y sus sesion activa.

---

## La arquitectura del directorio fue simplificada con un modelo minimalista pero sin perder modularidad ni escalabilidad. Se priorizo una arquitectura similar a un proyecto web simple con los screens hechos con jetpack compose como homologos a HTML y los viewmodels con la logica de eventos para su screen correspondiente siendo el homologo de javascript en un proyecto web.

---

## Se prescindio de algunos requerimientos que aumentaban la complejidad del proyecto y se adopto un modelo final de lectura y debugueo rapido.

---

## El sesion manager comprueba si hay un user id logueado para enviarte directamente a home y no a login.

---

## Main Activity esta vinculado con sesion manager y con app navigation para movilizarse entre screens al momento de un evento touch en un boton que redireccione a otra screen.

---

## Gradle y Manifest cuentan con los permisos respectivos para consumir 2 recursos nativos los cuales son en la profile screen puedes acceder a la camara o a la galeria para subir una foto a tu cuenta de usuario.

---

## Se implementaron 3 archivos nuevos llamados LoginViewModelSimpleTest, SignupViewModelSimpleTest y HomeViewModelSimpleTest. Todos en la carpeta test a un lado de la carpeta java para correr las pruebas JUnit.

---

## Se expandio la arquitectura de la app para usar retrofit y aislar los errores en modulos interdependientes.

---

## Se implemento un servidor escrito en Java y levantado con Spring Boot en la plataforma Railway y esta perfectamente operativa en un servidor, con direccion url, y siendo invocada como api y micro servicio externo a la app Booksy.

# Autor: Clemente Garreton
