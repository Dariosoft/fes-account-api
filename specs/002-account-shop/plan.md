# Plan 002 — Tienda pública del vendedor

Desglose técnico de `spec.md` para `account-api`, alineado con la arquitectura **Layered** de `AGENTS.md`. Este plan define el diseño a implementar; no incluye código. Todo el texto está en español y los identificadores de clases/rutas usan los nombres reales previstos.

## Objetivo técnico

Incorporar al servicio de cuentas el dominio **Shop** (tienda del vendedor) como dato propiedad de la cuenta, sin salir del paquete `com.friendlyeshop.account` ni de las carpetas Layered (`controller/`, `service/`, `repository/`, `model/`, `model/dto/`, `config/`, `exception/`). El vendedor autenticado guarda su tienda con un único `PUT` idempotente (upsert) y cualquier visitante anónimo la consulta por identificador de cuenta. La tienda es una sola por cuenta, expone únicamente `name` y `description`, y nunca correo ni credenciales.

Se reutiliza la sesión existente (`fes_session` + `SessionService`) para resolver la cuenta dueña; no se crea autenticación nueva ni se modifica CORS. La persistencia es una migración Flyway aditiva `V3` sobre la base `accounts`, compatible con `ddl-auto: validate`.

## Estado actual relevante

- Java 25 / Spring Boot 4.1, paquete `com.friendlyeshop.account`; Layered con extensiones concretas `client/oauth`, `http/cookie`, `constants`.
- Migraciones existentes: `V1__create_accounts.sql`, `V2__google_accounts_and_sessions.sql`. Hibernate `ddl-auto: validate`; no se editan migraciones aplicadas.
- `ApiPathConfig` prefija con `/accounts` todo controlador del paquete `controller`; los controladores solo declaran su recurso.
- `CorsConfig` registra `/accounts/**` con `allowCredentials(true)` y métodos `GET`, `POST`, `OPTIONS` desde los orígenes de `BROWSER_ORIGINS`. No se modifica.
- `SessionCookieWriter.COOKIE_NAME = "fes_session"` (HttpOnly, SameSite=Lax, Path=/).
- `SessionService.read(UUID)` devuelve `SessionResponse` con `authenticated` y, si aplica, `id` de la cuenta autenticada.
- `Account`/`Session` son entidades JPA; `Session` referencia la cuenta con un `UUID accountId` plano (FK solo en base de datos), patrón que se replica en `Shop`.
- Tests ya usan JUnit 5, Mockito, MockMvc, `@SpringBootTest` y Testcontainers PostgreSQL. Cobertura mínima 80 % (JaCoCo).

## Tabla de decisiones

| Decisión | Motivo | RF |
|---|---|---|
| Una sola tienda por cuenta garantizada por `UNIQUE (account_id)` en base de datos, con `ShopRepository.findByAccountId(UUID)`. | La unicidad es estructural y no depende de lógica de aplicación; evita duplicados aun ante concurrencia. | RF-1 |
| Un único `PUT /accounts/shop` que crea si no existe y reemplaza si existe (upsert). | La spec pide guardar sin distinguir alta de edición ni rechazar por "ya existe"/"no existe todavía". | RF-2, RF-3 |
| Identidad del dueño tomada **solo** de la sesión (`fes_session` → `SessionService.read`), nunca del body ni de la ruta en operaciones de escritura. | Hace estructuralmente imposible escribir la tienda de otra cuenta. | RF-5, RF-11 |
| `GET /accounts/shop` y `PUT /accounts/shop` exigen sesión; sin sesión → `401`. | La tienda propia pertenece a la cuenta autenticada. | RF-4, RF-11 |
| `GET /accounts/shops/{accountId}` público, sin sesión. | El detalle público de la publicación lo consume un visitante anónimo. | RF-12, RF-15 |
| Distinguir en el mensaje la cuenta inexistente de la cuenta sin tienda, manteniendo `404` en ambos casos. | La spec exige mensajes distintos para RF-13 y RF-14; ambos son "no hay tienda que mostrar". | RF-4, RF-13, RF-14 |
| Respuesta pública `ShopResponse(name, description)` sin `email` ni credenciales. | Solo se publican datos públicos de la tienda. | RF-15, RF-16 |
| Normalizar en el DTO (recorte de extremos, descripción vacía/solo espacios → `null`) y validar con Bean Validation; además la entidad `Shop` revalida sus invariantes. | Cumple los límites medidos sobre el texto recortado y protege la regla en cualquier punto de entrada. | RF-6, RF-7, RF-8, RF-9 |
| Serializar `description` como `null` explícito (sin `@JsonInclude(NON_NULL)` en `ShopResponse`). | El contrato cross-repo expone `{name, description|null}` con la clave presente. | RF-9 |
| Sin restricción de unicidad sobre `name`. | Dos cuentas pueden usar el mismo nombre de tienda. | RF-10 |
| `Shop` guarda `accountId` como `UUID` plano (igual que `Session`); la FK existe solo en la migración. | Evita cargar/acoplar la entidad `Account` para operar la tienda y mantiene el estilo Layered existente. | RF-1, RF-5 |
| Reutilizar `SessionService` y la cookie existente; no crear autenticación nueva. | La sesión es la fuente de verdad de identidad del servicio. | RF-11 |
| No modificar `CorsConfig` ni `ApiPathConfig`. | El prefijo `/accounts` ya aplica y el storefront solo requiere `GET`; el contrato pide no cambiar CORS. | RF-12, RF-15 |
| Migración aditiva `V3__create_shops.sql`; no tocar `V1`/`V2`. | Compatibilidad con `ddl-auto: validate` y despliegues existentes. | RF-1 |

## Estructura objetivo (Layered)

```
com.friendlyeshop.account
  controller/
    ShopController            (nuevo: GET /shop, PUT /shop)
    PublicShopController      (nuevo: GET /shops/{accountId})
    SessionController         (sin cambios)
    GoogleLoginController     (sin cambios)
  service/
    ShopService               (nuevo: consulta propia, upsert, consulta pública)
    SessionService            (reutilizado para resolver la cuenta autenticada)
    GoogleLoginService        (sin cambios)
  repository/
    ShopRepository            (nuevo: JpaRepository<Shop, UUID> + findByAccountId)
    AccountRepository         (reutilizado: existsById para distinguir 404)
    SessionRepository         (sin cambios)
  model/
    Shop                      (nuevo: entidad JPA de la tienda)
    Account, Session          (sin cambios)
    dto/
      ShopRequest             (nuevo: body de entrada)
      ShopResponse            (nuevo: contrato público)
      ErrorResponse           (nuevo: cuerpo de error uniforme)
      SessionResponse, ...    (sin cambios)
  exception/
    ShopNotFoundException     (nuevo: 404 sin tienda / cuenta inexistente)
    NotAuthenticatedException (nuevo: 401 sin sesión)
    GlobalExceptionHandler    (nuevo: @RestControllerAdvice acotado a lo nuevo + validación)
  config/
    ApiPathConfig             (sin cambios: prefija /accounts)
    CorsConfig                (sin cambios)
  http/cookie/
    SessionCookieWriter       (reutilizado: COOKIE_NAME = fes_session)
```

Aislamiento del dominio Shop mediante nombres/Clases dentro de las carpetas existentes: no se crean paquetes por capacidad ni un paquete `shop/`; todo se identifica por prefijo `Shop*` en las capas actuales.

## Contrato HTTP y JSON

Prefijo aplicado automáticamente por `ApiPathConfig`: `/accounts`.

| Método | Ruta | Sesión | Body | Éxito | Errores | RF |
|---|---|---|---|---|---|---|
| `GET` | `/accounts/shop` | `fes_session` obligatoria | — | `200 {name, description\|null}` | `401` sin sesión; `404` si la cuenta no tiene tienda | RF-4, RF-11 |
| `PUT` | `/accounts/shop` | `fes_session` obligatoria | `{name, description\|null}` | `200 {name, description\|null}` (upsert) | `400` validación; `401` sin sesión | RF-2, RF-3, RF-5, RF-6, RF-7, RF-8, RF-9, RF-11 |
| `GET` | `/accounts/shops/{accountId}` | pública | — | `200 {name, description\|null}` | `404` si la cuenta no existe o no tiene tienda | RF-12, RF-13, RF-14, RF-15, RF-16 |

Cuerpos JSON de éxito:

```json
{ "name": "Tienda de Ada", "description": "Cosas bonitas" }
```

```json
{ "name": "Tienda de Ada", "description": null }
```

Cuerpos JSON de error (`ErrorResponse` = `{ "error": "<código>", "message": "<mensaje en español>" }`):

| Situación | HTTP | `error` | `message` |
|---|---|---|---|
| Sin sesión en tienda propia | 401 | `AUTH_REQUIRED` | "Necesitás iniciar sesión para administrar tu tienda." |
| Cuenta autenticada sin tienda | 404 | `SHOP_NOT_FOUND` | "Todavía no configuraste tu tienda." |
| Vendedor público sin tienda | 404 | `SHOP_NOT_FOUND` | "Ese vendedor todavía no tiene tienda." |
| Cuenta pública inexistente | 404 | `ACCOUNT_NOT_FOUND` | "La cuenta indicada no existe." |
| Body inválido | 400 | `VALIDATION_ERROR` | Mensajes en español del/los campos inválidos (unidos). |

Reglas de validación (medidas sobre el texto recortado):

- `name`: obligatorio, `1..80` caracteres tras recortar extremos (RF-6).
- `description`: opcional, `<= 1000` caracteres tras recortar extremos (RF-7, RF-8); vacío o solo espacios → `null` (RF-9).
- Sin unicidad de `name` entre cuentas (RF-10).

## Cambios por capa

- **`model/Shop` (nuevo)**: entidad JPA `@Table(name = "shops")`; `id UUID`, `accountId UUID` (`account_id`, `nullable=false`, `unique=true`), `name` (`nullable=false`, `length=80`), `description` (`length=1000`, nullable), `createdAt`/`updatedAt` (`nullable=false`). Métodos `open(accountId, name, description, now)` y `updateDetails(name, description, now)` con normalización/validación de límites; constantes `NAME_MAX_LENGTH=80`, `DESCRIPTION_MAX_LENGTH=1000`.
- **`model/dto/ShopRequest` (nuevo)**: record `name`, `description`; constructor compacto que recorta extremos y convierte descripción vacía/solo espacios a `null`; anotaciones Bean Validation (`@NotBlank` + `@Size(max = Shop.NAME_MAX_LENGTH)`; `@Size(max = Shop.DESCRIPTION_MAX_LENGTH)`), con mensajes en español.
- **`model/dto/ShopResponse` (nuevo)**: record `name`, `description` sin `NON_NULL` para exponer `description: null`.
- **`model/dto/ErrorResponse` (nuevo)**: record `error`, `message` para respuestas de error uniformes.
- **`repository/ShopRepository` (nuevo)**: `JpaRepository<Shop, UUID>` + `Optional<Shop> findByAccountId(UUID accountId)`.
- **`service/ShopService` (nuevo)**: depende de `ShopRepository`, `AccountRepository`, `SessionService`.
  - `readOwnShop(sessionId)`: resuelve la cuenta vía `SessionService.read`; sin sesión → `NotAuthenticatedException`; sin tienda → `ShopNotFoundException` (mensaje de tienda propia); con tienda → `ShopResponse`.
  - `saveOwnShop(sessionId, name, description)`: resuelve la cuenta; `findByAccountId` y crea con `Shop.open` o actualiza con `updateDetails`; devuelve `ShopResponse` (upsert).
  - `readPublicShop(accountId)`: `AccountRepository.existsById` para distinguir cuenta inexistente (`ACCOUNT_NOT_FOUND`) de cuenta sin tienda (`SHOP_NOT_FOUND`); devuelve `ShopResponse` sin datos privados.
- **`controller/ShopController` (nuevo)**: controlador delgado con `@RequestMapping("/shop")`; `@CookieValue(COOKIE_NAME, required = false)` en los endpoints propios; `@Valid @RequestBody ShopRequest` en `PUT`; traduce servicio → `ShopResponse` y delega errores al advice.
- **`controller/PublicShopController` (nuevo)**: controlador delgado con `@RequestMapping("/shops")`; `GET /{accountId}` público sin cookie para la lectura anónima.
- **`exception/ShopNotFoundException` (nuevo)**: lleva `error` y `message` para los tres 404 diferenciados.
- **`exception/NotAuthenticatedException` (nuevo)**: 401 de tienda propia sin sesión.
- **`exception/GlobalExceptionHandler` (nuevo)**: `@RestControllerAdvice` acotado a `ShopNotFoundException`, `NotAuthenticatedException` y `MethodArgumentNotValidException`; no altera el comportamiento de controladores existentes.
- **`http/cookie/SessionCookieWriter`**: reutilizado (solo su constante `COOKIE_NAME`); sin cambios.
- **`config/ApiPathConfig` y `config/CorsConfig`**: sin cambios.

## Migración y compatibilidad (`ddl-auto: validate`)

Nueva migración `src/main/resources/db/migration/V3__create_shops.sql` (no editar `V1`/`V2`):

```sql
CREATE TABLE shops (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL UNIQUE REFERENCES accounts (id),
    name VARCHAR(80) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

- El `UNIQUE` sobre `account_id` crea el índice que respalda `findByAccountId`; no se agrega índice extra.
- Tipos y longitudes deben coincidir exactamente con el mapeo JPA (`VARCHAR(80)`, `VARCHAR(1000)`, `TIMESTAMPTZ`, `UUID`) para que la validación de Hibernate pase.
- La tabla es aditiva: no cambia columnas de `accounts` ni `sessions`; despliegue aplica Flyway antes de la validación JPA (orden estándar de Spring Boot).
- `ON DELETE` sin acción explícita (no hay borrado de cuentas en este servicio).

## Pruebas

El repositorio ya usa Testcontainers, por lo que la persistencia se prueba contra PostgreSQL real.

- **Unitarias de dominio (`ShopTest`)**: `open`/`updateDetails` recortan extremos; nombre de 1 y 80 se aceptan; nombre vacío/solo espacios/81 se rechazan; descripción `null`/vacía/solo espacios → `null`; 1000 se acepta; 1001 se rechaza; `updatedAt` se actualiza en `updateDetails` (RF-6, RF-7, RF-8, RF-9).
- **Unitarias de servicio (`ShopServiceTest`)** con repos y `SessionService` mockeados: sin sesión → `NotAuthenticatedException` sin tocar repositorios; alta crea una tienda; segundo guardado actualiza la misma (RF-2, RF-3); cuenta B no modifica la tienda de A (RF-5); consulta propia sin tienda → 404 mensaje propio (RF-4); consulta pública con tienda devuelve solo `name`/`description` (RF-12, RF-15, RF-16); cuenta sin tienda vs inexistente → 404 diferenciados (RF-13, RF-14).
- **Web con MockMvc (`ShopControllerTest`)**: `GET /accounts/shop` con cookie → 200; sin cookie → 401; `PUT /accounts/shop` válido → 200 y upsert; nombre/descripción inválidos → 400 con mensaje en español sin guardar (RF-6, RF-7, RF-8, RF-9); `GET /accounts/shops/{accountId}` sin cookie → 200 y 404 (RF-12, RF-13, RF-14).
- **Integración con Testcontainers (`ShopRepositoryTest`)**: `findByAccountId`, unicidad por cuenta (segundo `save` de la misma cuenta viola el `UNIQUE`), dos cuentas con el mismo `name` permitidas (RF-1, RF-10), y migraciones `V1`–`V3` con `ddl-auto: validate` en contexto.
- **Aceptación (`AccountShopAcceptanceTest`, `@SpringBootTest` + Testcontainers)**: cuenta + sesión sembradas, doble `PUT` con la cookie → `count()` de tiendas = 1 y datos actualizados; `GET /accounts/shops/{accountId}` anónimo devuelve `name`/`description` y el JSON **no** contiene `email`; vendedor sin tienda y UUID inexistente → 404; `PUT` sin cookie → 401 (RF-1, RF-2, RF-3, RF-4, RF-5, RF-10, RF-11, RF-12, RF-13, RF-14, RF-15, RF-16).
- **No regresión**: `GET /accounts/session` y `POST /accounts/logout` siguen en verde; CORS `GET` desde el origen del storefront con credenciales (RF-12, RF-15).
- **Cierre**: `./mvnw verify` (Checkstyle + tests + JaCoCo ≥ 80 %).

## Riesgos

- **CORS y `PUT`**: `CorsConfig` permite `GET`, `POST`, `OPTIONS`. El `PUT /accounts/shop` desde el navegador del panel necesitaría `PUT` en `allowedMethods`; la instrucción prohíbe cambiar CORS, así que se documenta y se verifica el consumo real (mismo origen/proxy o llamada servidor-a-servidor). El `GET` público del storefront ya está cubierto.
- **Carrera en el primer guardado**: dos `PUT` concurrentes de una cuenta sin tienda podrían chocar con el `UNIQUE(account_id)` (`DataIntegrityViolationException`). Se acepta como riesgo para este corte; si aparece, reevaluar/reintentar lectura+update en un corte posterior.
- **Divulgación de existencia de cuenta**: el endpoint público distingue cuenta inexistente de cuenta sin tienda (RF-13/RF-14), revelando si un `accountId` existe. La spec lo exige y solo se expone un mensaje, no datos privados.
- **Alcance del exception handler**: acotarlo a excepciones nuevas y `MethodArgumentNotValidException` para no cambiar el cuerpo de error de controladores existentes.
- **Longitud "caracteres"**: `@Size`/`String.length()` miden unidades UTF-16, no puntos de código; textos con emojis podrían contar distinto. Se documenta; si se requiere estricto, usar `codePointCount` en la entidad.
- **Recorte de espacios**: usar `String.strip()` (consciente de Unicode) en lugar de `trim()`; los espacios internos sí cuentan para el límite.
- **Validación duplicada DTO/entidad**: mismo límite expresado en dos capas; se mitiga centralizando las constantes `NAME_MAX_LENGTH`/`DESCRIPTION_MAX_LENGTH` en `Shop` y referenciándolas desde el DTO y los tests.
- **Acoplamiento DTO → entidad**: `ShopRequest` importa las constantes de `Shop` para no duplicar literales; es un acoplamiento mínimo y explícito.
- **Cobertura**: el nuevo código debe quedar cubierto para no bajar del 80 % exigido por JaCoCo.

## Trazabilidad RF → plan/pruebas

| RF | Cubierto en | Prueba |
|---|---|---|
| RF-1 | Decisión `UNIQUE(account_id)`; migración `shops`; `ShopRepository` | `ShopRepositoryTest` unicidad por cuenta |
| RF-2 | `ShopService.saveOwnShop` crea si no existe | `ShopServiceTest` alta; aceptación doble PUT |
| RF-3 | `ShopService.saveOwnShop` actualiza si existe | `ShopServiceTest` update sin segunda fila |
| RF-4 | `GET /accounts/shop` 404 propio con mensaje | `ShopServiceTest`/`ShopControllerTest` |
| RF-5 | Dueño solo desde sesión; sin `accountId` en escritura | `ShopServiceTest` cuenta B no altera A |
| RF-6 | `ShopRequest` + `Shop` validan nombre 1..80 | `ShopTest`, `ShopControllerTest` 400 |
| RF-7 | Descripción recortada ≤1000, 1000 acepta | `ShopTest`, `ShopControllerTest` |
| RF-8 | Descripción >1000 rechazada sin guardar | `ShopTest`, `ShopControllerTest` 400 |
| RF-9 | Descripción vacía/solo espacios → `null`; JSON con `null` | `ShopTest`, respuesta `ShopResponse` |
| RF-10 | Sin unicidad de `name` | `ShopRepositoryTest` mismo nombre en dos cuentas |
| RF-11 | Sin sesión → 401 sin efectos | `ShopServiceTest`, `ShopControllerTest`, aceptación |
| RF-12 | `GET /accounts/shops/{accountId}` público | `ShopControllerTest`, aceptación |
| RF-13 | Cuenta sin tienda → 404 mensaje "no tiene tienda" | `ShopServiceTest`, aceptación |
| RF-14 | Cuenta inexistente → 404 "no existe" | `ShopServiceTest`, aceptación |
| RF-15 | `ShopResponse` solo `name`/`description` | `ShopControllerTest`, aceptación (JSON sin `email`) |
| RF-16 | Sin email/credenciales en respuesta pública | aserción de ausencia de `email` en aceptación |
```
