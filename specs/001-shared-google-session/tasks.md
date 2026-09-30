# Tasks 001 — Sesión Google compartida

Implementación paso a paso de `spec.md` / `plan.md`. Cada tarea ~20–30 min. No implementar fuera de este orden de dependencias.

## Persistencia y dominio

- [x] **T1. Migración Flyway V2 de cuentas Google y sesiones**  
  Cubierta: **RF-1**, **RF-2**.  
  Crear `V2__google_accounts_and_sessions.sql`: `google_sub` UNIQUE NOT NULL, `display_name` NOT NULL, `password_hash` nullable; tabla `sessions` con `id`, `account_id`, `created_at`, `expires_at`, `revoked_at`. No editar V1.  
  **Done when:** `./mvnw -DskipTests flyway:migrate` (o verify de migraciones del perfil de test) aplica V2 sin error y el esquema incluye esas columnas/tabla.

- [x] **T2. Entidad Account (JPA en model)**  
  Cubierta: **RF-1**, **RF-2**.  
  Entidad JPA `Account` en `model` (id UUID, `googleSub`, email, `displayName`, timestamps); regla de una cuenta por subject de Google.  
  *Nota implementación:* Layered con entidad JPA (no dominio puro sin JPA del plan Clean Architecture original).  
  **Done when:** existe la clase con tests unitarios que confirman identidad por `googleSub` y que no hay account paralelo tienda/panel.

- [x] **T3. Entidad Session (JPA en model)**  
  Cubierta: **RF-8**, **RF-13**, **RF-22**.  
  Entidad JPA `Session` (id opaco, `accountId`, `createdAt`, `expiresAt` a 30 días, `revoke` / `isValid`); varias sesiones por cuenta permitidas.  
  *Nota implementación:* Layered con entidad JPA (no dominio puro sin JPA).  
  **Done when:** tests unitarios cubren creación con caducidad 30 días, invalidación y que una sesión nueva no implica invalidar otra de la misma cuenta.

- [x] **T4. Repositorios Spring Data AccountRepository y SessionRepository**  
  Cubierta: **RF-1**, **RF-2**, **RF-8**, **RF-13**.  
  Interfaces Spring Data `JpaRepository`: buscar/guardar Account por `googleSub`; CRUD de Session por id; invalidación vía `Session.revoke` en servicio.  
  *Nota implementación:* sin puertos Clean Architecture en un núcleo sin Spring/JPA; repositorios Spring Data directos.  
  **Done when:** los repositorios compilan y están listos para mock/stub en tests de servicio.

- [x] **T5. Persistencia JPA de Account**  
  Cubierta: **RF-1**, **RF-2**, **RF-6**, **RF-7**, **RF-21**.  
  Entidad + `AccountRepository` Spring Data; unicidad de `google_sub`; upsert de email/`displayName` en `GoogleLoginService`.  
  *Nota implementación:* sin gateway dominio↔JPA separado.  
  **Done when:** test de integración (perfil test / Testcontainers) crea, busca por `googleSub` y actualiza email/nombre sin duplicar filas.

- [x] **T6. Persistencia JPA de Session**  
  Cubierta: **RF-8**, **RF-13**, **RF-22**.  
  Persistencia de sesiones; validez = `revoked_at IS NULL` + `expires_at > now()` vía `Session.isValid` en `SessionService`; crear sin revocar previas.  
  *Nota implementación:* sin gateway dominio↔JPA separado.  
  **Done when:** test de integración crea dos sesiones de la misma cuenta, invalida una y la otra sigue válida.

## Configuración y adaptadores transversales

- [x] **T7. ConfigurationProperties de auth/sesión**  
  Cubierta: **RF-4**, **RF-5**, **RF-15**.  
  `@ConfigurationProperties(prefix = "fes.auth")` enlazado a `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `SESSION_COOKIE_DOMAIN`, `BROWSER_ORIGINS`, `PUBLIC_API_BASE_URL` y `SESSION_COOKIE_SECURE` (HTTPS vs Minikube HTTP). Secretos no logueados.  
  **Done when:** con env de test las properties se inyectan; falta de orígenes/client id falla el arranque o la validación de forma explícita (sin secretos en logs).

- [x] **T8. Writer HTTP de cookie `fes_session`**  
  Cubierta: **RF-8**, **RF-14**, **RF-15**.  
  Escritura/borrado HttpOnly, SameSite=Lax, Path=/, Domain=`SESSION_COOKIE_DOMAIN`; Secure según `SESSION_COOKIE_SECURE`; logout con Max-Age=0 mismos atributos.  
  *Nota implementación:* `SessionCookieWriter` vive en `http/cookie`, no en `config`; usa properties, pero su responsabilidad es construir cookies HTTP.  
  **Done when:** test unitario o de slice web verifica atributos de set-cookie y clear-cookie según properties HTTP vs Secure.

- [x] **T9. CORS con credenciales para orígenes configurados**  
  Cubierta: **RF-15**.  
  `WebMvcConfigurer`: `allowedOrigins` = `BROWSER_ORIGINS`, `allowCredentials=true`, métodos/headers para session y logout.  
  **Done when:** test MockMvc/CORS confirma origen permitido con credentials y rechazo (o ausencia de ACAO) para origen no listado.

## Sesión y logout (antes del login Google)

- [x] **T10. SessionService.read (GetSession)**  
  Cubierta: **RF-11**, **RF-12**, **RF-15**.  
  Lee id de cookie; ausente/inválida/caducada → no autenticado; válida → id, email, name de la cuenta (`SessionResponse` JSON plano).  
  *Nota implementación:* método `SessionService.read`, no clase `GetSession` separada.  
  **Done when:** tests unitarios con repos stub cubren ambos resultados JSON.

- [x] **T11. SessionService.logout**  
  Cubierta: **RF-13**, **RF-14**.  
  Si hay id de sesión, invalidarla; el controlador siempre borra cookie `fes_session` y responde **204 No Content** (sin body JSON).  
  *Nota implementación:* método `SessionService.logout`, no clase `Logout` separada.  
  **Done when:** tests unitarios invalidan cuando hay id; sin id no fallan; MockMvc confirma clear-cookie + 204.

- [x] **T12. Endpoints GET /accounts/session y POST /accounts/logout**  
  Cubierta: **RF-11**, **RF-12**, **RF-13**, **RF-14**, **RF-18**, **RF-19**.  
  `SessionController`; contrato JSON plano `{authenticated:false}` / `{authenticated:true,id,email,name}`; logout **204** + clear cookie vía `http/cookie/SessionCookieWriter`.  
  **Done when:** tests `@WebMvcTest` o MockMvc verifican rutas, cuerpos (session), 204 vacío (logout) y headers Set-Cookie de logout.

## Login Google

- [x] **T13. GoogleOAuthClient concreto y fake para tests**  
  Cubierta: **RF-3**.  
  Clase concreta: URL de autorización; intercambio de `code` + verificación (subject, email, name); errores como `Optional.empty()`. Fake/`TokenExchange` stub sin red.  
  *Nota implementación:* sin puerto `GoogleAuthClient` de Clean Architecture; `GoogleOAuthClient`, `TokenExchange` y `HttpTokenExchange` viven en `client/oauth` como integración externa, en archivos separados.  
  **Done when:** el fake puede simular éxito y fallo/cancelación; ningún test de CI llama a la red de Google.

- [x] **T14. GoogleLoginService.start**  
  Cubierta: **RF-4**, **RF-5**.  
  Valida `return_to` contra orígenes exactos de `BROWSER_ORIGINS`; inválido/ausente → vacío → 400; ok → `OAuthStateCodec` Base64 opaco (`UUID|returnTo`, no firmado) + URL a Google.  
  *Nota implementación:* método `GoogleLoginService.start`, no clase `StartGoogleLogin`.  
  **Done when:** tests unitarios: origen permitido genera URL; origen inválido o ausente no llama a Google y devuelve error de validación.

- [x] **T15. Endpoint GET /accounts/login/google**  
  Cubierta: **RF-4**, **RF-5**, **RF-16**.  
  `GoogleLoginController`: 302 a Google o 400 sin redirección; parámetro `return_to`.  
  **Done when:** MockMvc: `return_to` permitido → 302 Location hacia Google; ausente/no permitido → 400 y sin Location a Google.

- [x] **T16. GoogleLoginService.complete — upsert de cuenta**  
  Cubierta: **RF-3**, **RF-6**, **RF-7**, **RF-21**.  
  Tras identidad Google válida: crear si no existe; reutilizar si existe; actualizar email/nombre si cambian; sin allowlist de dominios.  
  *Nota implementación:* método `GoogleLoginService.complete`, no clase `CompleteGoogleLogin`.  
  **Done when:** tests unitarios: primer login crea; segundo reutiliza mismo id; cambio de email/nombre actualiza; cualquier subject de Google se acepta.

- [x] **T17. GoogleLoginService.complete — sesión, redirect y error**  
  Cubierta: **RF-8**, **RF-9**, **RF-10**, **RF-22**.  
  Éxito: nueva sesión sin invalidar previas + redirect a `return_to` + datos cookie. Fallo/cancel/rechazo: sin sesión + redirect a `return_to` con `login_error=1`. State OAuth inválido → redirect a `http://localhost` (comportamiento implementado).  
  **Done when:** tests unitarios cubren set de sesión nueva coexistiendo con anterior, redirect de éxito, y redirect con `login_error=1` sin crear sesión.

- [x] **T18. GoogleOAuthClient real (google-api-client)**  
  Cubierta: **RF-3**, **RF-17**.  
  Implementación con `GoogleAuthorizationCodeRequestUrl` / token request / `GoogleIdTokenVerifier`; redirect URI `{PUBLIC_API_BASE_URL}/accounts/login/google/callback`; scopes openid email profile; secretos solo desde env.  
  *Nota implementación:* `GoogleOAuthClient`, `TokenExchange`, `HttpTokenExchange` y `OAuthStateCodec` viven en `client/oauth`; `OAuthStateCodec` es un mecanismo del protocolo OAuth, no un `util` genérico.  
  **Done when:** bean cableado; test con fake/verificador mock confirma lectura de `sub`/email/name; no hay secretos en código ni logs de prueba.

- [x] **T19. Endpoint GET /accounts/login/google/callback**  
  Cubierta: **RF-6**, **RF-7**, **RF-8**, **RF-9**, **RF-10**, **RF-17**, **RF-21**, **RF-22**.  
  `GoogleLoginController`: Set-Cookie `fes_session` en éxito; 302 a `return_to` o `return_to?login_error=1`; sin página HTML de error.  
  **Done when:** MockMvc con Google stub: éxito → 302 + Set-Cookie; fallo → 302 con `login_error=1` y sin cookie de sesión nueva.

## Conservación, aceptación y cierre

- [x] **T21. Tests de aceptación de unicidad y sesión compartida**  
  Cubierta: **RF-1**, **RF-2**, **RF-15**, **RF-22**.  
  Integración: una sola fila por `google_sub`; cookie de dominio padre; dos consultas de sesión ven la misma sesión activa; logout invalida para ambos consumidores lógicos.  
  **Done when:** suite de integración en verde cubre unicidad, multi-sesión por cuenta y consulta autenticada vía cookie.

- [x] **T22. Batería final `./mvnw verify` y checklist de RF**  
  Cubierta: **RF-1** … **RF-19**, **RF-21**, **RF-22**.  
  Ejecutar verify (Checkstyle + tests); revisar que cada RF tiene al menos una prueba automatizada verde según criterios de finalización.  
  **Done when:** `./mvnw verify` pasa y la checklist de RF vigentes está marcada como cubierta por tests (sin RF abiertos).

  RF coverage (automated, `./mvnw verify` green — 42 tests):
  - RF-1 / RF-2: Account entity + JPA uniqueness + acceptance
  - RF-3: GoogleLoginService.complete upsert accepts any subject; GoogleOAuthClient
  - RF-4 / RF-5 / RF-16: GoogleLoginService.start + GoogleLoginControllerTest
  - RF-6 / RF-7 / RF-21: GoogleLoginService upsert tests
  - RF-8 / RF-9 / RF-10 / RF-17 / RF-22: GoogleLoginService session/redirect tests + callback MockMvc
  - RF-11 / RF-12 / RF-18: SessionService.read + SessionControllerTest
  - RF-13 / RF-14 / RF-19: SessionService.logout + SessionControllerTest (204)
  - RF-15: CORS + acceptance shared cookie across origins

- [ ] **T23. Demostración manual del flujo principal**  
  Cubierta: **RF-3**, **RF-4**, **RF-6**, **RF-7**, **RF-8**, **RF-9**, **RF-12**, **RF-13**, **RF-14**, **RF-16**, **RF-18**, **RF-19**.  
  Flujo: login con `return_to` permitido → sesión abierta → GET session (id/email/name) → logout **204** borra cookie; comprobar primer alta, no duplicado, cualquier Google; sin puerta panel en este servicio.  
  **Done when:** demo documentada (pasos y resultado) cumple los criterios de finalización de la spec.
