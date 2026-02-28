# univerliga-crm-service

Production-style CRM монолит (People + Tasks/Episodes) с JWT security, Flyway, RabbitMQ и Outbox pattern.

## Что делает сервис

- CRM является source of truth для `Person` и `Task`.
- CRM не создает пользователей Keycloak синхронно.
- Вместо этого CRM пишет доменные события в `outbox_events`, а scheduler публикует их в RabbitMQ (`univerliga.crm.events`).
- Это уменьшает связность и защищает от потери событий при падении между DB commit и publish.

## Стек

- Java 21
- Spring Boot 3.3
- Spring Web MVC, Validation, Data JPA
- Spring Security OAuth2 Resource Server (JWT)
- PostgreSQL + Flyway
- RabbitMQ (AMQP)
- springdoc OpenAPI + Swagger UI
- Actuator

## Быстрый старт

```bash
docker compose up --build
```

Локальный запуск без Docker:

```bash
./gradlew bootRun
```

Сервисы:

- CRM: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Actuator health: `http://localhost:8080/actuator/health`
- RabbitMQ UI: `http://localhost:15672` (`guest/guest`)
- Keycloak: `http://localhost:8081` (admin/admin)

## Keycloak Realm

- Realm: `univerliga`
- Client: `univerliga-crm` (confidential)
- Secret: `univerliga-crm-secret`
- Roles: `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_HR`, `ROLE_EMPLOYEE`
- Users:
  - `admin/admin`
  - `manager/manager`
  - `hr/hr`
  - `employee/employee`

## Получение токенов (password grant)

```bash
KC_URL="http://localhost:8081/realms/univerliga/protocol/openid-connect/token"
CLIENT_ID="univerliga-crm"
CLIENT_SECRET="univerliga-crm-secret"

# admin
ADMIN_TOKEN=$(curl -s -X POST "$KC_URL" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=admin" \
  -d "password=admin" | jq -r .access_token)

# manager
MANAGER_TOKEN=$(curl -s -X POST "$KC_URL" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=manager" \
  -d "password=manager" | jq -r .access_token)

# hr
HR_TOKEN=$(curl -s -X POST "$KC_URL" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=hr" \
  -d "password=hr" | jq -r .access_token)

# employee
EMPLOYEE_TOKEN=$(curl -s -X POST "$KC_URL" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=employee" \
  -d "password=employee" | jq -r .access_token)
```

## Примеры curl

### SYSTEM

```bash
curl -s http://localhost:8080/api/v1/system/version
```

### ME

```bash
curl -s http://localhost:8080/api/v1/me \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

### PEOPLE

```bash
# list (admin/manager/hr)
curl -s "http://localhost:8080/api/v1/crm/people?page=1&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# create (admin)
curl -s -X POST http://localhost:8080/api/v1/crm/people \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "displayName":"Alice Doe",
    "email":"alice@univerliga.com",
    "departmentId":"d_2",
    "teamId":"t_2",
    "role":"EMPLOYEE"
  }'

# get by id
curl -s http://localhost:8080/api/v1/crm/people/p_employee \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"

# patch (admin)
curl -s -X PATCH http://localhost:8080/api/v1/crm/people/p_employee \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"teamId":"t_3","active":true}'

# soft delete (admin)
curl -s -X DELETE http://localhost:8080/api/v1/crm/people/p_101 \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# manual identity bridge (admin)
curl -s -X POST http://localhost:8080/api/v1/crm/people/p_101/identity \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "identityStatus":"PROVISIONED",
    "keycloakUserId":"kc_123",
    "lastIdentityError":null
  }'
```

### TASKS

```bash
# list
curl -s "http://localhost:8080/api/v1/crm/tasks?page=1&size=20" \
  -H "Authorization: Bearer $MANAGER_TOKEN"

# create (manager/admin)
curl -s -X POST http://localhost:8080/api/v1/crm/tasks \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Quarter Review",
    "description":"Prepare quarterly review",
    "period":{"from":"2026-03-01","to":"2026-03-31"},
    "ownerId":"p_manager",
    "assigneeId":"p_employee",
    "participantIds":["p_employee","p_101"]
  }'

# patch
curl -s -X PATCH http://localhost:8080/api/v1/crm/tasks/<task_id> \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"ACTIVE","participantIds":["p_employee","p_101","p_102"]}'

# close
curl -s -X POST http://localhost:8080/api/v1/crm/tasks/<task_id>/close \
  -H "Authorization: Bearer $MANAGER_TOKEN"
```

### OUTBOX

```bash
# list outbox
curl -s "http://localhost:8080/api/v1/crm/outbox?status=FAILED&page=1&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# replay
curl -s -X POST http://localhost:8080/api/v1/crm/outbox/<event_id>/replay \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## RabbitMQ события

Exchange: `univerliga.crm.events` (topic)

Routing keys:

- `crm.person.created`
- `crm.person.updated`
- `crm.person.deactivated`
- `crm.task.created`
- `crm.task.updated`
- `crm.task.closed`

Формат сообщения:

```json
{
  "eventId": "uuid",
  "type": "PersonCreated",
  "occurredAt": "ISO-8601",
  "source": "crm-service",
  "payload": {}
}
```

Проверка публикации:

1. Создать/изменить человека или задачу.
2. Открыть `GET /api/v1/crm/outbox` и убедиться, что события переходят в `SENT`.
3. Смотреть логи CRM: есть строки `Outbox event sent`.

## Почему не синхронный вызов Keycloak из CRM

Синхронная интеграция CRM -> Keycloak создает жесткую зависимость и увеличивает вероятность ошибок при временной недоступности identity-системы.
Outbox + asynchronous provisioning позволяет:

- не терять событие при падении процесса;
- изолировать CRM от latency/failure Keycloak;
- повторно отправлять события (`FAILED` -> replay/retry);
- масштабировать provisioning независимо от CRM.
