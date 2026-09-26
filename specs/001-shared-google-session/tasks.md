# Tasks 001 — Sesión Google compartida

Implementación paso a paso de `spec.md` / `plan.md`. Cada tarea ~20–30 min. No implementar fuera de este orden de dependencias.

## Persistencia y dominio

- [ ] **T1. Migración Flyway V2 de cuentas Google y sesiones**  
  Cubierta: **RF-1**, **RF-2**.  
  Crear `V2__google_accounts_and_sessions.sql`: `google_sub` UNIQUE NOT NULL, `display_name` NOT NULL, `password_hash` nullable; tabla `sessions` con `id`, `account_id`, `created_at`, `expires_at`, `revoked_at`. No editar V1.  
  **Done when:** `./mvnw -DskipTests flyway:migrate` (o verify de migraciones del perfil de test) aplica V2 sin error y el esquema incluye esas columnas/tabla.

- [ ] **T2. Entidad de dominio Account**  
  Cubierta: **RF-1**, **RF-2**.  
  Modelo de dominio `Account` (id UUID, `googleSubject`, email, `displayName`, timestamps) sin JPA/HTTP; regla de una cuenta por subject de Google.  
  **Done when:** existe la clase de dominio con tests unitarios que confirman identidad por `googleSubject` y que no hay account paralelo tienda/panel.

- [ ] **T3. Entidad de dominio Session**  
  Cubierta: **RF-8**, **RF-13**, **RF-22**.  
  Modelo `Session` (id opaco, `accountId`, `createdAt`, `expiresAt` a 30 días, estado válido/invalidado); varias sesiones por cuenta permitidas.  
  **Done when:** tests unitarios cubren creación con caducidad 30 días, invalidación y que una sesión nueva no implica invalidar otra de la misma cuenta.

- [ ] **T4. Puertos AccountRepository y SessionRepository**  
  Cubierta: **RF-1**, **RF-2**, **RF-8**, **RF-13**.  
  Interfaces de aplicación: buscar/guardar Account por `googleSubject`; crear/buscar sesión válida/no expirada; invalidar por id.  
  **Done when:** los puertos compilan en el núcleo de aplicación sin dependencias Spring/JPA y están listos para stub en tests.

- [ ] **T5. Adaptador JPA de Account**  
  Cubierta: **RF-1**, **RF-2**, **RF-6**, **RF-7**, **RF-21**.  
  Entidad/repositorio Spring Data + gateway que mapea a dominio; unicidad de `google_sub`; upsert de email/`displayName`.  
  **Done when:** test de integración (perfil test / Testcontainers) crea, busca por `googleSubject` y actualiza email/nombre sin duplicar filas.

- [ ] **T6. Adaptador JPA de Session**  
  Cubierta: **RF-8**, **RF-13**, **RF-22**.  
  Persistencia de sesiones; consulta activa = id + `revoked_at IS NULL` + `expires_at > now()`; crear sin revocar previas.  
  **Done when:** test de integración crea dos sesiones de la misma cuenta, invalida una y la otra sigue válida.

## Configuración y adaptadores transversales

- [ ] **T7. ConfigurationProperties de auth/sesión**  
  Cubierta: **RF-4**, **RF-5**, **RF-15**.  
  `@ConfigurationProperties` enlazado a `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `SESSION_COOKIE_DOMAIN`, `BROWSER_ORIGINS` y flag Secure (HTTPS vs Minikube HTTP). Secretos no logueados.  
  **Done when:** con env de test las properties se inyectan; falta de orígenes/client id falla el arranque o la validación de forma explícita (sin secretos en logs).

- [ ] **T8. Helper de cookie `fes_session`**  
  Cubierta: **RF-8**, **RF-14**, **RF-15**.  
  Escritura/borrado HttpOnly, SameSite=Lax, Path=/, Domain=`SESSION_COOKIE_DOMAIN`; Secure solo en HTTPS; logout con Max-Age=0 mismos atributos.  
  **Done when:** test unitario o de slice web verifica atributos de set-cookie y clear-cookie según properties HTTP vs Secure.

- [ ] **T9. CORS con credenciales para orígenes configurados**  
  Cubierta: **RF-15**.  
  `WebMvcConfigurer`: `allowedOrigins` = `BROWSER_ORIGINS`, `allowCredentials=true`, métodos/headers para session y logout.  
  **Done when:** test MockMvc/CORS confirma origen permitido con credentials y rechazo (o ausencia de ACAO) para origen no listado.

## Sesión y logout (antes del login Google)

- [ ] **T10. Caso de uso GetSession**  
  Cubierta: **RF-11**, **RF-12**, **RF-15**.  
  Lee id de cookie; ausente/inválida/caducada → no autenticado; válida → id, email, name de la cuenta.  
  **Done when:** tests unitarios con repos stub cubren ambos resultados JSON de dominio/DTO de aplicación.

- [ ] **T11. Caso de uso Logout**  
  Cubierta: **RF-13**, **RF-14**.  
  Si hay id de sesión, invalidarla; siempre ordenar borrado de cookie `fes_session`.  
  **Done when:** tests unitarios invalidan cuando hay id y siempre emiten orden de clear-cookie; sin id no fallan.

- [ ] **T12. Endpoints GET /accounts/session y POST /accounts/logout**  
  Cubierta: **RF-11**, **RF-12**, **RF-13**, **RF-14**, **RF-18**, **RF-19**.  
  Controlador delgado de sesión/auth; contrato JSON `{authenticated:false}` / `{authenticated:true,id,email,name}`; set/clear cookie vía helper.  
  **Done when:** tests `@WebMvcTest` o MockMvc verifican rutas, cuerpos y headers Set-Cookie de logout.

## Login Google

- [ ] **T13. Puerto GoogleAuthClient y fake para tests**  
  Cubierta: **RF-3**.  
  Puerto: URL de autorización; intercambio de `code` + verificación (subject, email, name); errores como resultado, no excepciones de framework al dominio. Fake/stub sin red.  
  **Done when:** el fake puede simular éxito y fallo/cancelación; ningún test de CI llama a la red de Google.

- [ ] **T14. Caso de uso StartGoogleLogin**  
  Cubierta: **RF-4**, **RF-5**.  
  Valida `return_to` contra orígenes exactos de `BROWSER_ORIGINS`; inválido/ausente → error de validación; ok → `state` ligado a `return_to` + URL a Google.  
  **Done when:** tests unitarios: origen permitido genera URL; origen inválido o ausente no llama a Google y devuelve error de validación.

- [ ] **T15. Endpoint GET /accounts/login/google**  
  Cubierta: **RF-4**, **RF-5**, **RF-16**.  
  Adaptador web: 302 a Google o 400 sin redirección; parámetro `return_to`.  
  **Done when:** MockMvc: `return_to` permitido → 302 Location hacia Google; ausente/no permitido → 400 y sin Location a Google.

- [ ] **T16. Caso de uso CompleteGoogleLogin — upsert de cuenta**  
  Cubierta: **RF-3**, **RF-6**, **RF-7**, **RF-21**.  
  Tras identidad Google válida: crear si no existe; reutilizar si existe; actualizar email/nombre si cambian; sin allowlist de dominios.  
  **Done when:** tests unitarios: primer login crea; segundo reutiliza mismo id; cambio de email/nombre actualiza; cualquier subject de Google se acepta.

- [ ] **T17. Caso de uso CompleteGoogleLogin — sesión, redirect y error**  
  Cubierta: **RF-8**, **RF-9**, **RF-10**, **RF-22**.  
  Éxito: nueva sesión sin invalidar previas + redirect a `return_to` + datos cookie. Fallo/cancel/rechazo: sin sesión + redirect a `return_to` con `login_error=1`.  
  **Done when:** tests unitarios cubren set de sesión nueva coexistiendo con anterior, redirect de éxito, y redirect con `login_error=1` sin crear sesión.

- [ ] **T18. Adaptador Google real (google-api-client)**  
  Cubierta: **RF-3**, **RF-17**.  
  Implementación con `GoogleAuthorizationCodeRequestUrl` / token request / `GoogleIdTokenVerifier`; redirect URI `{api}/accounts/login/google/callback`; scopes openid email profile; secretos solo desde env.  
  **Done when:** bean cableado; test con fake/verificador mock confirma lectura de `sub`/email/name; no hay secretos en código ni logs de prueba.

- [ ] **T19. Endpoint GET /accounts/login/google/callback**  
  Cubierta: **RF-6**, **RF-7**, **RF-8**, **RF-9**, **RF-10**, **RF-17**, **RF-21**, **RF-22**.  
  Controlador delgado: Set-Cookie `fes_session` en éxito; 302 a `return_to` o `return_to?login_error=1`; sin página HTML de error.  
  **Done when:** MockMvc con Google stub: éxito → 302 + Set-Cookie; fallo → 302 con `login_error=1` y sin cookie de sesión nueva.

## Conservación, aceptación y cierre

- [ ] **T20. Conservar GET /accounts sin cambio de contrato**  
  Cubierta: **RF-20**.  
  Asegurar que `AccountController` y su test existentes siguen verdes; no retirar la ruta.  
  **Done when:** `AccountControllerTest` (o equivalente) pasa y `GET /accounts` responde igual que antes de este corte.

- [ ] **T21. Tests de aceptación de unicidad y sesión compartida**  
  Cubierta: **RF-1**, **RF-2**, **RF-15**, **RF-22**.  
  Integración: una sola fila por `google_sub`; cookie de dominio padre; dos consultas de sesión ven la misma sesión activa; logout invalida para ambos consumidores lógicos.  
  **Done when:** suite de integración en verde cubre unicidad, multi-sesión por cuenta y consulta autenticada vía cookie.

- [ ] **T22. Batería final `./mvnw verify` y checklist de RF**  
  Cubierta: **RF-1** … **RF-22**.  
  Ejecutar verify (Checkstyle + tests); revisar que cada RF tiene al menos una prueba automatizada verde según criterios de finalización.  
  **Done when:** `./mvnw verify` pasa y la checklist RF-1–RF-22 está marcada como cubierta por tests (sin RF abiertos).

- [ ] **T23. Demostración manual del flujo principal**  
  Cubierta: **RF-3**, **RF-4**, **RF-6**, **RF-7**, **RF-8**, **RF-9**, **RF-12**, **RF-13**, **RF-14**, **RF-16**, **RF-18**, **RF-19**, **RF-20**.  
  Flujo: login con `return_to` permitido → sesión abierta → GET session (id/email/name) → logout borra cookie; comprobar primer alta, no duplicado, cualquier Google, `GET /accounts` intacto; sin puerta panel en este servicio.  
  **Done when:** demo documentada (pasos y resultado) cumple los criterios de finalización de la spec.
