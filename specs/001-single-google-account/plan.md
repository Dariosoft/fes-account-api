# Plan técnico — Spec 001: Cuenta única con Google

Desglose técnico de `specs/001-single-google-account/spec.md` para `account-api`.
No hay `docs/constitution.md` en este proyecto; este plan se alinea con `AGENTS.md` salvo donde la spec prevalece (sin contraseñas ni roles en este corte).

---

## 1. Objetivo técnico

Convertir `account-api` en la **única fuente de verdad** de la identidad compartida entre tienda y panel:

- Persistencia de una sola cuenta por correo de Google (sin segunda cuenta de vendedor, sin roles).
- Entrada y salida de la **tienda** vía Google en este servicio.
- Consulta de identidad autenticada para la tienda (nombre + correo).
- Reconocimiento de la misma cuenta hacia el **panel** por correo, **sin** resolver la entrada del panel aquí.
- Una sola sesión compartida: entrar en tienda (o que el panel la abra por su propio flujo) implica la misma permanencia; salir desde cualquiera la cierra en ambas.

**Historias cubiertas:** H1–H4.

---

## 2. Estado actual relevante

| Pieza | Hoy | Implicación del corte |
| --- | --- | --- |
| `AccountController` (`GET /accounts`) | Stub “ready” | Sustituir/ampliar con endpoints de auth e identidad |
| `V1__create_accounts.sql` | `email`, `password_hash`, `role` | La spec **no** exige contraseña ni roles; migrar el esquema |
| `pom.xml` | Web, JPA, Flyway, AMQP, Actuator | Añadir Spring Security / cliente OAuth2 Google según diseño |
| Tests | Solo stub del controlador | Cubrir RF-1…RF-13 con pruebas automatizadas |

---

## 3. Modelo de dominio y persistencia

### 3.1 Cuenta (identidad única)

**Cubre: RF-1, RF-2, RF-9**

- Entidad `Account` (paquete por dominio, p. ej. `com.friendlyeshop.account`):
  - Identificador estable (`UUID`).
  - Correo de Google: único, normalizado (p. ej. minúsculas), clave de unicidad.
  - Nombre visible (proveniente del perfil Google al crear/actualizar en login).
  - Marcas de tiempo (`created_at`; opcional `updated_at` al refrescar perfil).
- **No** modelar: `password_hash`, `role`, distinción comprador/vendedor, membresía de panel.
- Este servicio es el único dueño de la fila de cuenta; no duplicar identidades para el panel.

### 3.2 Migración Flyway

**Cubre: RF-1, RF-2**

- Nueva migración `V2__…` (no editar `V1` ya aplicada):
  - Eliminar o dejar de usar columnas `password_hash` y `role` (y el índice `accounts_role_idx`).
  - Asegurar `email` UNIQUE y campo de nombre (p. ej. `display_name` / `name`).
  - Hibernate solo valida (`ddl-auto: validate`); el esquema lo mueve Flyway.
- Datos locales de desarrollo: si hay filas con el esquema viejo, la migración debe ser segura (drop columnas / recreate según entorno vacío típico).

### 3.3 Sesión compartida

**Cubre: RF-5, RF-12, RF-13** (y soporte de RF-6, RF-8)

- Modelo de **una sesión por persona autenticada**, compartida entre tienda y panel:
  - Identificador de sesión opaco (token / id de sesión).
  - Referencia a `account_id`.
  - Persistencia server-side (tabla `sessions` o equivalente) **sin caducidad automática** (RNF: permanece hasta salir).
- Entrar (tienda vía este servicio) crea o reutiliza esa sesión y la asocia a la cuenta.
- Salir (desde tienda o desde panel, invocando este servicio) invalida la sesión; peticiones posteriores se tratan como sin acceso activo.
- Coordinación con panel: el panel **no** inicia OAuth aquí (RF-10), pero al confirmar identidad / abrir sesión compartida desde su puerta, este servicio debe poder **crear o exponer** la misma sesión (detalle de contrato en §5.3) de modo que RF-12 se cumpla.

---

## 4. Autenticación Google (solo tienda en este servicio)

### 4.1 Dependencias y configuración

**Cubre: RF-3, RF-7**

- Integrar validación de identidad Google (p. ej. Spring Security OAuth2 Client / Resource Server con ID token de Google, o verificación explícita del `id_token` / code exchange).
- Configuración externalizada (`application.yaml` + variables de entorno): client id/secret, redirect URI, issuer de Google. Secretos **nunca** en claro en logs ni respuestas (RNF).
- Aceptar **cualquier** cuenta de Google válida en este corte (sin allowlist ni vinculación previa al panel).

### 4.2 Flujo “pedir entrar” (tienda)

**Cubre: RF-3, RF-4, RF-7, RF-9**

1. La tienda inicia el flujo de entrada contra este servicio.
2. Google valida a la persona.
3. Si la validación **falla o se rechaza** → no crear sesión; no marcar autenticado; mensaje en español (RF-7).
4. Si OK → buscar cuenta por correo de Google:
   - No existe → **crear** cuenta (correo + nombre) y abrir sesión (RF-4).
   - Existe → **reutilizar** la misma cuenta; no crear otra (RF-9); abrir/actualizar sesión.
5. Salida: persona autenticada vinculada a su cuenta (cookie/header de sesión según contrato HTTP elegido).

### 4.3 Fuera de este servicio

**Cubre: RF-10**

- No implementar endpoints ni callbacks de “login panel” como responsabilidad de entrada del panel.
- Otros métodos (email/password, otros IdP): fuera de alcance.

---

## 5. API HTTP (contratos)

Mantener el prefijo `/accounts` y Actuator. Controladores delgados; lógica en servicios. DTOs; no exponer entidades JPA. Mensajes de error visibles en **español**.

### 5.1 Entrada tienda

**Cubre: RF-3, RF-4, RF-7, RF-9**

- Endpoints orientados a iniciar/completar el login con Google (redirect OAuth o intercambio de credencial Google, según el diseño elegido con `client-web`).
- Respuesta de éxito: estado autenticado + datos mínimos de cuenta (o redirect con sesión establecida).
- Respuesta de fallo Google: 401/403 (o redirect de error) sin sesión.

### 5.2 Salida (tienda y panel)

**Cubre: RF-5, RF-12, RF-13**

- Endpoint de logout (p. ej. `POST /accounts/session/logout` o equivalente bajo `/accounts`).
- Idempotente: si no hay sesión, resultado “sin persona autenticada” (caso límite alineado con RF-8).
- Invalida la **única** sesión compartida → tienda y panel quedan sin acceso activo en la siguiente solicitud (RF-5, RF-12, RF-13).

### 5.3 Quién soy (tienda)

**Cubre: RF-6, RF-8**

- Endpoint de sesión actual (p. ej. `GET /accounts/me` o `GET /accounts/session`).
- Con sesión válida: devolver **nombre** y **correo de Google** (RF-6).
- Sin sesión: no presentar a nadie como autenticado — 401 o cuerpo explícito “anónimo”, sin inventar identidad (RF-8).

### 5.4 Reconocimiento para el panel (sin login aquí)

**Cubre: RF-10, RF-11, RF-12**

- Contrato **server-to-server** (o autenticado entre servicios) para que `panel-api` pregunte por una persona **ya conocida por correo de Google**:
  - Si existe cuenta → confirmar misma cuenta; devolver nombre + correo (RF-11).
  - Si no existe → indicar no encontrada (sin crear cuenta desde este camino salvo que el diseño de sesión compartida lo exija al “abrir” sesión desde el panel; la **creación** de cuenta en primer ingreso sigue siendo del flujo Google de tienda o del flujo de confirmación Google del panel vía este reconocimiento — la spec exige confirmación por correo, no login OAuth del panel en este servicio).
- Operación asociada a **sesión compartida**: al autenticarse el panel por su puerta, debe poder **abrir/asociar** la misma sesión en este servicio (RF-12) sin que este servicio sea la “puerta de entrada” OAuth del panel (RF-10). Detallar en implementación: p. ej. “establish session for accountId/email” tras que panel-api valide Google por su lado y consulte account-api.

---

## 6. Capas y estructura de paquetes

Organización por dominio (`com.friendlyeshop.account…`), no por capas genéricas:

| Componente | Responsabilidad | RF |
| --- | --- | --- |
| Controladores auth / session / lookup | HTTP delgado, validación de entrada | RF-3…RF-13 según endpoint |
| `AccountService` | Crear/buscar por email, sin roles | RF-1, RF-2, RF-4, RF-9 |
| `GoogleIdentityService` (o Security filter/adapter) | Validar token/code Google | RF-3, RF-7 |
| `SessionService` | Crear, leer, invalidar sesión única | RF-5, RF-6, RF-8, RF-12, RF-13 |
| Repositorios JPA + migraciones | Persistencia accounts (+ sessions) | RF-1, RF-2 |
| `@ControllerAdvice` | Errores consistentes en español | RNF / RF-7 |
| Config `@ConfigurationProperties` | Client Google, cookies, etc. | RF-3, RNF secretos |

Separar autenticación de gestión de cuentas (regla AGENTS.md).

---

## 7. Seguridad y requisitos no funcionales

**Cubre: RNF; apoya RF-7, RF-12, RF-13**

- Mensajes visibles en español.
- No almacenar ni exponer secretos de autenticación en claro (tokens Google, client secret, session ids en logs).
- Sesión **sin TTL de inactividad/absoluto** en este corte: solo logout invalida.
- Cookies (si se usan): `HttpOnly`, `Secure` en no-local, `SameSite` acorde a tienda/panel en dominios distintos (coordinar con `infra` / frontends; cambios de puerto/ruta en `infra`).
- No añadir Kafka; conservar RabbitMQ preparado (eventos de cuenta/sesión **opcionales** en este corte si no son necesarios para los RF; no bloquear el corte por eventos).

---

## 8. Casos límite → comportamiento esperado

| Caso | Comportamiento | RF |
| --- | --- | --- |
| Primer Google nunca visto | Crear cuenta + sesión | RF-4 |
| Mismo correo otra vez | Reutilizar cuenta | RF-9 |
| Google inválido/cancelado | Sin sesión | RF-7 |
| Logout sin sesión | Sigue sin autenticado | RF-8 |
| “Quién es” sin sesión | Nadie autenticado | RF-8 |
| “Quién es” con sesión | Nombre + correo | RF-6 |
| Login panel como OAuth en account-api | Fuera de alcance | RF-10 |
| Lookup panel por correo | Misma cuenta, nombre + correo | RF-11 |
| Cuenta sin roles | Solo identidad | RF-2 |
| Tras logout, reload/request | Sin acceso activo | RF-13 |
| Sesión única tienda↔panel | Entrar/salir afecta ambas | RF-5, RF-12 |

---

## 9. Pruebas y criterios de finalización

**Cubre: todos los RF (RF-1 … RF-13)**

- Pruebas automatizadas por RF (unitarias de servicios + slice/MVC o integración):
  - Unicidad de cuenta / no roles / no password (RF-1, RF-2).
  - Login Google OK crea o reutiliza (RF-3, RF-4, RF-9).
  - Login Google fail (RF-7).
  - Logout cierra sesión compartida; doble logout idempotente (RF-5, RF-12).
  - Me con/sin sesión (RF-6, RF-8).
  - Ausencia de endpoint de “panel login” OAuth; presencia de lookup por email (RF-10, RF-11).
  - Tras logout, siguientes requests no autenticados (RF-13).
- `./mvnw verify` (Checkstyle + tests) en verde.
- Demostración manual: entrar Google → ver nombre/correo → salir; primer ingreso crea; mismo correo no duplica; sin roles; panel no entra por este servicio.

---

## 10. Fuera de alcance (no planificar implementación)

Pantallas tienda/panel, puerta OAuth del panel, disponibilidad en `infra`, segunda cuenta vendedor, roles, catálogo, compra, otros IdP, caducidad automática de sesión, borrado/fusión de cuentas.

---

## 11. Mapa RF → secciones del plan

| RF | Secciones |
| --- | --- |
| RF-1 | §1, §3.1, §3.2, §6, §9 |
| RF-2 | §3.1, §3.2, §6, §8, §9 |
| RF-3 | §4.1, §4.2, §5.1, §6, §9 |
| RF-4 | §4.2, §5.1, §6, §8, §9 |
| RF-5 | §3.3, §5.2, §6, §8, §9 |
| RF-6 | §3.3, §5.3, §6, §8, §9 |
| RF-7 | §4.1, §4.2, §5.1, §7, §8, §9 |
| RF-8 | §5.2, §5.3, §6, §8, §9 |
| RF-9 | §3.1, §4.2, §5.1, §8, §9 |
| RF-10 | §4.3, §5.4, §8, §9, §10 |
| RF-11 | §5.4, §8, §9 |
| RF-12 | §3.3, §5.2, §5.4, §7, §8, §9 |
| RF-13 | §3.3, §5.2, §7, §8, §9 |

---

## 12. Orden de implementación sugerido

1. Migración de esquema (quitar password/role; nombre; tabla sesión) — RF-1, RF-2.
2. Dominio + repositorios + `AccountService` (find-or-create por email) — RF-1, RF-2, RF-4, RF-9.
3. `SessionService` (crear / invalidar / resolver) — RF-5, RF-6, RF-8, RF-12, RF-13.
4. Integración Google + endpoints login tienda — RF-3, RF-4, RF-7, RF-9.
5. Endpoints me + logout — RF-5, RF-6, RF-8, RF-12, RF-13.
6. Lookup / establish-session para panel — RF-10, RF-11, RF-12.
7. Pruebas por RF + verify + demo — RF-1…RF-13.
