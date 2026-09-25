# Tasks — Spec 001: Cuenta única con Google

Implementación paso a paso de `spec.md` según `plan.md`.
Tareas pequeñas (~20–30 min), en orden de dependencia. Cada una declara los RF que cubre y un criterio verificable.

---

## Fase 1 — Esquema y dominio de cuenta

- [x] **T1:** Crear migración Flyway `V2` que quite `password_hash`, `role` y el índice `accounts_role_idx` de la tabla `accounts`.
  - **RF:** RF-1, RF-2
  - **Done when:** Al arrancar con Flyway, el esquema de `accounts` ya no tiene columnas ni índice de contraseña/rol; `V1` no se edita.

- [x] **T2:** En la misma o siguiente migración, asegurar `email` UNIQUE (normalizable), añadir campo de nombre visible (`display_name` o `name`) y marcas de tiempo (`created_at`; opcional `updated_at`).
  - **RF:** RF-1, RF-2
  - **Done when:** El esquema validable por Hibernate incluye email único, nombre y timestamps; no hay `password_hash` ni `role`.

- [x] **T3:** Modelar la entidad JPA `Account` (UUID, email normalizado, nombre, timestamps) sin password ni roles, y el repositorio correspondiente.
  - **RF:** RF-1, RF-2
  - **Done when:** Existe `Account` + repositorio; la entidad no expone `password_hash` ni `role`; compila contra el esquema de T1–T2.

- [x] **T4:** Implementar `AccountService` con búsqueda por email normalizado y creación de cuenta (correo + nombre) cuando no exista.
  - **RF:** RF-1, RF-2, RF-4, RF-9
  - **Done when:** Un test unitario muestra: email nuevo → crea una fila; mismo email otra vez → reutiliza la misma cuenta (mismo id), sin segunda fila.

---

## Fase 2 — Sesión compartida

- [x] **T5:** Crear migración Flyway para tabla `sessions` (o equivalente): id opaco, `account_id`, marcas de tiempo; **sin** columna de caducidad automática.
  - **RF:** RF-5, RF-12, RF-13
  - **Done when:** Flyway crea la tabla de sesiones; el esquema no impone TTL de sesión.

- [x] **T6:** Modelar entidad/repositorio de sesión y `SessionService` con crear (asociar a `account_id`), resolver por id de sesión y invalidar.
  - **RF:** RF-5, RF-6, RF-8, RF-12, RF-13
  - **Done when:** Tests unitarios: crear → resolver devuelve la cuenta; invalidar → resolver ya no encuentra sesión activa.

- [x] **T7:** Definir el contrato HTTP de sesión (cookie HttpOnly y/o header) y helpers para leer/escribir el id de sesión en requests/responses, sin loguear el valor en claro.
  - **RF:** RF-12, RF-13
  - **Done when:** Hay un componente reutilizable que adjunta e interpreta el id de sesión; no aparecen session ids ni secretos en logs de prueba.

---

## Fase 3 — Google y entrada de la tienda

- [x] **T8:** Añadir dependencias de validación Google (OAuth2 Client / Resource Server o verificación de `id_token`) y propiedades externalizadas (client id/secret, redirect, issuer) vía env/`application.yaml`.
  - **RF:** RF-3
  - **Done when:** La app arranca con placeholders/env; secretos no están hardcodeados en el código fuente.

- [x] **T9:** Implementar `GoogleIdentityService` (o adapter) que valide credencial Google y extraiga email + nombre; en fallo/rechazo no inventa identidad.
  - **RF:** RF-3, RF-7
  - **Done when:** Test con token/código inválido o mock de rechazo → error claro; token válido (mock) → email y nombre; mensajes de error visibles en español.

- [x] **T10:** Exponer endpoints de entrada de tienda bajo `/accounts` (iniciar/completar login Google) que en éxito llaman find-or-create de cuenta y abren sesión compartida.
  - **RF:** RF-3, RF-4, RF-9
  - **Done when:** Test de integración/MVC: Google OK + email nuevo → cuenta creada + sesión; mismo email de nuevo → misma cuenta, sin duplicar.

- [x] **T11:** En el mismo flujo de login, si la validación Google falla o se rechaza, no crear sesión ni marcar autenticado; respuesta 401/403 (o redirect de error) con mensaje en español.
  - **RF:** RF-7
  - **Done when:** Test: Google fail → sin cookie/sesión activa; “quién soy” posterior no presenta autenticado.

---

## Fase 4 — Quién soy y salida

- [x] **T12:** Endpoint de sesión actual (p. ej. `GET /accounts/me` o `GET /accounts/session`): con sesión válida devolver **nombre** y **correo de Google**.
  - **RF:** RF-6
  - **Done when:** Test con sesión activa → 200 con nombre y email; no se expone password ni roles.

- [x] **T13:** En el endpoint “quién soy”, sin sesión válida no presentar a nadie como autenticado (401 o cuerpo anónimo explícito).
  - **RF:** RF-8
  - **Done when:** Test sin sesión (o sesión inválida) → no hay identidad inventada; coherente con el contrato elegido.

- [x] **T14:** Endpoint de logout (p. ej. `POST /accounts/session/logout`) que invalida la sesión compartida; idempotente si no hay sesión.
  - **RF:** RF-5, RF-8, RF-12
  - **Done when:** Test: login → logout → “quién soy” sin autenticado; segundo logout no falla y el resultado sigue sin autenticado.

- [x] **T15:** Tras logout, cualquier request posterior (me u otros protegidos) se trata como sin acceso activo (cubre recarga/solicitud post-salir).
  - **RF:** RF-13
  - **Done when:** Test: tras logout, reutilizar el id de sesión antiguo no autentica; respuesta alineada con RF-8.

---

## Fase 5 — Panel: reconocimiento y sesión compartida (sin OAuth aquí)

- [x] **T16:** Contrato server-to-server de lookup por correo de Google: si existe cuenta, devolver nombre + correo (misma cuenta); si no, no encontrada. **Sin** endpoint de login OAuth del panel.
  - **RF:** RF-10, RF-11
  - **Done when:** Test: email existente → nombre + email; email inexistente → not found; no hay ruta de “panel Google OAuth login” en este servicio.

- [x] **T17:** Operación para que el panel (tras validar Google por su puerta) abra/asocie la **misma** sesión compartida por email o `accountId`, sin ser este servicio la puerta OAuth del panel.
  - **RF:** RF-10, RF-12
  - **Done when:** Test: establish-session para cuenta existente → id de sesión usable; “quién soy” con esa sesión ve la misma cuenta; no se implementa callback OAuth de panel.

- [x] **T18:** Verificar que logout desde el flujo de tienda cierra también la sesión que el panel usaría (y viceversa vía el mismo endpoint de logout).
  - **RF:** RF-5, RF-12, RF-13
  - **Done when:** Test: sesión abierta (tienda o establish-session) → logout → tanto me de tienda como uso posterior del mismo id quedan sin acceso activo.

---

## Fase 6 — Errores, seguridad RNF y cierre de cobertura RF

- [x] **T19:** `@ControllerAdvice` (o equivalente) con mensajes de error visibles en español para fallos de auth/validación usados por login, me y lookup.
  - **RF:** RF-7
  - **Done when:** Respuestas de error de los flujos anteriores muestran mensajes en español; no filtran secretos ni tokens.

- [x] **T20:** Revisar configuración de cookies/sesión (`HttpOnly`, `Secure` en no-local, `SameSite`) y que no se logueen client secrets, tokens Google ni session ids.
  - **RF:** RF-12, RF-13
  - **Done when:** Checklist en código/config documentada en comentario o test de propiedades; grep/tests no muestran secretos en respuestas ni logs de prueba.

- [x] **T21:** Pruebas automatizadas explícitas de que la cuenta es solo identidad: una cuenta por email, sin roles, sin password_hash, y este servicio es dueño de la fila (sin segunda cuenta de vendedor).
  - **RF:** RF-1, RF-2, RF-9
  - **Done when:** Suite verde que aserta unicidad, ausencia de campos/roles de comprador-vendedor, y reutilización por email.

- [x] **T22:** Matriz de tests de aceptación por RF restante y `./mvnw verify` (Checkstyle + tests) en verde; confirmar ausencia de entrada OAuth del panel.
  - **RF:** RF-1, RF-2, RF-3, RF-4, RF-5, RF-6, RF-7, RF-8, RF-9, RF-10, RF-11, RF-12, RF-13
  - **Done when:** Cada RF-1…RF-13 tiene al menos una prueba automatizada en verde; `./mvnw verify` pasa; no existe endpoint de login OAuth del panel.

---

## Mapa RF → tareas

| RF | Tareas |
| --- | --- |
| RF-1 | T1, T2, T3, T4, T21, T22 |
| RF-2 | T1, T2, T3, T4, T21, T22 |
| RF-3 | T8, T9, T10, T22 |
| RF-4 | T4, T10, T22 |
| RF-5 | T5, T6, T14, T18, T22 |
| RF-6 | T6, T12, T22 |
| RF-7 | T9, T11, T19, T22 |
| RF-8 | T6, T13, T14, T22 |
| RF-9 | T4, T10, T21, T22 |
| RF-10 | T16, T17, T22 |
| RF-11 | T16, T22 |
| RF-12 | T5, T6, T7, T14, T17, T18, T20, T22 |
| RF-13 | T5, T6, T7, T15, T18, T20, T22 |
