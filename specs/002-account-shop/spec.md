# Spec 002 — Tienda pública del vendedor

## Contexto y objetivo
El detalle público de una publicación debe mostrar quién la vende, y en Friendly E-Shop la tienda pertenece a la cuenta de esa persona. Este servicio es el dueño de las cuentas, por lo que también es el dueño de los datos de la tienda asociada a cada una. En este corte el vendedor autenticado puede guardar el nombre y la descripción pública de su tienda, y cualquier visitante anónimo puede consultar esos datos para mostrarlos junto a la publicación. La tienda es una sola por cuenta y solo se publican datos públicos: nunca el correo ni credenciales. Crear la tienda no es requisito para publicar productos, porque la publicación pertenece a otro servicio y no se exige desde aquí.

## Usuarios / actores
- Vendedor: cuenta autenticada dueña de una tienda. En este corte no se distingue un rol de vendedor dentro de la cuenta.
- Visitante anónimo: persona sin sesión que consulta la tienda del vendedor desde el detalle público de una publicación.
- Consumidor de los datos públicos de la tienda: la tienda u otro servicio que necesita mostrar la tienda del vendedor.
- Este servicio, como único dueño de la cuenta y de los datos de su tienda.

## Historias de usuario
- H1: Como vendedor quiero guardar el nombre y la descripción de la tienda de mi cuenta para que los visitantes conozcan mi tienda en el detalle de mis publicaciones.
- H2: Como vendedor quiero que guardar mi tienda actualice su información pública aunque ya exista, para mantenerla al día sin pasos adicionales.
- H3: Como visitante anónimo quiero consultar el nombre y la descripción de la tienda de un vendedor para saber quién vende la publicación que estoy viendo.
- H4: Como dueño del producto quiero que cada cuenta tenga como máximo una tienda y que solo se expongan datos públicos para no duplicar tiendas ni filtrar información privada.
- H5: Como vendedor quiero saber si mi cuenta todavía no tiene tienda para entender qué datos públicos se muestran.

## Requisitos funcionales (criterios de aceptación en EARS)
- RF-1: EL SISTEMA permitirá que una cuenta tenga como máximo una tienda.
- RF-2: CUANDO una cuenta autenticada guarde su tienda y aún no tenga una, EL SISTEMA creará la tienda asociada a esa cuenta con el nombre y la descripción recibidos (salida: tienda creada y asociada a la cuenta).
- RF-3: CUANDO una cuenta autenticada guarde su tienda y ya tenga una, EL SISTEMA reemplazará el nombre y la descripción de la tienda existente (salida: datos de la tienda actualizados), sin crear una segunda tienda ni rechazar la operación por ya existir.
- RF-4: CUANDO una cuenta autenticada consulte su propia tienda y todavía no tenga una, EL SISTEMA informará explícitamente que la cuenta aún no tiene tienda, sin tratarlo como un error del sistema.
- RF-5: SI una cuenta autenticada intenta guardar los datos de una tienda que no le pertenece, ENTONCES EL SISTEMA rechazará la operación sin modificar esos datos.
- RF-6: SI una cuenta autenticada intenta guardar su tienda sin nombre, con un nombre compuesto solo por espacios, o con un nombre que tras recortar espacios tenga menos de 1 o más de 80 caracteres, ENTONCES EL SISTEMA rechazará la operación sin guardar cambios.
- RF-7: DONDE la tienda tenga descripción, EL SISTEMA aceptará una descripción de hasta 1000 caracteres, recortando los espacios de los extremos (salida: descripción guardada).
- RF-8: SI la descripción de la tienda supera los 1000 caracteres tras recortar espacios, ENTONCES EL SISTEMA rechazará la operación sin guardar cambios.
- RF-9: EL SISTEMA permitirá que una tienda no tenga descripción, guardando como ausente una descripción vacía o compuesta solo por espacios.
- RF-10: EL SISTEMA permitirá que dos cuentas distintas usen el mismo nombre de tienda, sin rechazar el guardado por considerarlo duplicado.
- RF-11: SI se intenta guardar la tienda sin sesión activa, ENTONCES EL SISTEMA rechazará la operación sin crear ni modificar datos.
- RF-12: CUANDO un visitante anónimo consulte la tienda de un vendedor que tiene tienda, EL SISTEMA devolverá el nombre y la descripción de esa tienda (salida: datos públicos de la tienda).
- RF-13: CUANDO un visitante anónimo consulte la tienda de un vendedor que no creó tienda, EL SISTEMA informará explícitamente que ese vendedor no tiene tienda, sin tratarlo como un error del sistema.
- RF-14: SI el identificador del vendedor consultado no corresponde a ninguna cuenta, ENTONCES EL SISTEMA responderá que no existe.
- RF-15: EL SISTEMA expondrá como datos públicos de la tienda únicamente el nombre y la descripción.
- RF-16: EL SISTEMA no expondrá el correo ni credenciales de la cuenta vendedora en la consulta pública de la tienda.

## Requisitos no funcionales
- Los mensajes visibles al vendedor estarán en español.
- La consulta pública de la tienda no requerirá una sesión activa.
- El límite de la descripción se medirá en caracteres, incluidos los espacios internos, con un máximo de 1000 tras recortar los extremos.
- El nombre de la tienda será obligatorio y tendrá entre 1 y 80 caracteres tras recortar los espacios de los extremos.
- No se almacenarán ni expondrán credenciales en los datos de la tienda.
- El nombre y la descripción de la tienda serán datos públicos; el resto de los datos de la cuenta serán privados.
- El nombre de la tienda no necesita ser único entre vendedores.

## Casos límite
- Cuenta sin tienda que guarda su tienda: se crea una sola tienda asociada (RF-2).
- Cuenta con tienda que vuelve a guardar: se reemplazan nombre y descripción sin crear una segunda tienda (RF-3).
- Cuenta sin tienda que consulta su propia tienda: se informa que aún no tiene tienda (RF-4).
- Nombre ausente, compuesto solo por espacios o fuera del rango de 1 a 80 caracteres tras recortar: se rechaza el guardado (RF-6).
- Nombre de exactamente 1 carácter y de exactamente 80 caracteres tras recortar: se acepta (RF-6).
- Descripción de exactamente 1000 caracteres: se acepta (RF-7).
- Descripción de 1001 caracteres o más: se rechaza sin guardar cambios (RF-8).
- Tienda sin descripción o con la descripción vacía o solo espacios: se guarda como ausente (RF-9).
- Guardado de una tienda ajena: se rechaza sin modificar datos (RF-5).
- Guardado sin sesión activa: se rechaza sin efectos (RF-11).
- Consulta pública de un vendedor sin tienda: se informa que ese vendedor no tiene tienda (RF-13).
- Consulta pública de un identificador que no corresponde a una cuenta: se responde que no existe (RF-14).
- Dos vendedores con el mismo nombre de tienda: se permite, sin rechazo por duplicado (RF-10).
- Cuenta sin tienda que publica productos: se permite; este servicio no exige tienda para publicar.

## Fuera de alcance
- La descripción del producto y su límite de 1000 caracteres (pertenecen a catalog-api).
- La página de detalle del storefront, su URL compartible y la navegación de vuelta al catálogo (pertenecen a client-web).
- Compra, carrito, cantidad y checkout.
- La visibilidad de las publicaciones (publicadas, no eliminadas y con stock disponible o sin límite).
- Exigir la existencia de una tienda para publicar productos.
- Puntos de venta, horarios y envíos.
- Imágenes o logo de la tienda.
- Más de una tienda por cuenta.
- Membresías de tienda.
- Roles o distinciones de comprador y vendedor dentro de la cuenta.
- Eliminar la tienda.
- Buscar o listar tiendas.
- Editar el correo o el nombre de la persona desde la tienda.

## Criterios de finalización
- Todos los RF tienen prueba automatizada en verde.
- Demostración manual: un vendedor autenticado guarda su tienda con nombre y descripción, la vuelve a guardar y comprueba que se actualiza la misma tienda, y un visitante anónimo consulta el nombre y la descripción; se comprueba que no aparece el correo de la cuenta.
- Queda comprobado que guardar la tienda dos veces no crea una segunda tienda, que solo el dueño puede modificar su tienda y que la consulta pública funciona sin sesión.
- Queda comprobado que la consulta pública de un vendedor sin tienda y la consulta de la tienda propia sin tienda informan la ausencia sin tratarla como error del sistema.

## Decisiones resueltas
- ~~¿qué debe responder la consulta pública cuando el vendedor no creó tienda?~~ Se informa explícitamente que ese vendedor no tiene tienda, sin tratarlo como error del sistema; la consulta de la propia tienda sin tienda también informa la ausencia (RF-4, RF-13).
- ~~¿qué longitud mínima y máxima debe tener el nombre de la tienda?~~ El nombre es obligatorio y debe tener entre 1 y 80 caracteres tras recortar los espacios de los extremos; la descripción es opcional y admite hasta 1000 caracteres, y una descripción vacía o solo espacios se guarda como ausente (RF-6, RF-7, RF-9).
- ~~¿la creación de la tienda es requisito para que una cuenta pueda publicar productos?~~ No; publicar productos pertenece a otro servicio y este corte no exige ni valida la existencia de tienda para publicar.
- ~~¿el nombre de la tienda debe ser único entre vendedores?~~ No; dos cuentas pueden usar el mismo nombre de tienda (RF-10).
- ~~¿la tienda se crea y se actualiza con operaciones separadas?~~ No; la operación de guardado crea la tienda si la cuenta no tiene y reemplaza nombre y descripción si ya tiene, sin rechazar por "ya existe" ni por "no existe todavía" (RF-2, RF-3).
- ~~¿qué datos públicos se exponen de la tienda?~~ Solo el nombre y la descripción; nunca el correo ni credenciales (RF-15, RF-16).
