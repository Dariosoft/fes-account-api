# Tasks 002 — Tienda pública del vendedor

Implementación paso a paso de `spec.md` / `plan.md` para `account-api`. Cada tarea está pensada para 20–30 minutos, en orden de dependencia. No implementar fuera de este orden. Ninguna tarea modifica CORS, `ApiPathConfig`, `V1` ni `V2`.

## Persistencia y dominio

- [x] **T1. Migración Flyway V3 de la tabla `shops`**
  Cubre: **RF-1**.
  Crear `src/main/resources/db/migration/V3__create_shops.sql` con `id UUID PRIMARY KEY`, `account_id UUID NOT NULL UNIQUE REFERENCES accounts (id)`, `name VARCHAR(80) NOT NULL`, `description VARCHAR(1000)` nullable, `created_at`/`updated_at TIMESTAMPTZ NOT NULL`. No editar `V1`/`V2`.
  **Done when:** el contexto con `ddl-auto: validate` y Testcontainers arranca aplicando `V1`–`V3`, y el esquema contiene la tabla `shops` con esas columnas, longitudes y el `UNIQUE (account_id)`.

- [x] **T2. Entidad `Shop` con reglas de nombre y descripción**
  Cubre: **RF-1**, **RF-6**, **RF-7**, **RF-8**, **RF-9**.
  Entidad JPA en `model/Shop.java` mapeada a `shops` (`accountId` como `UUID` plano, `name`, `description`, timestamps); constantes `NAME_MAX_LENGTH=80`, `DESCRIPTION_MAX_LENGTH=1000`; `open(...)` y `updateDetails(...)` que recortan extremos, convierten descripción vacía/solo espacios a `null` y validan `1..80`/`<=1000`.
  **Done when:** `ShopTest` en verde cubre 1 y 80 aceptados, vacío/solo espacios/81 rechazados, descripción `null`/vacía/solo espacios → `null`, 1000 aceptado y 1001 rechazado, y `updatedAt` actualizado en `updateDetails`.

- [x] **T3. `ShopRepository` y unicidad por cuenta**
  Cubre: **RF-1**, **RF-10**.
  `JpaRepository<Shop, UUID>` con `Optional<Shop> findByAccountId(UUID accountId)`. Sin unicidad sobre `name`.
  **Done when:** `ShopRepositoryTest` (Testcontainers) guarda y recupera por `accountId`, confirma que un segundo `save` de la misma cuenta viola el `UNIQUE` y que dos cuentas distintas pueden usar el mismo `name` sin error.

## DTOs y manejo de errores

- [x] **T4. DTOs `ShopRequest`, `ShopResponse` y `ErrorResponse`**
  Cubre: **RF-6**, **RF-7**, **RF-9**, **RF-15**, **RF-16**.
  `ShopRequest` (record `name`, `description`) con constructor compacto que recorta extremos y convierte descripción vacía/solo espacios a `null`, y Bean Validation con mensajes en español (`@NotBlank` + `@Size(max = Shop.NAME_MAX_LENGTH)`, `@Size(max = Shop.DESCRIPTION_MAX_LENGTH)`). `ShopResponse(name, description)` sin `@JsonInclude(NON_NULL)` para exponer `description: null` y sin campo `email`. `ErrorResponse(error, message)`.
  **Done when:** test unitario confirma normalización del request y que `ShopResponse` serializa `{"name":...,"description":null}` sin claves privadas.

- [x] **T5. Excepciones y `GlobalExceptionHandler`**
  Cubre: **RF-4**, **RF-11**, **RF-13**, **RF-14**.
  Crear `exception/NotAuthenticatedException` (401, `AUTH_REQUIRED`), `exception/ShopNotFoundException` (404 con código/mensaje para tienda propia, vendedor sin tienda y cuenta inexistente) y `exception/GlobalExceptionHandler` (`@RestControllerAdvice`) que mapea esas excepciones y `MethodArgumentNotValidException` a `ErrorResponse`, acotado para no alterar controladores existentes.
  **Done when:** test de slice confirma `401`, los tres `404` diferenciados y `400` de validación con cuerpo `{error, message}` en español.

## Servicio

- [x] **T6. Resolución de la cuenta autenticada (`ShopService`, 401)**
  Cubre: **RF-11**.
  Crear `service/ShopService` con dependencias `ShopRepository`, `AccountRepository`, `SessionService`; un método privado que resuelve la cuenta desde la cookie con `SessionService.read(sessionId)` y, si no está autenticada, lanza `NotAuthenticatedException`.
  **Done when:** `ShopServiceTest` confirma que sin sesión válida se lanza `NotAuthenticatedException` y no se invoca `ShopRepository` (sin crear ni modificar datos).

- [x] **T7. Upsert de la tienda propia**
  Cubre: **RF-2**, **RF-3**, **RF-5**.
  `saveOwnShop(sessionId, name, description)`: resuelve la cuenta, `findByAccountId`, crea con `Shop.open` si no existe y actualiza con `updateDetails` si existe; devuelve `ShopResponse`.
  **Done when:** `ShopServiceTest` confirma que el primer guardado crea una fila, el segundo actualiza la misma (sin segunda tienda) y que la cuenta B guardando no modifica la tienda de la cuenta A.

- [x] **T8. Consulta de la tienda propia**
  Cubre: **RF-4**.
  `readOwnShop(sessionId)`: resuelve la cuenta; si no tiene tienda lanza `ShopNotFoundException` con mensaje "Todavía no configuraste tu tienda."; si tiene, devuelve `ShopResponse`.
  **Done when:** `ShopServiceTest` cubre con tienda (devuelve nombre/descripción) y sin tienda (404 con mensaje propio, no error de sistema).

- [x] **T9. Consulta pública de la tienda**
  Cubre: **RF-12**, **RF-13**, **RF-14**, **RF-15**, **RF-16**.
  `readPublicShop(accountId)`: `AccountRepository.existsById` para distinguir cuenta inexistente (`ACCOUNT_NOT_FOUND`) de cuenta sin tienda (`SHOP_NOT_FOUND`, "Ese vendedor todavía no tiene tienda."); si existe tienda, devuelve solo `name`/`description`.
  **Done when:** `ShopServiceTest` cubre tienda existente, vendedor sin tienda y cuenta inexistente con 404 diferenciados, y verifica que la respuesta no contiene datos privados de la cuenta.

## Controller y validación web

- [x] **T10. Endpoints propios `GET` y `PUT /accounts/shop`**
  Cubre: **RF-2**, **RF-3**, **RF-4**, **RF-11**.
  `controller/ShopController` delgado con `@RequestMapping("/shop")`: `@GetMapping` y `@PutMapping` relativos, con `@CookieValue(SessionCookieWriter.COOKIE_NAME, required = false)`; `PUT` recibe `@Valid @RequestBody ShopRequest`. No declarar `/accounts` (lo aporta `ApiPathConfig`).
  **Done when:** `ShopControllerTest` (MockMvc) confirma `GET` con cookie → 200, `GET` sin cookie → 401, `PUT` válido → 200 con upsert y `PUT` sin cookie → 401.

- [x] **T11. Endpoint público `GET /accounts/shops/{accountId}`**
  Cubre: **RF-12**, **RF-13**, **RF-14**, **RF-15**.
  Crear `controller/PublicShopController` con `@RequestMapping("/shops")` y `@GetMapping("/{accountId}")` sin cookie; devuelve `ShopResponse`.
  **Done when:** `ShopControllerTest` confirma `200 {name, description|null}` sin sesión, `404` para vendedor sin tienda y `404` para cuenta inexistente, sin `email` en el JSON.

- [x] **T12. Validación web con `400` en español**
  Cubre: **RF-6**, **RF-7**, **RF-8**, **RF-9**.
  Asegurar que `PUT /accounts/shop` con nombre ausente/solo espacios/fuera de `1..80` o descripción `>1000` responde `400 VALIDATION_ERROR` con mensaje en español, sin guardar cambios; descripción vacía/solo espacios se persiste como `null`.
  **Done when:** `ShopControllerTest` cubre cada caso inválido (`400`, sin invocación de guardado) y el caso de descripción vacía → `200` con `description: null`.

## Pruebas de integración y cierre

- [x] **T13. Prueba de aceptación end-to-end**
  Cubre: **RF-1**, **RF-2**, **RF-3**, **RF-4**, **RF-5**, **RF-10**, **RF-11**, **RF-12**, **RF-13**, **RF-14**, **RF-15**, **RF-16**.
  `@SpringBootTest` + Testcontainers: sembrar cuenta y sesión, doble `PUT` con cookie (misma tienda, `count() == 1`, datos actualizados), `GET /accounts/shops/{accountId}` anónimo con `name`/`description` y JSON sin `email`, 404 de vendedor sin tienda y UUID inexistente, y `PUT` sin cookie → 401.
  **Done when:** la prueba de aceptación pasa y demuestra todos los criterios de finalización de la spec sin datos privados expuestos.

- [x] **T14. No regresión de sesión y CORS `GET`**
  Cubre: **RF-12**, **RF-15** (y conservación de `/accounts`).
  Verificar que `GET /accounts/session` y `POST /accounts/logout` siguen funcionando y que un `GET` desde el origen del storefront recibe CORS con credenciales; no modificar `CorsConfig`.
  **Done when:** los tests existentes de sesión/CORS siguen verdes y una prueba confirma `Access-Control-Allow-Origin` con credenciales para `GET /accounts/shops/{accountId}`.

- [x] **T15. Checkstyle, verificación y trazabilidad**
  Cubre: **RF-1** … **RF-16**.
  Ejecutar `./mvnw verify` (Checkstyle + tests + JaCoCo); revisar que cada RF tenga al menos una prueba en verde según el plan.
  **Done when:** `./mvnw verify` pasa, la cobertura de líneas es ≥ 80 % y la tabla de trazabilidad RF → prueba queda completa sin RF abiertos.
