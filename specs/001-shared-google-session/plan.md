# Plan 001 — Sesión Google compartida

Desglose técnico de `spec.md` para `account-api`. Este plan documenta el **diseño implementado** (actualizado tras la implementación en `001/feat-shared-google-session`), alineado con la arquitectura Layered del código y las prácticas Spring Boot del proyecto.

## Estado actual relevante

- Spring Boot 4.1 / Java 25, paquete `com.friendlyeshop.account`.
- Migración Flyway `V2__google_accounts_and_sessions.sql`: `google_sub`, `display_name`, `password_hash` nullable; tabla `sessions`.
- Dependencias: Web, JPA, Flyway, Validation, `google-api-client` / `google-http-client-gson`. Sin Spring Security OAuth2 Client; OAuth vía `GoogleOAuthClient` concreto.
- Configuración vía env: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `SESSION_COOKIE_DOMAIN`, `BROWSER_ORIGINS`, `PUBLIC_API_BASE_URL`, `SESSION_COOKIE_SECURE`. Procedimiento en `docs/google-oauth.md`.
- Hibernate `ddl-auto: validate`; solo migraciones Flyway.

## Principios de diseño

- **Layered Architecture** (implementado): paquetes técnicos base `controller` / `service` / `model` (+ `model.dto`) / `repository` / `config`, extendidos con `client/oauth` para la integración OAuth externa y `http/cookie` para transporte HTTP de cookies. Entidades JPA en `model`; repositorios Spring Data; sin puertos Clean Architecture ni paquetes por capacidad.
- **Separación auth / cuentas** (`AGENTS.md`): controladores delgados; login/logout/sesión distintos de la gestión de cuenta; la cuenta es la fuente de verdad de identidad.
- **Spring Boot**: inyección por constructor, `@ConfigurationProperties` (`fes.auth`), DTOs JSON (`SessionResponse`) sin exponer entidades JPA en HTTP, `@Transactional` en servicios que mutan, tests JUnit 5 / Mockito / MockMvc / `@SpringBootTest`.
- Mensajes visibles a la persona en español; nombres de código y documentación técnica en inglés.
- No almacenar ni exponer secretos de autenticación en claro; no persistir tokens de Google como contraseña.

## Arquitectura implementada (Layered)

```
controller  → GoogleLoginController, SessionController
service     → GoogleLoginService, SessionService
client      → oauth/GoogleOAuthClient, oauth/TokenExchange, oauth/HttpTokenExchange, oauth/OAuthStateCodec
http        → cookie/SessionCookieWriter
model       → Account, Session (+ dto: SessionResponse, CompletedLogin, GoogleProfile)
repository  → AccountRepository, SessionRepository (Spring Data JpaRepository)
config      → AuthProperties, AuthConfig, CorsConfig
```

### Modelo (entidades JPA)

- **Account** (**RF-1**, **RF-2**): entidad JPA; clave de negocio `googleSub` (único); `id` UUID; `email`; `displayName`; `role` por defecto `USER`; timestamps. Una sola cuenta por persona de Google; no hay cuenta paralela tienda/panel.
- **Session** (**RF-8**, **RF-13**, **RF-15**, **RF-22**, NFR caducidad): entidad JPA; id opaco (valor de cookie); `accountId`; `createdAt`; `expiresAt` (30 días, `Session.DEFAULT_TTL_DAYS`); `revokedAt` nullable. Varias sesiones activas por cuenta; un login nuevo no invalida las anteriores (**RF-22**). Validez en `Session.isValid(now)`.

### Persistencia (sin puertos de aplicación)

- `AccountRepository` (Spring Data): `findByGoogleSub`, `save` / `findById` heredados de `JpaRepository`.
- `SessionRepository` (Spring Data): CRUD por id; la validez se filtra en `SessionService` con `isValid`, no con query dedicada en el repositorio.
- `GoogleOAuthClient` (`client/oauth`, clase concreta `@Component`): URL de autorización; delega el intercambio de `code` a `TokenExchange`. Fallos → `Optional.empty()`, sin excepciones al controlador.
- `HttpTokenExchange` (`client/oauth`): implementación concreta del intercambio OAuth HTTP; verifica id_token (`sub`, email, name) y lo transforma a `GoogleProfile`.
- Tiempo: `Instant.now()` en servicios (sin `Clock` inyectado).
- `AuthProperties` (`fes.auth`): client id/secret, cookie domain, orígenes, `cookieSecure` (`SESSION_COOKIE_SECURE`), `publicApiBaseUrl` (`PUBLIC_API_BASE_URL`).

### Servicios (casos de uso)

1. **`GoogleLoginService.start`** (**RF-4**, **RF-5**, **RF-16**): valida `return_to` contra orígenes exactos de `BROWSER_ORIGINS`; si falta o no es permitido → `Optional.empty()` → HTTP 400. Si ok → `OAuthStateCodec.encode(returnTo)` (Base64 opaco `UUID|returnTo`, **no firmado**) + URL de Google vía `GoogleOAuthClient.authorizationUrl`.
2. **`GoogleLoginService.complete`** (**RF-3**, **RF-6**, **RF-7**, **RF-8**, **RF-9**, **RF-10**, **RF-21**, **RF-22**): decodifica `state` con `OAuthStateCodec.returnTo`; si el state es inválido → redirect a **`http://localhost`** (comportamiento implementado). Si Google falla/cancela → sin sesión + redirect a `return_to?login_error=1`. Si éxito → upsert por `googleSub` (`Account.open` / `updateProfile`); **nueva sesión** sin tocar previas; `CompletedLogin` con URL y `sessionId` para Set-Cookie.
3. **`SessionService.read`** (**RF-11**, **RF-12**, **RF-15**, **RF-18**): lee id de cookie; ausente/inválida/caducada → `{ authenticated: false }`; válida → JSON **plano** `{ authenticated: true, id, email, name }` (`SessionResponse`, sin objeto `account` anidado).
4. **`SessionService.logout`** (**RF-13**, **RF-14**, **RF-19**): si hay id, `revoke` + save; el controlador siempre responde **204 No Content** y borra la cookie `fes_session`.

Cualquier cuenta de Google aceptada: no hay allowlist de dominios ni roles de comprador/vendedor en este corte (**RF-3**; roles avanzados fuera de alcance).

## Persistencia (Flyway)

Nueva migración (p. ej. `V2__google_accounts_and_sessions.sql`); no editar `V1`.

Cambios en `accounts` (**RF-1**, **RF-2**, **RF-6**, **RF-7**, **RF-21**):

- Añadir `google_sub VARCHAR(...) NOT NULL UNIQUE` (identidad Google).
- Añadir `display_name VARCHAR(...) NOT NULL` (nombre expuesto en sesión).
- Hacer `password_hash` nullable (cuentas solo-Google no tienen contraseña; NFR: no inventar secretos).
- Conservar `role` con valor por defecto neutro existente/compatible (sin distinguir comprador/vendedor en este corte).
- Mantener `email` único; al reutilizar, actualizar email y `display_name` si cambian.

Nueva tabla `sessions` (**RF-8**, **RF-13**, **RF-22**, NFR 30 días):

- `id` UUID PK (token de sesión / valor de cookie).
- `account_id` UUID FK → `accounts.id`.
- `created_at`, `expires_at`, `revoked_at` nullable (o equivalente que permita invalidar sin borrar historial).
- Índice por `account_id`; consulta de sesión activa: id + `revoked_at IS NULL` + `expires_at > now()`.

Entidades JPA en `model`; repositorios Spring Data en `repository` (sin capa gateway dominio↔JPA).

## API HTTP (capa controller)

Base path `/accounts`. Controladores: `GoogleLoginController` y `SessionController` (auth/sesión).

| Método y ruta | Comportamiento | RF |
|---|---|---|
| `GET /accounts/login/google?return_to=` | Valida origen; 302 a Google o 400 | **RF-4**, **RF-5**, **RF-16** |
| `GET /accounts/login/google/callback` | Callback OAuth; set-cookie + 302 a `return_to` (o `return_to` + `login_error=1`); state inválido → `http://localhost` | **RF-6**–**RF-10**, **RF-17**, **RF-21**, **RF-22** |
| `GET /accounts/session` | JSON sesión plano; lee cookie `fes_session`; CORS con credenciales | **RF-11**, **RF-12**, **RF-15**, **RF-18** |
| `POST /accounts/logout` | Invalida sesión; borra cookie; **204 No Content** (sin body) | **RF-13**, **RF-14**, **RF-19** |

Contrato de sesión (alineado con consumidores tienda/panel vía cookie compartida):

- No autenticado: `{ "authenticated": false }`
- Autenticado: `{ "authenticated": true, "id": "<uuid>", "email": "...", "name": "..." }`

Parámetro de retorno: `return_to` (origen completo permitido, coherente con client-web / panel-api). Validación: el valor debe coincidir exactamente con uno de los orígenes de `BROWSER_ORIGINS` (esquema + host [+ puerto]).

Indicador de fallo en retorno (**RF-10**): query `login_error=1` añadida al `return_to` (sin body HTML de error).

Cookie `fes_session` (NFR + **RF-8**, **RF-14**, **RF-15**):

- HttpOnly, SameSite=Lax, Path=/, Domain=`SESSION_COOKIE_DOMAIN`.
- Secure según `SESSION_COOKIE_SECURE` (false en Minikube HTTP).
- Valor = id de sesión opaco (UUID); no JWT con secretos de cuenta.
- Al logout: `Max-Age=0` / expires pasado con mismos Domain/Path/atributos.

CORS (NFR + **RF-15**): `WebMvcConfigurer` con `allowedOrigins` = lista de `BROWSER_ORIGINS`, `allowCredentials=true`, métodos/headers necesarios para `GET /accounts/session` y `POST /accounts/logout`. Solo esos orígenes.

Manejo de errores HTTP: 400 en inicio de login inválido (**RF-5**); callback siempre redirige (éxito, `login_error`, o `http://localhost` si el state es inválido); no exponer secretos ni stack traces al cliente.

## Integración Google (`GoogleOAuthClient`)

Usar `google-api-client` del `pom.xml` en la clase concreta `GoogleOAuthClient`:

- Inicio: `GoogleAuthorizationCodeRequestUrl` con client id, redirect URI `{PUBLIC_API_BASE_URL}/accounts/login/google/callback`, scopes `openid email profile`, `state` Base64 opaco (`OAuthStateCodec`: `UUID|returnTo`, **no firmado**).
- Callback: intercambiar `code` con `GoogleAuthorizationCodeTokenRequest`; verificar id_token con `GoogleIdTokenVerifier` (audience = client id); leer `sub`, email, name → `GoogleProfile`.
- Secretos solo desde env (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`); nunca en frontends ni logs.
- Sin allowlist: éxito de Google ⇒ entrada permitida (**RF-3**).

El servicio es dueño único de cuenta y sesión; no implementa la puerta `/panel/login/google` (fuera de alcance; pertenece a panel-api).

## Configuración

`@ConfigurationProperties(prefix = "fes.auth")` → `AuthProperties`, enlazado a:

| Variable | Uso | RF / NFR |
|---|---|---|
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Cliente OAuth | NFR, docs |
| `SESSION_COOKIE_DOMAIN` | Domain de `fes_session` | NFR, **RF-15** |
| `BROWSER_ORIGINS` | Orígenes permitidos para `return_to` y CORS | **RF-4**, **RF-5**, NFR |
| `PUBLIC_API_BASE_URL` | Base pública del API (redirect URI OAuth) | NFR, **RF-17** |
| `SESSION_COOKIE_SECURE` | Atributo Secure de la cookie (`false` en Minikube HTTP) | NFR |

Documentación operativa: mantener `docs/google-oauth.md` como fuente del procedimiento; no duplicar secretos en código.

## Estructura de paquetes (implementada)

```
com.friendlyeshop.account
  AccountApiApplication
  controller
    GoogleLoginController
    SessionController
  service
    GoogleLoginService      (start / complete)
    SessionService          (read / logout)
  client
    oauth/ GoogleOAuthClient, TokenExchange, HttpTokenExchange, OAuthStateCodec
  http
    cookie/ SessionCookieWriter
  model
    Account, Session
    dto/ SessionResponse, CompletedLogin, GoogleProfile
  repository
    AccountRepository, SessionRepository
  config
    AuthProperties, AuthConfig, CorsConfig
```

Controladores delgados: HTTP ↔ servicios; la cookie se escribe/borra en `http/cookie/SessionCookieWriter` desde el controlador.

## Pruebas (criterios de finalización)

Cobertura automatizada de los RF:

- Unitarios de modelo/servicios: upsert cuenta (**RF-6**, **RF-7**, **RF-21**), multi-sesión (**RF-22**), invalidación/caducidad, validación de `return_to` (**RF-4**, **RF-5**).
- Web (MockMvc): rutas **RF-16**–**RF-19**, 400 sin redirect, callback con error → redirect + `login_error`, set/clear cookie, logout 204.
- Integración con DB de test: migraciones, unicidad `google_sub`, sesión compartida por cookie.
- `GoogleOAuthClient` con `TokenExchange` top-level inyectable/fake en tests (sin red en CI); `HttpTokenExchange` separado para la implementación real.
- `./mvnw verify` (Checkstyle + tests) en verde.

## Orden de implementación (seguido)

1. Migración y entidades JPA Account/Session + Spring Data repos.
2. Properties (`PUBLIC_API_BASE_URL`, `SESSION_COOKIE_SECURE`, …), CORS y `http/cookie/SessionCookieWriter`.
3. `SessionService` + endpoints session/logout (**RF-11**–**RF-15**, **RF-18**, **RF-19**).
4. `GoogleOAuthClient` + `GoogleLoginService` + endpoints login (**RF-3**–**RF-10**, **RF-16**, **RF-17**, **RF-21**, **RF-22**).
5. **RF-1**, **RF-2** y tests de aceptación.
6. Demo manual del flujo principal (T23; criterios de finalización de la spec).

## Fuera de alcance (confirmado)

Pantallas tienda/panel, puerta panel-api, publicación infra, otros métodos de login, roles comprador/vendedor, borrado/fusión de cuentas, Kafka.

## Mapa RF → secciones del plan

| RF | Cubierto en |
|---|---|
| RF-1 | Modelo Account; persistencia; dueño único |
| RF-2 | Unicidad `google_sub`; `GoogleLoginService.complete` upsert |
| RF-3 | `GoogleLoginService.complete`; `GoogleOAuthClient` sin allowlist |
| RF-4 | `GoogleLoginService.start`; `GET .../login/google` |
| RF-5 | `GoogleLoginService.start` validación → 400 |
| RF-6 | `GoogleLoginService.complete` creación de cuenta |
| RF-7 | `GoogleLoginService.complete` reutilización |
| RF-8 | `GoogleLoginService.complete` + cookie `fes_session` |
| RF-9 | Redirect a `return_to` tras éxito |
| RF-10 | Callback fallo → redirect + `login_error=1` |
| RF-11 | `SessionService.read` no autenticado |
| RF-12 | `SessionService.read` autenticado (JSON plano id, email, name) |
| RF-13 | `SessionService.logout` invalidación |
| RF-14 | Logout borrado de cookie |
| RF-15 | Cookie de dominio padre; misma sesión tienda/panel; CORS |
| RF-16 | Endpoint inicio login |
| RF-17 | Endpoint callback (`PUBLIC_API_BASE_URL`) |
| RF-18 | Endpoint sesión |
| RF-19 | Endpoint logout (204) |
| RF-21 | Actualización email/nombre en reuso |
| RF-22 | Nueva sesión sin invalidar anteriores |
