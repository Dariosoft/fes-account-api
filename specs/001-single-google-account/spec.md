# Spec 001 — Cuenta única con Google

## Contexto y objetivo
Friendly E-Shop necesita una sola cuenta, propiedad de este servicio, que sirva tanto para la tienda como para el panel. En este corte, la entrada a la tienda se resuelve solo con Google: este servicio atiende el pedir entrar y el pedir salir, y permite saber quién es la persona autenticada. El panel reutiliza esa misma cuenta, pero no inicia sesión a través de este servicio. La misma persona es quien tiene el mismo correo de Google.

## Usuarios / actores
- Persona que compra o navega en la tienda y quiere entrar o salir con Google.
- Consumidor de identidad de la tienda, que necesita saber quién es la persona cuando hay sesión.
- El panel (y su puerta de acceso), que reutiliza la misma cuenta sin autenticarse por este servicio.
- Este servicio, como único dueño de la cuenta.

## Historias de usuario
- H1: Como persona de la tienda quiero entrar con Google para que el sistema me reconozca con una sola cuenta.
- H2: Como persona de la tienda quiero salir para que deje de considerarse que estoy dentro.
- H3: Como consumidor de identidad de la tienda quiero saber quién es la persona autenticada para adaptar la experiencia a ella.
- H4: Como dueño del producto quiero una sola cuenta compartida entre tienda y panel para no duplicar identidades ni crear una cuenta aparte de vendedor.

## Requisitos funcionales (criterios de aceptación en EARS)
- RF-1: EL SISTEMA será el único dueño de la cuenta usada en la tienda y en el panel.
- RF-2: EL SISTEMA mantendrá una sola cuenta por persona para tienda y panel, sin una segunda cuenta de vendedor y sin roles de comprador o vendedor.
- RF-3: EL SISTEMA permitirá entrar a la tienda únicamente con Google en este corte, y aceptará cualquier cuenta de Google.
- RF-4: CUANDO la tienda pida entrar y Google valide a la persona, EL SISTEMA creará la cuenta si ese correo de Google no existe, dejará a esa persona dentro y la vinculará a su cuenta (salida: persona autenticada con su cuenta).
- RF-5: CUANDO la persona pide salir desde la tienda o desde el panel, EL SISTEMA dejará de considerarla dentro en ambas (salida: persona sin acceso activo).
- RF-6: MIENTRAS la persona esté dentro por la tienda, EL SISTEMA permitirá saber quién es esa persona mediante su nombre y su correo de Google.
- RF-7: SI la validación con Google falla o se rechaza, ENTONCES EL SISTEMA no dejará a la persona dentro.
- RF-8: SI no hay persona dentro por la tienda, ENTONCES EL SISTEMA no presentará a nadie como autenticado.
- RF-9: SI una persona ya tiene cuenta y vuelve a entrar con el mismo correo de Google, ENTONCES EL SISTEMA reutilizará esa cuenta y no creará otra.
- RF-10: EL SISTEMA no resolverá la entrada del panel; el panel usará la misma cuenta sin entrar por este servicio.
- RF-11: CUANDO el panel pregunte por una persona ya conocida por su correo de Google, EL SISTEMA confirmará que es la misma cuenta y dirá su nombre y su correo, sin que el panel entre por la tienda.
- RF-12: EL SISTEMA mantendrá una sola sesión para tienda y panel. Entrar en cualquiera de las dos abre esa sesión. Salir en cualquiera de las dos la cierra en ambas.
- RF-13: CUANDO la tienda o el panel recarguen o envíen una solicitud después de un salir, EL SISTEMA los tratará como sin acceso activo.

## Requisitos no funcionales
- Los mensajes visibles a la persona estarán en español.
- No se almacenarán ni expondrán secretos de autenticación en claro.
- La persona sigue dentro hasta que sale. No hay un plazo tras el cual deje de considerarse autenticada. Salir en la tienda o en el panel cierra la misma sesión.

## Casos límite
- Primer ingreso con Google de una persona nunca vista: el sistema crea la cuenta con ese correo y la deja dentro (RF-4).
- Reintento de entrada con el mismo correo de Google cuando ya existe cuenta: se reutiliza la cuenta (RF-9); no se duplica.
- Entrada con Google inválida, cancelada o rechazada: no se deja a la persona dentro (RF-7).
- Pedido de salida sin persona dentro: el resultado permanece sin persona autenticada (alineado con RF-8).
- Consulta de “quién es” sin persona dentro: no se presenta a nadie como autenticado (RF-8).
- Consulta de “quién es” con persona dentro: se informa el nombre y el correo de Google (RF-6).
- Intento de tratar la entrada del panel como responsabilidad de este servicio: fuera de este corte (RF-10 y fuera de alcance). El reconocimiento de la misma persona hacia el panel es solo por el mismo correo de Google (RF-11).
- La cuenta no distingue comprador y vendedor: es solo identidad (RF-2).

## Fuera de alcance
- Pantallas de la tienda (pertenecen a fes-client-web).
- Puerta de acceso / entrada del panel (pertenece a fes-panel-api).
- Pantallas del panel (pertenecen a fes-panel-web).
- Dejar este servicio disponible junto al resto de la plataforma (pertenece a fes-infra).
- Crear una segunda cuenta para el vendedor.
- Roles o distinciones de acceso dentro de la cuenta.
- Publicar un catálogo.
- Empezar una compra.
- Otros métodos de entrada distintos de Google (correo y contraseña, redes distintas, etc.).
- Caducidad automática de la permanencia dentro de la tienda.
- Gestión avanzada de cuenta más allá de lo necesario para entrar, salir y saber quién es (p. ej. borrado de cuenta, fusión de identidades, cambio de proveedor).

## Criterios de finalización
- Todos los RF tienen prueba automatizada en verde.
- Demostración manual del flujo principal de la tienda: entrar con Google, saber el nombre y el correo, y salir.
- Queda comprobado que el primer ingreso crea la cuenta, que el mismo correo no duplica la cuenta, que no hay roles ni una segunda cuenta de vendedor, y que la entrada del panel no se resuelve en este servicio.

## Dudas abiertas
No quedan dudas abiertas.
