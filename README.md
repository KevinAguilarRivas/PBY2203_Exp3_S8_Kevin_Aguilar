# Banco XYZ — Microservicios en la Nube con Spring Cloud

**Curso:** Desarrollo Backend III (PBY2203) — Exp3, Semana 8  
**Actividad:** Desarrollando microservicios y resiliencia en la nube con Spring Cloud  
**Alumno:** Kevin Aguilar  
**Repositorio:** https://github.com/KevinAguilarRivas/PBY2203_Exp3_S8_Kevin_Aguilar  

---

## Descripción general

Este proyecto es la continuación del BFF Banco XYZ (Semana 5). Sobre la base de los mismos microservicios (`core-service`, `bff-web`, `bff-mobile`, `bff-cajero`), se agrega la infraestructura completa de Spring Cloud más los requisitos de la Semana 8:

| Requisito | Implementación |
|-----------|---------------|
| **OAuth2.0** | Keycloak como Authorization Server; los 3 BFF actúan como Resource Servers validando JWT |
| **Dockerfiles** | Cada microservicio y servidor tiene su propio `Dockerfile` (imagen `eclipse-temurin:17-jre-alpine`) |
| **docker-compose.yaml** | Orquesta postgres, zookeeper, kafka, keycloak, config-server, eureka-server y los 4 microservicios de negocio |
| **Resilience4j** | Circuit Breaker en todos los BFF; Retry adicional en `bff-cajero` por ser canal crítico |
| **Kafka** | `core-service` publica un evento `RetiroEvent` en el topic `bancoxyz.retiros` cada vez que se aprueba y persiste un retiro |

---

## Arquitectura

```
                        ┌─────────────────────────────────────────────────┐
                        │              Keycloak (puerto 8180)              │
                        │         Authorization Server — realm bancoxyz    │
                        └────────────────────┬────────────────────────────┘
                                             │ emite JWT
                                             ▼
              ┌────────────┐     ┌─────────────────────────────┐
              │Config Server│     │        Eureka Server        │
              │  (8888)    │     │         (8761)              │
              └─────┬──────┘     └────────────┬────────────────┘
                    │ provee configs           │ registro/descubrimiento
                    ▼                          ▼
          ┌──────────────────────────────────────────────┐
          │                core-service (8080)            │
          │  JPA → PostgreSQL  │  Kafka Producer/Consumer │
          └──────────────────────────────────────────────┘
                    ▲                  │
         Circuit    │                  │ topic: bancoxyz.retiros
         Breaker    │                  ▼
    ┌───────────────┼─────────────────────────────────┐
    │               │                                 │
┌───┴────┐    ┌─────┴──────┐    ┌────────────────────┴┐
│bff-web │    │ bff-mobile │    │     bff-cajero       │
│ (8081) │    │  (8082)    │    │      (8083)          │
│OAuth2  │    │  OAuth2    │    │  OAuth2 + Retry CB   │
└────────┘    └────────────┘    └─────────────────────┘
    ▲               ▲                      ▲
    │               │                      │
 JWT token      JWT token              JWT token
 (canal-web)  (canal-mobile)         (canal-cajero)
```

---

## Componentes

### Infraestructura Spring Cloud

| Servicio | Puerto | Rol |
|----------|--------|-----|
| `config-server` | 8888 | Servidor de configuración centralizado (Spring Cloud Config). Sirve configuraciones desde `classpath:/config-repo` |
| `eureka-server` | 8761 | Service Discovery (Netflix Eureka). Todos los microservicios se registran aquí |
| `keycloak` | 8180 | Authorization Server OAuth2/OIDC. Realm `bancoxyz` con 3 usuarios y 3 roles |

### Microservicios de negocio

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| `core-service` | 8080 | Expone datos de cuentas migrados por el batch (Semana 5). Publica `RetiroEvent` en Kafka al actualizar saldo |
| `bff-web` | 8081 | Canal Web: respuesta completa (intereses + historial) para pantallas de detalle/auditoría |
| `bff-mobile` | 8082 | Canal Móvil: respuesta mínima (resumen + últimos N movimientos) para ahorro de ancho de banda |
| `bff-cajero` | 8083 | Canal Cajero: saldo y retiro. Circuit Breaker + Retry por criticidad del canal |

---

## OAuth2 — Flujo de autenticación

Este proyecto implementa el flujo **Resource Owner Password Credentials** de OAuth2 (apropiado para servicios internos y entornos académicos) y el flujo **Client Credentials** para integraciones M2M.

### 1. Obtener token JWT desde Keycloak

```bash
# Token para canal Web (usuario.web / web123)
curl -s -X POST http://localhost:8180/realms/bancoxyz/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=bancoxyz-app" \
  -d "client_secret=bancoxyz-secret-2026" \
  -d "username=usuario.web" \
  -d "password=web123" | jq -r .access_token
```

```bash
# Token para canal Móvil
curl -s -X POST http://localhost:8180/realms/bancoxyz/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=bancoxyz-app" \
  -d "client_secret=bancoxyz-secret-2026" \
  -d "username=usuario.mobile" \
  -d "password=mobile123" | jq -r .access_token
```

```bash
# Token para canal Cajero
curl -s -X POST http://localhost:8180/realms/bancoxyz/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=bancoxyz-app" \
  -d "client_secret=bancoxyz-secret-2026" \
  -d "username=cajero.atm001" \
  -d "password=cajero123" | jq -r .access_token
```

### 2. Llamar al BFF con el token

```bash
# Guardar el token en variable
TOKEN=$(curl -s -X POST http://localhost:8180/realms/bancoxyz/protocol/openid-connect/token \
  -d "grant_type=password&client_id=bancoxyz-app&client_secret=bancoxyz-secret-2026&username=usuario.web&password=web123" \
  | jq -r .access_token)

# Consultar cuenta en BFF Web
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/web/cuentas/107

# Consultar transacciones en BFF Web
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/web/transacciones
```

---

## Tolerancia a fallos — Resilience4j

Cada BFF tiene configurado un **Circuit Breaker** sobre las llamadas a `core-service`. El `bff-cajero` añade además un **Retry** (3 intentos, 500ms entre ellos) por ser el canal más crítico.

### Comportamiento del Circuit Breaker

| Estado | Condición | Comportamiento |
|--------|-----------|---------------|
| CLOSED | Normal | Las llamadas pasan a core-service |
| OPEN | ≥50% de fallos en ventana de 5 llamadas | Retorna 503 inmediatamente sin llamar al servicio. Espera 10s |
| HALF_OPEN | Tras 10s en OPEN | Permite 2 llamadas de prueba para verificar si el servicio se recuperó |

### Verificar estado del Circuit Breaker

```bash
# Estado de los circuit breakers en bff-cajero
curl http://localhost:8083/actuator/health | jq .
```

---

## Mensajería asíncrona — Kafka

Cuando el BFF Cajero aprueba un retiro, la secuencia es:

```
bff-cajero → PATCH /internal/cuentas/{id}/saldo → core-service
                                                        │
                                                        ├─ persiste nuevo saldo en PostgreSQL
                                                        │
                                                        └─ publica RetiroEvent en topic bancoxyz.retiros
                                                                        │
                                                                        └─ RetiroEventConsumer (core-service) registra en log
```

### Ver eventos en Kafka (desde contenedor)

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic bancoxyz.retiros \
  --from-beginning
```

---

## Cómo ejecutar

### Prerrequisitos
- Docker Desktop 4.x con Docker Compose V2
- Maven 3.9+ y Java 17 (solo para compilar; el runtime es Docker)

### Paso 1: Compilar todos los módulos

```bash
# Desde la raíz del proyecto
mvn clean package -DskipTests
```

### Paso 2: Levantar la base de datos y poblarla (batch Semana 5)

> El batch de migración (Semana 5) debe ejecutarse primero para que PostgreSQL tenga datos.
> Si ya tienes la base poblada, salta al Paso 3.

```bash
# Levantar solo el postgres de ESTE proyecto (queda expuesto en localhost:5432)
docker compose up -d postgres

# Ejecutar el batch desde el directorio batch-migracion de la Semana 5.
# NO levantar el docker-compose del batch: usaria el mismo puerto 5432.
cd <ruta>/Exp2_S5_Kevin_Aguilar/batch-migracion
mvn clean package -DskipTests
java -jar target/batch-migracion-0.0.1-SNAPSHOT.jar --job=all
# Cuando el log muestre los 3 jobs "completed", detener con Ctrl+C
```

Resultado esperado en la base `bancoxyz`: 1000 filas en `cuentas_interes`, 1000 en `estado_cuenta_anual` y 945 en `resumen_transacciones_diarias`.

### Paso 3: Levantar el stack completo

```bash
# Desde la raíz de este proyecto
docker compose up --build
```

El orden de arranque está gestionado por `healthcheck` y `depends_on`:
1. `postgres` y `kafka` (infraestructura base)
2. `keycloak` (OAuth2)
3. `config-server` → `eureka-server`
4. `core-service` → `bff-web`, `bff-mobile`, `bff-cajero`

### Paso 4: Probar los endpoints

```bash
# 1. Obtener token Web
TOKEN=$(curl -s -X POST http://localhost:8180/realms/bancoxyz/protocol/openid-connect/token \
  -d "grant_type=password&client_id=bancoxyz-app&client_secret=bancoxyz-secret-2026&username=usuario.web&password=web123" \
  | jq -r .access_token)

# 2. BFF Web — cuenta completa
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/web/cuentas/107

# 3. BFF Web — transacciones
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/web/transacciones

# 4. Token Móvil
TOKEN_M=$(curl -s -X POST http://localhost:8180/realms/bancoxyz/protocol/openid-connect/token \
  -d "grant_type=password&client_id=bancoxyz-app&client_secret=bancoxyz-secret-2026&username=usuario.mobile&password=mobile123" \
  | jq -r .access_token)

# 5. BFF Móvil — resumen
curl -H "Authorization: Bearer $TOKEN_M" http://localhost:8082/mobile/cuentas/107/resumen

# 6. BFF Móvil — últimos 3 movimientos
curl -H "Authorization: Bearer $TOKEN_M" "http://localhost:8082/mobile/cuentas/107/movimientos?limite=3"

# 7. Token Cajero
TOKEN_C=$(curl -s -X POST http://localhost:8180/realms/bancoxyz/protocol/openid-connect/token \
  -d "grant_type=password&client_id=bancoxyz-app&client_secret=bancoxyz-secret-2026&username=cajero.atm001&password=cajero123" \
  | jq -r .access_token)

# 8. BFF Cajero — saldo
curl -H "Authorization: Bearer $TOKEN_C" http://localhost:8083/cajero/cuentas/107/saldo

# 9. BFF Cajero — retiro (activa el evento Kafka)
curl -X POST -H "Authorization: Bearer $TOKEN_C" \
  -H "Content-Type: application/json" \
  -d '{"monto": 50.00}' \
  http://localhost:8083/cajero/cuentas/107/retiro

# 10. Ver el evento en Kafka
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic bancoxyz.retiros --from-beginning
```

### Paso 5: Script de pruebas automatizado (evidencia)

El script [`evidencia/pruebas.sh`](evidencia/pruebas.sh) ejecuta todas las pruebas end-to-end y su salida queda en [`evidencia/salida_pruebas.txt`](evidencia/salida_pruebas.txt):

```bash
bash evidencia/pruebas.sh > evidencia/salida_pruebas.txt 2>&1
```

| # | Prueba | Resultado esperado |
|---|--------|--------------------|
| 1-3 | Contenedores, registro en Eureka, Config Server | 10 contenedores arriba, 4 servicios `UP` en Eureka |
| 4 | OAuth2 | Sin token / token inválido → `401`; JWT con `iss=http://keycloak:8180/realms/bancoxyz` y rol por canal; Client Credentials OK |
| 5-6 | BFF Web y Mobile | `200` con datos; cuenta inexistente → `404` (no abre el circuito) |
| 7 | BFF Cajero + Kafka | Retiro `APROBADO`, saldo insuficiente `RECHAZADO`; `RetiroEvent` publicado en `bancoxyz.retiros` y consumido por `RetiroEventConsumer` |
| 8 | Resilience4j | Se detiene `core-service` → circuito `CLOSED → OPEN` (respuestas en ~10 ms sin llamar al servicio); Retry del cajero con 3 intentos; al levantarlo `HALF_OPEN → CLOSED` |

Estado del Circuit Breaker en cualquier BFF: `curl http://localhost:8082/actuator/circuitbreakers`

> **Nota Keycloak:** el realm `bancoxyz` define `frontendUrl=http://keycloak:8180` (en `keycloak/bancoxyz-realm.json`). Así el `iss` del token es siempre `http://keycloak:8180/realms/bancoxyz`, aunque se pida desde el host (`localhost:8180`), y coincide con el `issuer-uri` que validan los BFF. La consola de administración (realm master) sigue funcionando en `http://localhost:8180`. Keycloak usa `KC_DB=dev-file` (H2 en archivo); con `dev-mem` la base se vaciaba en ejecución.

### Evidencia de ejecución (capturas)

Las capturas están en [`evidencia/capturas/`](evidencia/capturas/):

| Criterio | Capturas |
|----------|----------|
| Dockerización | `01_build_maven`, `02_imagenes_docker` |
| docker-compose | `03_docker_compose_ps`, `04_arranque_microservicios`, `05_health_actuator`, `06_eureka_dashboard`, `07_config_server` |
| OAuth2.0 | `08_oauth2_token`, `09_oauth2_proteccion`, `10_keycloak_realm`, `10b`–`10f` (consola de Keycloak: login, realm, clientes, usuarios y roles) |
| Microservicios (resultados) | `11_bff_web`, `12_bff_mobile`, `13_bff_cajero`, `17_base_datos` |
| Kafka | `14_kafka` |
| Resilience4j | `15_circuit_breaker`, `16_retry_cajero` |

### Consola Eureka (Service Discovery)

Abre en el navegador: [http://localhost:8761](http://localhost:8761)

Verás registrados: `CORE-SERVICE`, `BFF-WEB`, `BFF-MOBILE`, `BFF-CAJERO`.

### Consola Keycloak (OAuth2)

Abre en el navegador: [http://localhost:8180](http://localhost:8180)  
Usuario admin: `admin` / Contraseña: `admin`  
Realm: `bancoxyz`

---

## Estructura del repositorio

```
Exp3_S8_Kevin_Aguilar/
├── docker-compose.yml              # Orquestación completa del stack
├── pom.xml                         # POM padre multi-módulo
│
├── config-server/                  # Servidor de configuración (puerto 8888)
│   ├── Dockerfile
│   └── src/main/resources/
│       ├── application.yml
│       └── config-repo/            # Configuraciones por servicio
│           ├── core-service.yml
│           ├── bff-web.yml
│           ├── bff-mobile.yml
│           └── bff-cajero.yml
│
├── eureka-server/                  # Service Discovery (puerto 8761)
│   ├── Dockerfile
│   └── src/...
│
├── keycloak/
│   └── bancoxyz-realm.json         # Realm pre-configurado (import automático)
│
├── core-service/                   # Backend interno (puerto 8080)
│   ├── Dockerfile
│   └── src/main/java/com/bancoxyz/core/
│       ├── controller/             # CuentasController, TransaccionesController
│       ├── event/                  # RetiroEvent (Kafka)
│       ├── kafka/                  # KafkaConfig, RetiroEventProducer, RetiroEventConsumer
│       ├── model/                  # Entidades JPA
│       ├── repository/             # Spring Data JPA
│       └── dto/
│
├── bff-web/                        # Canal Web — OAuth2 + Circuit Breaker (puerto 8081)
│   ├── Dockerfile
│   └── src/main/java/com/bancoxyz/bff/web/
│       ├── config/                 # SecurityConfig (OAuth2), RestClientConfig
│       ├── client/                 # CoreServiceClient (con @CircuitBreaker)
│       ├── controller/             # CuentaWebController, TransaccionesWebController
│       └── dto/
│
├── bff-mobile/                     # Canal Móvil — OAuth2 + Circuit Breaker (puerto 8082)
│   ├── Dockerfile
│   └── src/...
│
└── bff-cajero/                     # Canal Cajero — OAuth2 + Circuit Breaker + Retry (puerto 8083)
    ├── Dockerfile
    └── src/...
```

---

## Decisiones de diseño

### ¿Por qué Keycloak?
Es el Authorization Server más usado en ecosistemas Spring Boot en producción. Se integra nativamente con `spring-boot-starter-oauth2-resource-server` — los BFF solo necesitan conocer el `issuer-uri` y Spring valida automáticamente la firma del JWT.

### ¿Por qué Kafka y no JMS/ActiveMQ?
Kafka es más adecuado para eventos de dominio financiero: garantía de entrega, replay de mensajes y escalabilidad horizontal. En este proyecto, el topic `bancoxyz.retiros` recibe eventos de retiro que podrían ser consumidos por sistemas de auditoría, notificaciones o conciliación.

### ¿Por qué Circuit Breaker + Retry en bff-cajero?
El cajero ATM es el canal más crítico en cuanto a disponibilidad: un fallo intermitente de red no debe denegar un retiro. Con **Retry** se reintenta 3 veces antes de abrir el circuito, tolerando microfallos transitorios. El **Circuit Breaker** protege de fallos sostenidos, devolviendo 503 inmediatamente en lugar de bloquear el hilo.

### Config Server con perfil `native`
Las configuraciones se sirven desde `classpath:/config-repo` (archivos dentro del JAR). En producción se apuntaría a un repositorio Git privado para que los cambios de configuración no requieran recompilar.
