# Plan 001 — Sesión Google compartida

Desglose técnico de `spec.md` para `account-api`. No implementa la funcionalidad; solo define el diseño respetando Clean Architecture, las prácticas Spring Boot del proyecto y el código existente.

## Estado actual relevante

- Spring Boot 4.1 / Java 25, paquete `com.friendlyeshop.account`.
- `GET /accounts` ya existe en `AccountController` y debe conservarse (**RF-20**).
- Esquema Flyway `V1__create_accounts.sql`: `accounts` con `email`, `password_hash` y `role` obligatorios; sin identidad Google ni sesiones.
- Dependencias ya presentes: Web, JPA, Flyway, Validation, `google-api-client` / `google-http-client-gson`. No hay Spring Security OAuth2 Client; el flujo OAuth se implementará con el cliente Google ya declarado.
- Configuración vía variables de entorno (infra inyectará `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `SESSION_COOKIE_DOMAIN`, `BROWSER_ORIGINS`). Procedimiento operativo en `docs/google-oauth.md`.
- Hibernate `ddl-auto: validate`; solo migraciones Flyway nuevas.

## Principios de diseño

- **Clean Architecture**: dependencias hacia adentro. Entidades y casos de uso sin HTTP, JPA ni APIs de Google. Puertos (interfaces) en el núcleo; adaptadores en el exterior.
- **Separación auth / cuentas** (`AGENTS.md`): controladores delgados; login/logout/sesión distintos de la gestión de cuenta; la cuenta es la fuente de verdad de identidad.
- **Spring Boot**: inyección por constructor, `@ConfigurationProperties` tipadas, DTOs en la capa web (nunca entidades JPA expuestas), `@Transactional` en casos de uso que mutan, tests con JUnit 5 / Mockito y slices o `@SpringBootTest` según capa.
- **Organización por capacidad** bajo `com.friendlyeshop.account` (p. ej. `account`, `session`, `auth.google`, `config`), no por capas técnicas planas.
- Mensajes visibles a la persona en español; nombres de código y documentación técnica en inglés.
- No almacenar ni exponer secretos de autenticación en claro; no persistir tokens de Google como contraseña.

## Arquitectura por capas

```
Frameworks/Drivers     → Spring MVC, JPA/Flyway, google-api-client, cookies HTTP
Interface Adapters     → Controllers, Cookie writer, JPA repos, Google OAuth gateway, CORS
Application (use cases)→ StartGoogleLogin, CompleteGoogleLogin, GetSession, Logout
Domain                 → Account, Session (+ reglas: una cuenta por google_sub, caducidad 30 días)
```

### Dominio

- **Account** (**RF-1**, **RF-2**): identidad propia del servicio; clave de negocio `googleSubject` (subject de Google, único); `id` UUID; `email`; `displayName` (nombre); timestamps. Una sola cuenta por persona de Google; no existe una segunda cuenta paralela para tienda/panel.
- **Session** (**RF-8**, **RF-13**, **RF-15**, **RF-22**, NFR caducidad): id opaco (valor de cookie); `accountId`; `createdAt`; `expiresAt` (30 días desde creación); estado válido vs invalidado. Varias sesiones activas por cuenta permitidas; un login nuevo no invalida las anteriores (**RF-22**).

### Puertos (aplicación)

- `AccountRepository`: buscar por `googleSubject`, guardar/actualizar.
- `SessionRepository`: crear, buscar por id (solo válidas/no expiradas), invalidar por id.
- `GoogleAuthClient`: construir URL de autorización; intercambiar `code` y verificar identidad (subject, email, name). Fallos de Google se modelan como resultado de error, no como excepciones de framework filtradas al dominio.
- `Clock` / tiempo inyectable para caducidad (testeable).
- `AuthProperties` (valores ya validados): client id/secret, cookie domain, orígenes permitidos, flag Secure según entorno HTTPS.

### Casos de uso

1. **StartGoogleLogin** (**RF-4**, **RF-5**, **RF-16**): valida `return_to` contra orígenes de `BROWSER_ORIGINS`; si falta o no es origen permitido → error de validación (HTTP 400, sin ir a Google). Si ok → genera `state` ligado al `return_to` (y nonce CSRF) y URL de redirección a Google.
2. **CompleteGoogleLogin** (**RF-3**, **RF-6**, **RF-7**, **RF-8**, **RF-9**, **RF-10**, **RF-21**, **RF-22**): valida `state`/`code`; ante fallo/cancelación/rechazo de Google → no crea sesión y ordena redirección al `return_to` con indicador de URL `login_error=1` (sin página de error propia). Ante éxito → upsert de cuenta por `googleSubject` (crear si no existe; reutilizar si existe; actualizar email/nombre si Google los trae distintos); **crear una sesión nueva** sin tocar sesiones previas; devolver redirección al `return_to` y datos para emitir cookie `fes_session`.
3. **GetSession** (**RF-11**, **RF-12**, **RF-15**, **RF-18**): lee id de sesión desde cookie; si ausente, inválida o caducada → `{ authenticated: false }`; si válida → `{ authenticated: true, id, email, name }` de la cuenta.
4. **Logout** (**RF-13**, **RF-14**, **RF-19**): si hay id de sesión, invalidarla; siempre indicar borrado de cookie `fes_session` en la respuesta.

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

Entidades JPA / repositorios Spring Data solo en adaptadores; mapeo a/desde el dominio en el gateway de persistencia.

## API HTTP (adaptadores web)

Base path `/accounts`. Separar controladores: conservar el de cuentas; añadir uno de autenticación/sesión.

| Método y ruta | Comportamiento | RF |
|---|---|---|
| `GET /accounts` | Sin cambio de contrato actual | **RF-20** |
| `GET /accounts/login/google?return_to=` | Valida origen; 302 a Google o 400 | **RF-4**, **RF-5**, **RF-16** |
| `GET /accounts/login/google/callback` | Callback OAuth; set-cookie + 302 a `return_to` (o `return_to` + `login_error=1`) | **RF-6**–**RF-10**, **RF-17**, **RF-21**, **RF-22** |
| `GET /accounts/session` | JSON sesión; lee cookie `fes_session`; CORS con credenciales | **RF-11**, **RF-12**, **RF-15**, **RF-18** |
| `POST /accounts/logout` | Invalida sesión; borra cookie; respuesta coherente | **RF-13**, **RF-14**, **RF-19** |

Contrato de sesión (alineado con consumidores tienda/panel vía cookie compartida):

- No autenticado: `{ "authenticated": false }`
- Autenticado: `{ "authenticated": true, "id": "<uuid>", "email": "...", "name": "..." }`

Parámetro de retorno: `return_to` (origen completo permitido, coherente con client-web / panel-api). Validación: el valor debe coincidir exactamente con uno de los orígenes de `BROWSER_ORIGINS` (esquema + host [+ puerto]).

Indicador de fallo en retorno (**RF-10**): query `login_error=1` añadida al `return_to` (sin body HTML de error).

Cookie `fes_session` (NFR + **RF-8**, **RF-14**, **RF-15**):

- HttpOnly, SameSite=Lax, Path=/, Domain=`SESSION_COOKIE_DOMAIN`.
- Secure solo si el despliegue es HTTPS; en Minikube (HTTP) Secure=false.
- Valor = id de sesión opaco (UUID); no JWT con secretos de cuenta.
- Al logout: `Max-Age=0` / expires pasado con mismos Domain/Path/atributos.

CORS (NFR + **RF-15**): `WebMvcConfigurer` con `allowedOrigins` = lista de `BROWSER_ORIGINS`, `allowCredentials=true`, métodos/headers necesarios para `GET /accounts/session` y `POST /accounts/logout`. Solo esos orígenes.

Manejo de errores HTTP: 400 en inicio de login inválido (**RF-5**); callback siempre redirige (éxito o `login_error`); no exponer secretos ni stack traces al cliente.

## Integración Google (adaptador)

Usar `google-api-client` ya en el `pom.xml`:

- Inicio: `GoogleAuthorizationCodeRequestUrl` (o `GoogleAuthorizationCodeFlow`) con client id, redirect URI `{origen público de api}/accounts/login/google/callback`, scopes `openid email profile`, `state` firmado/opaco que recupere `return_to`.
- Callback: intercambiar `code` con `GoogleAuthorizationCodeTokenRequest` / flow; verificar id_token con `GoogleIdTokenVerifier` (audience = client id); leer `sub`, email, name.
- Secretos solo desde env (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`); nunca en frontends ni logs.
- Sin allowlist: éxito de Google ⇒ entrada permitida (**RF-3**).

El servicio es dueño único de cuenta y sesión; no implementa la puerta `/panel/login/google` (fuera de alcance; pertenece a panel-api).

## Configuración

`@ConfigurationProperties` (p. ej. prefijo `fes.auth` / `fes.session`) enlazado a:

| Variable | Uso | RF / NFR |
|---|---|---|
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Cliente OAuth | NFR, docs |
| `SESSION_COOKIE_DOMAIN` | Domain de `fes_session` | NFR, **RF-15** |
| `BROWSER_ORIGINS` | Orígenes permitidos para `return_to` y CORS | **RF-4**, **RF-5**, NFR |
| Flag o detección HTTPS | Atributo Secure de la cookie | NFR |

Documentación operativa: mantener `docs/google-oauth.md` como fuente del procedimiento; no duplicar secretos en código.

## Estructura de paquetes propuesta

```
com.friendlyeshop.account
  AccountApiApplication
  account
    domain / application / adapter.persistence / adapter.web (GET /accounts)
  session
    domain / application / adapter.persistence
  auth.google
    application (Start/Complete) / adapter.web / adapter.google
  session.web (GetSession, Logout + cookie helper)
  config (properties, CORS, beans)
```

Controladores delgados: traducen HTTP ↔ request/response de casos de uso; la cookie se escribe/borra en el adaptador web a partir de la orden del caso de uso.

## Pruebas (criterios de finalización)

Automatizar cobertura de todos los RF (sin aclaraciones abiertas):

- Unitarios de dominio/casos de uso: upsert cuenta (**RF-6**, **RF-7**, **RF-21**), multi-sesión (**RF-22**), invalidación/caducidad, validación de `return_to` (**RF-4**, **RF-5**).
- Web (`MockMvc` / `@WebMvcTest` o slice equivalente): rutas **RF-16**–**RF-20**, 400 sin redirect, callback con error → redirect + `login_error`, set/clear cookie.
- Integración con DB de test (Testcontainers o perfil de test del proyecto): migraciones, unicidad `google_sub`, sesión compartida por cookie.
- Stub/fake del puerto Google para no llamar a la red en CI.
- Conservar/adaptar `AccountControllerTest` para **RF-20**.
- Tras implementar: `./mvnw verify` (Checkstyle + tests).

## Orden de implementación sugerido

1. Migración y modelo de persistencia Account/Session.
2. Properties, CORS y helper de cookie.
3. Casos de uso GetSession / Logout + endpoints (**RF-11**–**RF-15**, **RF-18**, **RF-19**).
4. Adaptador Google + StartGoogleLogin / CompleteGoogleLogin + endpoints (**RF-3**–**RF-10**, **RF-16**, **RF-17**, **RF-21**, **RF-22**).
5. Asegurar **RF-1**, **RF-2**, **RF-20** y batería de tests de aceptación.
6. Demo manual del flujo principal (criterios de finalización de la spec).

## Fuera de alcance (confirmado)

Pantallas tienda/panel, puerta panel-api, publicación infra, otros métodos de login, roles comprador/vendedor, borrado/fusión de cuentas, Kafka.

## Mapa RF → secciones del plan

| RF | Cubierto en |
|---|---|
| RF-1 | Dominio Account; persistencia; dueño único |
| RF-2 | Dominio + unicidad `google_sub`; CompleteGoogleLogin upsert |
| RF-3 | CompleteGoogleLogin; adaptador Google sin allowlist |
| RF-4 | StartGoogleLogin; `GET .../login/google` |
| RF-5 | StartGoogleLogin validación → 400 |
| RF-6 | CompleteGoogleLogin creación de cuenta |
| RF-7 | CompleteGoogleLogin reutilización |
| RF-8 | CompleteGoogleLogin + cookie `fes_session` |
| RF-9 | Redirect a `return_to` tras éxito |
| RF-10 | Callback fallo → redirect + `login_error=1` |
| RF-11 | GetSession no autenticado |
| RF-12 | GetSession autenticado (id, email, name) |
| RF-13 | Logout invalidación |
| RF-14 | Logout borrado de cookie |
| RF-15 | Cookie de dominio padre; misma sesión tienda/panel; CORS |
| RF-16 | Endpoint inicio login |
| RF-17 | Endpoint callback |
| RF-18 | Endpoint sesión |
| RF-19 | Endpoint logout |
| RF-20 | Conservar `GET /accounts` |
| RF-21 | Actualización email/nombre en reuso |
| RF-22 | Nueva sesión sin invalidar anteriores |
