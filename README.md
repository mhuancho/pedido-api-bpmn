# pedido-api-bpmn
API de dominio para gestión de pedidos con estados `PENDIENTE`, `APROBADO`, `PAGADO`, `CANCELADO`.

Este servicio:
- expone endpoints REST de negocio,
- persiste pedidos/eventos/idempotencia en PostgreSQL,
- inicia proceso BPMN vía `camunda-gateway-api` (síncrono en creación),
- publica transiciones de estado (`aprobar/pagar/cancelar`) usando Outbox + publicación asíncrona.

## Arquitectura
- `pedido-api-bpmn`: dominio de negocio + persistencia + outbox publisher.
- `camunda-gateway-api`: integración con Camunda 8 (start process + correlate message + workers).

Flujo general:
1. `POST /api/pedidos` crea pedido y arranca instancia BPMN (retorna `processInstanceKey` y `processDefinitionKey`).
2. `POST /api/pedidos/{id}/aprobar|pagar|cancelar` cambia estado y encola evento outbox.
3. Worker de outbox publica al gateway y marca evento `PROCESSED`/`FAILED`.

## Requisitos
- Java 21
- PostgreSQL
- Camunda 8 (Zeebe + Operate)
- `camunda-gateway-api` en ejecución

## Variables de entorno
### Base de datos
- `DB_URL` (default: `jdbc:postgresql://localhost:5534/ventas_db`)
- `DB_USERNAME` (default: `postgres17`)
- `DB_PASSWORD` (default: `123`)

### Seguridad JWT (resource server)
- `JWT_ISSUER_URI`
- `JWT_RESOURCE_CLIENT_ID`

### Comunicación interna con gateway
- `ORCHESTRATION_BASE_URL` (default: `http://localhost:4333`)
- `INTERNAL_API_TOKEN` (debe coincidir con el gateway)
- `INTERNAL_TOKEN_HEADER` (default: `X-Internal-Token`)

### Resiliencia Feign
- `ORCHESTRATION_CONNECT_TIMEOUT_MS` (default: `3000`)
- `ORCHESTRATION_READ_TIMEOUT_MS` (default: `5000`)
- `ORCHESTRATION_RETRY_MAX_ATTEMPTS` (default: `2`)
- `ORCHESTRATION_RETRY_PERIOD_MS` (default: `200`)
- `ORCHESTRATION_RETRY_MAX_PERIOD_MS` (default: `1000`)

## Ejecutar
```bash
./gradlew bootRun
```

## Endpoints principales
Base URL: `http://localhost:4222`

### Crear pedido
```bash
curl --location 'http://localhost:4222/api/pedidos' \
--header 'Content-Type: application/json' \
--data '{
  "cliente": "Mateo",
  "monto": 150
}'
```

### Aprobar pedido
```bash
curl --location --request POST 'http://localhost:4222/api/pedidos/1/aprobar' \
--header 'Idempotency-Key: key-aprobar-1'
```

### Pagar pedido
```bash
curl --location --request POST 'http://localhost:4222/api/pedidos/1/pagar' \
--header 'Idempotency-Key: key-pagar-1'
```

### Cancelar pedido
```bash
curl --location --request POST 'http://localhost:4222/api/pedidos/1/cancelar' \
--header 'Idempotency-Key: key-cancelar-1'
```

### Consultar pedido
```bash
curl --location 'http://localhost:4222/api/pedidos/1'
```

## Observabilidad
- Actuator: `/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus`
- Métricas de outbox/transiciones con Micrometer.
- Logs estructurados en formato ECS.

## BPMN (resumen)
- Inicio -> espera `pedido-aprobado` o `pedido-cancelado`.
- Si aprueba -> espera `pedido-pagado` o `pedido-cancelado`.
- Finaliza en tarea `notificar-pedido-finalizado` y luego `Fin pagado` o `Fin cancelado`.

## Imágenes del flujo
![proceso-pedido.png](proceso-pedido.png)
![proceso-completado.png](proceso-completado.png)
![pedido-cancelado.png](pedido-cancelado.png)
