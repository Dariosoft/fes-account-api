# Spec 001 — Sesión Google compartida

## Contexto y objetivo
Friendly E-Shop necesita una sola cuenta por persona, propiedad de este servicio, y una sesión compartida entre la tienda y el panel. En este corte, este servicio completa el login con Google: al volver crea o reutiliza esa cuenta, abre la sesión compartida y permite consultarla o cerrarla. Cualquier cuenta de Google puede entrar. Así la identidad queda centralizada aquí y el resto de la plataforma reutiliza esa misma sesión sin inventar otra cuenta.

## Usuarios / actores
- Persona que entra o sale con Google desde la tienda o, a través de la puerta del panel, desde el panel.
- Consumidor de identidad (tienda u otros servicios) que necesita saber si hay sesión y quién es la persona.
- Quien provisiona el cliente OAuth de Google en el entorno (persona operadora), según el procedimiento documentado.
- Este servicio, como único dueño de la cuenta y de la sesión compartida.

## Historias de usuario
- H1: Como persona quiero entrar con Google para que el sistema me reconozca con una sola cuenta y deje abierta la sesión compartida.
- H2: Como persona quiero consultar si estoy dentro y quién soy para adaptar la experiencia a mí.
- H3: Como persona quiero salir para que la sesión compartida deje de considerarse activa.
- H4: Como dueño del producto quiero una sola cuenta y una sola sesión compartida entre tienda y panel para no duplicar identidades.

## Requisitos funcionales (criterios de aceptación en EARS)
- RF-1: EL SISTEMA será el único dueño de la cuenta de la persona usada en la tienda y en el panel.
- RF-2: EL SISTEMA mantendrá una sola cuenta por persona identificada por su cuenta de Google, sin una segunda cuenta aparte.
- RF-3: EL SISTEMA aceptará la entrada de cualquier cuenta de Google.
- RF-4: CUANDO se solicite iniciar el login con Google con un destino de retorno que sea un origen permitido de los orígenes de navegador configurados, EL SISTEMA redirigirá a Google para autenticar (salida: redirección a Google).
- RF-5: SI el destino de retorno falta o no es un origen permitido de los orígenes de navegador configurados, ENTONCES EL SISTEMA responderá 400 y no redirigirá a Google.
- RF-6: CUANDO Google valide a la persona y regrese al retorno del login, EL SISTEMA creará la cuenta si esa persona de Google aún no existe (salida: cuenta nueva asociada a esa persona de Google).
- RF-7: CUANDO Google valide a la persona y regrese al retorno del login y esa persona de Google ya tenga cuenta, EL SISTEMA reutilizará esa cuenta sin crear otra (salida: misma cuenta).
- RF-8: CUANDO Google valide a la persona y regrese al retorno del login, EL SISTEMA abrirá la sesión compartida dejando la cookie de sesión `fes_session` (salida: persona autenticada con sesión compartida).
- RF-9: CUANDO Google valide a la persona y regrese al retorno del login, EL SISTEMA redirigirá de vuelta al destino de retorno indicado al iniciar el login (salida: navegación a ese destino con la sesión ya abierta).
- RF-10: SI la validación con Google falla, se cancela o se rechaza, ENTONCES EL SISTEMA no abrirá sesión y redirigirá al destino de retorno con un indicador en la URL, sin mostrar una página de error propia.
- RF-11: CUANDO se consulte la sesión y no haya sesión activa válida, EL SISTEMA responderá que no está autenticada (salida: autenticado en falso).
- RF-12: CUANDO se consulte la sesión y haya sesión activa válida, EL SISTEMA responderá que está autenticada e incluirá el id, el correo y el nombre de la cuenta (salida: autenticado en verdadero con esos datos de cuenta).
- RF-13: CUANDO se solicite cerrar la sesión, EL SISTEMA invalidará esa sesión (salida: la sesión deja de ser válida).
- RF-14: CUANDO se solicite cerrar la sesión, EL SISTEMA borrará la cookie `fes_session` (salida: cookie eliminada en la respuesta).
- RF-15: MIENTRAS exista una sesión compartida abierta, EL SISTEMA la considerará activa tanto para quien consulta desde la tienda como para quien consulta a través de la puerta del panel.
- RF-16: EL SISTEMA expondrá el inicio de login con Google en `GET /accounts/login/google` con el parámetro de destino de retorno.
- RF-17: EL SISTEMA expondrá el retorno de Google en `GET /accounts/login/google/callback` como URI de redirección de Google.
- RF-18: EL SISTEMA expondrá la consulta de sesión en `GET /accounts/session`.
- RF-19: EL SISTEMA expondrá el cierre de sesión en `POST /accounts/logout`.
- RF-21: CUANDO se reutilice una cuenta en un nuevo login con Google y Google devuelva un nombre o un correo distintos a los guardados, EL SISTEMA actualizará el nombre y el correo de esa cuenta.
- RF-22: CUANDO una persona inicie sesión de nuevo, EL SISTEMA conservará las sesiones anteriores de esa cuenta.

## Requisitos no funcionales
- La cookie `fes_session` será HttpOnly, con SameSite=Lax, Path=/ y Domain igual al dominio de cookie de sesión configurado.
- La cookie `fes_session` llevará Secure solo cuando el entorno sea HTTPS. En Minikube (HTTP) no llevará Secure.
- El sistema requerirá la configuración de identificador y secreto de cliente de Google, dominio de cookie de sesión y orígenes de navegador permitidos.
- El identificador y el secreto de cliente de Google los crea una persona; el procedimiento está en `docs/google-oauth.md` y no van en el código de los frontends.
- El sistema permitirá CORS con credenciales únicamente para los orígenes de navegador configurados.
- Los mensajes visibles a la persona estarán en español.
- No se almacenarán ni expondrán secretos de autenticación en claro.
- La sesión caducará a los 30 días y también cuando se cierre de forma explícita.

## Casos límite
- Primer ingreso con Google de una persona nunca vista: se crea la cuenta y se abre la sesión compartida (RF-6, RF-8).
- Reintento con la misma persona de Google cuando ya existe cuenta: se reutiliza la cuenta y no se duplica (RF-7).
- Destino de retorno ausente o no permitido: respuesta 400 y sin redirección a Google (RF-5).
- Validación con Google inválida, cancelada o rechazada: no se abre sesión y se vuelve al destino de retorno con un indicador en la URL (RF-10).
- Consulta de sesión sin sesión activa: autenticado en falso (RF-11).
- Consulta de sesión con sesión activa: autenticado en verdadero con id, correo y nombre (RF-12).
- Cierre de sesión: la sesión queda inválida y la cookie se borra (RF-13, RF-14).
- Entrar en un lado y consultar en el otro con la misma cookie de dominio padre: la sesión compartida se ve activa en ambos (RF-15).
- Salir en un lado: la sesión compartida deja de ser válida también para el otro (RF-13, RF-15).
- Un nuevo login no invalida las sesiones anteriores de la misma cuenta (RF-22).

## Fuera de alcance
- Pantallas de la tienda (pertenecen a client-web).
- Puerta de login del panel (pertenece a panel-api).
- Pantallas del panel (pertenecen a panel-web).
- Publicación del catálogo.
- Inicio de la compra.
- Membresías de tienda (pertenecen a panel-api).
- Dejar este servicio publicado junto al resto de la plataforma (pertenece a infra).
- Otros métodos de entrada distintos de Google.
- Roles o distinciones de comprador y vendedor dentro de la cuenta.
- Gestión avanzada de cuenta más allá de crear o reutilizar al entrar, consultar la sesión y cerrarla (p. ej. borrado de cuenta o fusión de identidades).

## Criterios de finalización
- Todos los RF resueltos (sin marcadores de aclaración) tienen prueba automatizada en verde.
- Demostración manual del flujo principal: iniciar login con Google con un destino de retorno permitido, volver con sesión abierta, consultar id/correo/nombre, y cerrar la sesión borrando la cookie.
- Queda comprobado que el primer ingreso crea la cuenta, que la misma persona de Google no duplica la cuenta, que cualquier cuenta de Google puede entrar y que la puerta de login del panel no se implementa en este servicio.

## Dudas abiertas
Ninguna.
