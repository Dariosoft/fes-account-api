# UML 002 — Tienda pública del vendedor

Diagramas del diseño descrito en `plan.md` / `tasks.md` para `account-api`, en la rama `002/feat-account-shop`. La organización es **Layered** (`controller` / `service` / `repository` / `model` (+ `model/dto`) / `config` / `exception`) con extensiones ya existentes `http/cookie` donde se reutiliza `SessionCookieWriter`. El dominio Shop se aísla por nombres de clase (`Shop*`) sin crear paquetes por capacidad.

## Flujo: guardar y consultar la tienda

El vendedor guarda su tienda con `PUT /accounts/shop` enviando la cookie `fes_session`; el servicio resuelve la cuenta con `SessionService` y hace upsert en `shops`. Un visitante anónimo consulta `GET /accounts/shops/{accountId}` sin cookie. `GET /accounts/shop` devuelve la tienda de la cuenta autenticada. La respuesta pública solo contiene `name` y `description` (nunca `email`).

```mermaid
sequenceDiagram
    actor Seller as Vendedor (navegador)
    actor Visitor as Visitante anónimo
    participant Controller as ShopController
    participant ShopService
    participant SessionService
    participant SessionRepo as SessionRepository
    participant AccountRepo as AccountRepository
    participant ShopRepo as ShopRepository

    Seller->>Controller: PUT /accounts/shop (Cookie fes_session)\n{name, description|null}
    Controller->>Controller: valida ShopRequest (@Valid, trim, blank→null)
    alt request inválido
        Controller-->>Seller: 400 VALIDATION_ERROR (mensaje en español)
    else request válido
        Controller->>ShopService: saveOwnShop(sessionId, name, description)
        ShopService->>SessionService: read(sessionId)
        SessionService->>SessionRepo: findById
        SessionService->>AccountRepo: findById (si sesión válida)
        SessionService-->>ShopService: SessionResponse(authenticated, id)
        alt sin sesión válida
            ShopService-->>Controller: NotAuthenticatedException
            Controller-->>Seller: 401 AUTH_REQUIRED
        else autenticado
            ShopService->>ShopRepo: findByAccountId(accountId)
            alt no existe tienda
                ShopService->>ShopRepo: save(Shop.open(...))
            else tienda existente
                ShopService->>ShopRepo: save(Shop.updateDetails(...))
            end
            ShopService-->>Controller: ShopResponse(name, description)
            Controller-->>Seller: 200 {name, description|null}
        end
    end

    Seller->>Controller: GET /accounts/shop (Cookie fes_session)
    Controller->>ShopService: readOwnShop(sessionId)
    ShopService->>SessionService: read(sessionId)
    alt sin sesión
        ShopService-->>Controller: NotAuthenticatedException
        Controller-->>Seller: 401 AUTH_REQUIRED
    else sin tienda
        ShopService-->>Controller: ShopNotFoundException (propia)
        Controller-->>Seller: 404 SHOP_NOT_FOUND "Todavía no configuraste tu tienda."
    else con tienda
        ShopService-->>Controller: ShopResponse(name, description)
        Controller-->>Seller: 200 {name, description|null}
    end

    Visitor->>Controller: GET /accounts/shops/{accountId} (público, sin cookie)
    Controller->>ShopService: readPublicShop(accountId)
    ShopService->>AccountRepo: existsById(accountId)
    alt cuenta inexistente
        ShopService-->>Controller: ShopNotFoundException (cuenta)
        Controller-->>Visitor: 404 ACCOUNT_NOT_FOUND "La cuenta indicada no existe."
    else cuenta existe, sin tienda
        ShopService->>ShopRepo: findByAccountId(accountId)
        ShopService-->>Controller: ShopNotFoundException (sin tienda)
        Controller-->>Visitor: 404 SHOP_NOT_FOUND "Ese vendedor todavía no tiene tienda."
    else cuenta con tienda
        ShopService->>ShopRepo: findByAccountId(accountId)
        ShopService-->>Controller: ShopResponse(name, description)
        Controller-->>Visitor: 200 {name, description|null}
    end
```

## Estructura de clases

Clases previstas del paquete `com.friendlyeshop.account` para este corte. Se muestran las relaciones arquitectónicas relevantes (colaboradores, repositorios y contratos públicos) y se omiten dependencias transitivas ya explicadas por un colaborador principal.

```mermaid
classDiagram
    direction TB

    class ShopController {
        -ShopService shopService
        +getOwn(sessionId) ShopResponse
        +saveOwn(sessionId, request) ShopResponse
        +getPublic(accountId) ShopResponse
    }

    class ShopService {
        -ShopRepository shopRepository
        -AccountRepository accountRepository
        -SessionService sessionService
        +readOwnShop(sessionId) ShopResponse
        +saveOwnShop(sessionId, name, description) ShopResponse
        +readPublicShop(accountId) ShopResponse
        -resolveAccountId(sessionId) UUID
    }

    class SessionService {
        +read(sessionId) SessionResponse
    }

    class Shop {
        <<entity>>
        +NAME_MAX_LENGTH : int = 80
        +DESCRIPTION_MAX_LENGTH : int = 1000
        -UUID id
        -UUID accountId
        -String name
        -String description
        -Instant createdAt
        -Instant updatedAt
        +open(accountId, name, description, now)$ Shop
        +updateDetails(name, description, now) void
    }

    class ShopRequest {
        <<record>>
        +String name
        +String description
    }

    class ShopResponse {
        <<record>>
        +String name
        +String description
    }

    class ErrorResponse {
        <<record>>
        +String error
        +String message
    }

    class ShopNotFoundException {
        <<exception>>
        +String error
        +String message
    }

    class NotAuthenticatedException {
        <<exception>>
    }

    class GlobalExceptionHandler {
        <<RestControllerAdvice>>
        +handleNotFound(ex) ResponseEntity~ErrorResponse~
        +handleNotAuthenticated(ex) ResponseEntity~ErrorResponse~
        +handleValidation(ex) ResponseEntity~ErrorResponse~
    }

    class ShopRepository {
        <<interface>>
        +findByAccountId(accountId) Optional~Shop~
    }

    class AccountRepository {
        <<interface>>
        +existsById(accountId) boolean
    }

    class SessionCookieWriter {
        <<component>>
        +COOKIE_NAME : String = "fes_session"
    }

    ShopController --> ShopService
    ShopController ..> ShopRequest
    ShopController ..> ShopResponse
    ShopController ..> SessionCookieWriter : COOKIE_NAME
    ShopService --> ShopRepository
    ShopService --> AccountRepository
    ShopService --> SessionService
    ShopService ..> Shop
    ShopService ..> ShopNotFoundException
    ShopService ..> ShopResponse
    ShopRepository ..> Shop
    GlobalExceptionHandler ..> ShopNotFoundException
    GlobalExceptionHandler ..> NotAuthenticatedException
    GlobalExceptionHandler ..> ErrorResponse
    JpaRepository <|-- ShopRepository
    JpaRepository <|-- AccountRepository
```

## Notas de contrato

- **Prefijo**: `ApiPathConfig` publica todo `controller/` bajo `/accounts`; el controlador solo declara `/shop` y `/shops/{accountId}`.
- **Sesión**: cookie `fes_session` (HttpOnly, SameSite=Lax, Path=/) resuelta con `SessionService.read`; no se inventa autenticación y no se modifica CORS ni `ApiPathConfig`.
- **Guardado (upsert)**: `PUT /accounts/shop` → `200 {name, description|null}`; crea si no existe y reemplaza si existe; una sola fila por cuenta (`UNIQUE (account_id)`).
- **Consulta propia**: `GET /accounts/shop` → `200 {name, description|null}` o `404` si aún no hay tienda; `401` sin sesión.
- **Consulta pública**: `GET /accounts/shops/{accountId}` sin sesión → `200 {name, description|null}` o `404` (cuenta inexistente vs vendedor sin tienda, con mensajes distintos).
- **Privacidad**: `ShopResponse` expone solo `name` y `description`; nunca `email` ni credenciales. `description` se serializa como `null` cuando está ausente.
- **Validación**: `name` obligatorio `1..80` tras recorte; `description` opcional `<=1000` tras recorte y vacía/solo espacios → `null`; `400 VALIDATION_ERROR` con mensaje en español.
- **Persistencia**: migración `V3__create_shops.sql` (`id`, `account_id`, `name`, `description`, `created_at`, `updated_at`) compatible con `ddl-auto: validate`.
