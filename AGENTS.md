# AGENTS.md - account-api

## Proyecto
API de cuentas de Friendly E-Shop. Es un servicio independiente en Java 25 y Spring Boot que posee la identidad y las credenciales usadas para acceder a la tienda y al panel; expone `/accounts`.
Persiste únicamente en la base `accounts`, usa Flyway, prepara RabbitMQ para eventos y emite telemetría mediante Actuator/OpenTelemetry.

## Comandos
- Ejecutar: `./mvnw spring-boot:run`
- Tests: `./mvnw test`
- Compilar y verificar: `./mvnw verify`
- Lint: `./mvnw checkstyle:check`; también se ejecuta automáticamente en la fase `validate`.

## Estilo y convenciones
- Usa Java 25, Spring Boot 4.1 y el paquete `com.friendlyeshop.account`.
- Nombres, código y documentación técnica en inglés; mensajes visibles al usuario en español.
- Respeta `checkstyle.xml`: 4 espacios, sin tabs, líneas de hasta 120 caracteres e imports explícitos.
- Mantén controladores delgados y separa la autenticación de la gestión de cuentas.
- Crea nuevas migraciones Flyway; no edites migraciones ya aplicadas. Hibernate solo valida el esquema.

## Reglas
- Lee la skill `/java-springboot` y la spec activa, si existe, antes de tocar código.
- Este servicio es la fuente de verdad de identidades, credenciales y roles de acceso.
- Nunca almacenes ni expongas contraseñas en claro; persiste únicamente hashes seguros.
- Nunca escribas tablas de otros dominios; comunica cambios mediante contratos o eventos definidos.
- Conserva `/accounts`, variables de entorno, health checks, métricas y RabbitMQ; no añadas Kafka.
- No omitas ni desactives reglas de Checkstyle para evitar corregir una violación.
- Los manifiestos y secretos pertenecen a `infra`; coordina allí cambios de puerto, ruta o configuración.

## Al terminar cualquier tarea
- Ejecuta `./mvnw verify`; incluye Checkstyle y los tests.
- Prueba autenticación, autorización y validaciones afectadas; añade migraciones para cambios de esquema.
- Comprueba que no se hayan roto `/accounts` ni los endpoints de Actuator.
