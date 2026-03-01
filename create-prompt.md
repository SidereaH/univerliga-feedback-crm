Ты — senior Java/Spring архитектор. Сгенерируй полностью рабочий production-grade проект CRM (упрощённый “монолит как 1С-имитация”, но очень простой),
который является источником истины по People и Tasks/Episodes и публикует доменные события в RabbitMQ для Identity Provisioning и других сервисов.
Название репозитория: "univerliga-crm-service"

КОНТЕКСТ (ВАЖНО сохранить):
- Архитектура проекта: API Gateway + Keycloak (auth), микросервис Feedback, микросервис Analytics, и CRM-монолит.
- CRM хранит людей и задачи. При создании человека НЕ создавать синхронно пользователя в Keycloak (избегаем жёсткой связности).
- Вместо этого: CRM публикует событие PersonCreated/Updated/Deactivated в RabbitMQ, а Identity Provisioning service слушает и создаёт/обновляет пользователя в Keycloak асинхронно.
- CRM хранит статус identity provisioning и связь personId <-> keycloakUserId (через события/вебхук от provisioning или ручное обновление).
- Front ходит через gateway, но CRM API должен быть полноценным и prod-style.

СТЕК:
- Java 21
- Spring Boot 3.3+
- Spring Web (MVC)
- Spring Validation (jakarta)
- Spring Security OAuth2 Resource Server (JWT) — проверка токена от Keycloak
- Spring Data JPA + PostgreSQL
- Flyway migrations
- Spring AMQP (RabbitMQ)
- springdoc-openapi-starter-webmvc-ui (Swagger UI)
- Actuator
- Lombok
- Логи: requestId в MDC
- Сборка: Maven

ДОКЕР:
- docker-compose.yml поднимает:
    1) postgres (для CRM)
    2) rabbitmq (management enabled)
    3) keycloak + postgres (можно отдельный) + импорт realm-export.json
    4) crm-service
- Realm: univerliga
- Клиент: univerliga-crm (confidential) или univerliga-gateway (можно reuse) — выбери univerliga-crm.
- Роли realm:
    - ROLE_ADMIN
    - ROLE_MANAGER
    - ROLE_HR
    - ROLE_EMPLOYEE
- Пользователи:
    - admin/admin -> ROLE_ADMIN
    - manager/manager -> ROLE_MANAGER
    - hr/hr -> ROLE_HR
    - employee/employee -> ROLE_EMPLOYEE

SECURITY:
- /actuator/** и /swagger-ui/** и /v3/api-docs/** без токена
- Все /api/v1/** требуют JWT
- Политики доступа:
    - ADMIN: полный доступ ко всем CRM endpoints
    - MANAGER и HR: чтение людей/задач; создание/редактирование задач (MANAGER), HR может только read + deactivate person (опционально)
    - EMPLOYEE: только чтение самого себя (people/{self}) и задач где он participant/assignee/owner; не может CRUD людей; не может создавать задачи (можно запретить)
- Определи current user из JWT:
    - username (preferred_username)
    - roles (realm_access.roles)
    - personId: брать из user attribute "personId" если он есть в токене (в MVP можно:
        - маппить employee->p_employee, manager->p_manager, admin->p_admin, hr->p_hr если claim отсутствует)
    - teamId/departmentId: если есть в claims, иначе мок.

ОБЩИЕ ТРЕБОВАНИЯ К API:
- Base path: /api/v1
- Все ответы JSON: application/json; charset=utf-8
- Успех envelope:
  {
  "data": <payload>,
  "meta": { "requestId": "...", "timestamp": "ISO-8601", "version": "v1" }
  }
- Ошибки:
  {
  "error": {
  "code": "STRING_CODE",
  "message": "Human readable",
  "details": [ { "field": "optional", "issue": "optional" } ],
  "requestId": "..."
  }
  }
- Correlation/request id:
    - принимать X-Request-Id, если нет — генерировать UUID
    - вернуть X-Request-Id + включить в meta/error
- Global exception handler (ControllerAdvice):
    - validation -> 400 VALIDATION_ERROR + details
    - 401 UNAUTHORIZED
    - 403 FORBIDDEN
    - 404 NOT_FOUND
    - 409 CONFLICT (например duplicate email)
    - 500 INTERNAL_ERROR
- Swagger/OpenAPI: документировать ВСЕ endpoints + схемы + примеры
- Валидация входных DTO.

СУЩНОСТИ И ДАННЫЕ:
A) Person (CRM источник истины)
- id: "p_<uuid>" (строка)
- displayName: string (1..200)
- email: string (email, unique, lowercased)
- departmentId: "d_<...>" (строка)
- teamId: "t_<...>" (строка)
- role: EMPLOYEE|MANAGER|HR|ADMIN (бизнес-роль, может не совпадать 1:1 с keycloak)
- active: boolean
- identity:
    - identityStatus: PENDING|PROVISIONED|FAILED|DEPROVISIONED
    - keycloakUserId: string nullable
    - lastIdentityError: string nullable
- createdAt, updatedAt (ISO)
- version (optimistic locking)

B) Task/Episode
- id: "task_<uuid>"
- title: 1..200
- description: 0..5000
- status: DRAFT|ACTIVE|CLOSED
- period: from/to (LocalDate, required, from<=to)
- ownerId (personId) required
- assigneeId (personId) optional
- participantIds: list[personId] (может включать assignee)
- createdAt, updatedAt, closedAt nullable
- version

ИНВАРИАНТЫ (MVP):
- Участники задачи должны существовать и быть active=true (иначе 400)
- employee может читать задачу только если он owner/assignee/participant
- manager/admin могут читать любую задачу в рамках scope (в MVP можно без scope-check или только по teamId)
- При close задачи статус=CLOSED, closedAt проставить

EVENT-DRIVEN (RabbitMQ):
- Exchange: univerliga.crm.events (topic)
- Routing keys:
    - crm.person.created
    - crm.person.updated
    - crm.person.deactivated
    - crm.task.created
    - crm.task.updated
    - crm.task.closed
- Сообщения: JSON CloudEvents-like (но простой):
  {
  "eventId":"uuid",
  "type":"PersonCreated|PersonUpdated|PersonDeactivated|TaskCreated|TaskUpdated|TaskClosed",
  "occurredAt":"ISO-8601",
  "source":"crm-service",
  "payload": { ... }
  }
- payload для Person событий:
  {
  "personId":"p_123",
  "username":"<можно равным email до @ или отдельно поле username>",
  "email":"u@example.com",
  "displayName":"User",
  "departmentId":"d_1",
  "teamId":"t_1",
  "roles":["ROLE_EMPLOYEE|ROLE_MANAGER|ROLE_HR|ROLE_ADMIN"],
  "enabled": true
  }
  Правило: roles маппить из Person.role:
  EMPLOYEE -> ROLE_EMPLOYEE, MANAGER->ROLE_MANAGER, HR->ROLE_HR, ADMIN->ROLE_ADMIN.
  enabled = active.
- payload для Task событий:
  {
  "taskId":"task_1",
  "title":"...",
  "status":"DRAFT|ACTIVE|CLOSED",
  "period": { "from":"YYYY-MM-DD","to":"YYYY-MM-DD" },
  "ownerId":"p_10",
  "assigneeId":"p_11",
  "participantIds":["p_11","p_12"],
  "updatedAt":"ISO"
  }
- Публикация событий должна быть надежной:
    - реализуй Outbox pattern:
        - таблица outbox_events(id, aggregate_type, aggregate_id, type, routing_key, payload_json, status NEW|SENT|FAILED, created_at, sent_at, last_error)
        - при изменениях Person/Task записывай событие в outbox в рамках той же транзакции
        - отдельный scheduler (каждые 2-5 секунд) читает NEW/FAILED и публикует в RabbitMQ, затем помечает SENT
        - retries + backoff (или счетчик attempts)
    - В README объясни, почему это нужно (чтобы не потерять событие при падении).
- (Опционально) Consumer входящих событий от Identity Provisioning:
    - Exchange: univerliga.identity.events
    - Routing keys:
        - identity.user.provisioned
        - identity.user.failed
        - identity.user.deprovisioned
    - CRM обновляет identityStatus/keycloakUserId/lastIdentityError по personId.
    - Для MVP можно также дать ручной endpoint для обновления identity (см ниже).

ENDPOINTS (реализовать все):

1) SYSTEM
   GET /api/v1/system/version
   -> data { "name":"univerliga-crm-service","version":"0.1.0" }

2) AUTH/ME (для удобства фронта)
   GET /api/v1/me
   -> data:
   {
   "personId":"p_employee",
   "username":"employee",
   "roles":["ROLE_EMPLOYEE"],
   "departmentId":"d_1",
   "teamId":"t_1",
   "displayName":"Employee User"
   }

3) PEOPLE
   GET /api/v1/crm/people?query=&departmentId=&teamId=&active=&page=1&size=20
   Access:
- ADMIN/MANAGER/HR: можно
- EMPLOYEE: запрещено (403)
  Ответ data:
  {
  "items":[
  {
  "id":"p_1",
  "displayName":"...",
  "email":"...",
  "departmentId":"d_1",
  "teamId":"t_1",
  "role":"EMPLOYEE",
  "active":true,
  "identityStatus":"PENDING",
  "createdAt":"ISO",
  "updatedAt":"ISO"
  }
  ],
  "page": { "page":1,"size":20,"totalItems":100,"totalPages":5 }
  }

POST /api/v1/crm/people (ADMIN only)
body:
{ "displayName":"...", "email":"...", "departmentId":"d_1", "teamId":"t_1", "role":"EMPLOYEE|MANAGER|HR|ADMIN" }
Поведение:
- создать Person active=true, identityStatus=PENDING, keycloakUserId=null
- записать outbox событие PersonCreated
  Ответ data:
  {
  "id":"p_999",
  "displayName":"...",
  "email":"...",
  "departmentId":"d_1",
  "teamId":"t_1",
  "role":"EMPLOYEE",
  "active":true,
  "identityStatus":"PENDING",
  "keycloakUserId": null,
  "createdAt":"ISO",
  "updatedAt":"ISO"
  }

GET /api/v1/crm/people/{personId}
Access:
- ADMIN/MANAGER/HR: любой person
- EMPLOYEE: только self (personId из JWT)
  Ответ data:
- Для ADMIN включить keycloakUserId и lastIdentityError
- Для остальных скрыть keycloakUserId/lastIdentityError (оставить identityStatus)
  Пример data (ADMIN):
  {
  "id":"p_1",
  "displayName":"...",
  "email":"...",
  "departmentId":"d_1",
  "teamId":"t_1",
  "role":"EMPLOYEE",
  "active":true,
  "identityStatus":"PROVISIONED",
  "keycloakUserId":"kc_xxx",
  "lastIdentityError": null,
  "createdAt":"ISO",
  "updatedAt":"ISO"
  }

PATCH /api/v1/crm/people/{personId} (ADMIN only)
body (partial):
{ "displayName":"...", "departmentId":"d_2", "teamId":"t_3", "role":"MANAGER", "active": true|false }
Поведение:
- обновить Person
- если active меняется на false -> identityStatus=DEPROVISIONED (логическое), outbox PersonDeactivated
- иначе -> outbox PersonUpdated
  Ответ data: person (как для ADMIN)

DELETE /api/v1/crm/people/{personId} (ADMIN only)
Поведение:
- для MVP делай soft-delete: active=false
- identityStatus=DEPROVISIONED
- outbox PersonDeactivated
  Ответ data: { "deleted": true }

(Мостик для MVP, если нет consumer от provisioning)
POST /api/v1/crm/people/{personId}/identity
Access: ADMIN only
body:
{ "identityStatus":"PROVISIONED|FAILED|DEPROVISIONED|PENDING", "keycloakUserId":"kc_xxx", "lastIdentityError":"..." }
Ответ data: person (ADMIN view)
Назначение: ручной апдейт статуса provisioning.

4) TASKS/Episodes
   GET /api/v1/crm/tasks?status=&ownerId=&assigneeId=&participantId=&periodFrom=&periodTo=&page=1&size=20
   Access:
- ADMIN/MANAGER/HR: можно (в MVP без scope ограничений)
- EMPLOYEE: возвращать только те задачи, где он owner/assignee/participant (игнорируя фильтры, которые расширяют доступ)
  Ответ data:
  {
  "items":[
  {
  "id":"task_1",
  "title":"Quarter review",
  "description":"...",
  "status":"ACTIVE",
  "period": { "from":"2026-01-01","to":"2026-01-31" },
  "ownerId":"p_10",
  "assigneeId":"p_11",
  "participantIds":["p_11","p_12"],
  "createdAt":"ISO",
  "updatedAt":"ISO",
  "closedAt": null
  }
  ],
  "page": { "page":1,"size":20,"totalItems":50,"totalPages":3 }
  }

POST /api/v1/crm/tasks
Access: MANAGER/ADMIN (HR optional запретить)
body:
{
"title":"...",
"description":"...",
"period": { "from":"YYYY-MM-DD","to":"YYYY-MM-DD" },
"ownerId":"p_10",
"assigneeId":"p_11",
"participantIds":["p_11","p_12"]
}
Поведение:
- проверить существование и active всех людей
- создать task, status=DRAFT (или ACTIVE — выбери DRAFT по умолчанию)
- outbox TaskCreated
  Ответ data: task object

GET /api/v1/crm/tasks/{taskId}
Access:
- ADMIN/MANAGER/HR: любой
- EMPLOYEE: только если он owner/assignee/participant
  Ответ data: task object

PATCH /api/v1/crm/tasks/{taskId}
Access: MANAGER/ADMIN
body (partial):
{ "title":"...", "description":"...", "status":"DRAFT|ACTIVE", "period":{...}, "assigneeId":"...", "participantIds":[...] }
Поведение:
- валидации как выше
- outbox TaskUpdated
  Ответ data: task object

POST /api/v1/crm/tasks/{taskId}/close
Access: MANAGER/ADMIN
Ответ data:
{ "id":"task_1", "status":"CLOSED", "closedAt":"ISO", "updatedAt":"ISO" }
+ outbox TaskClosed

5) EVENTS MONITORING (для диагностики)
   GET /api/v1/crm/outbox?status=NEW|SENT|FAILED&page=1&size=20 (ADMIN only)
   Ответ data:
   {
   "items":[
   { "id":"evt_1","type":"PersonCreated","routingKey":"crm.person.created","status":"SENT","createdAt":"ISO","sentAt":"ISO" }
   ],
   "page": { ... }
   }
   POST /api/v1/crm/outbox/{eventId}/replay (ADMIN only) -> data { "replayed": true }

OPENAPI/DTO:
- Все DTO должны иметь OpenAPI annotations и примеры
- Не возвращать лишних полей
- Строго соблюдать схемы JSON выше

БАЗА ДАННЫХ (Flyway):
- persons
- tasks
- task_participants (many-to-many)
- outbox_events
- (опционально) inbox_identity_events если будешь делать consumer от provisioning
  Обязательно: индексы по email unique, по teamId/departmentId, по status/period.

КОД-СТРУКТУРА:
- package: com.univerliga.crm
- controller/*
- dto/*
- service/*
- repository/*
- messaging/* (outbox publisher, rabbit config, optional consumers)
- security/* (jwt converter, access checks)
- config/*
- error/*
- util/*

НЕФУНКЦИОНАЛЬНОЕ:
- Используй optimistic locking (@Version) на Person и Task.
- Таймауты/ретраи для Rabbit publishing в publisher (и статус FAILED в outbox при проблемах).
- В mock/seed:
    - CommandLineRunner создает 10 people, 10 tasks
    - outbox событий на старте НЕ публикуй автоматически (или публикуй — но объясни в README), главное чтобы scheduler работал

README.md:
- docker compose up --build
- как получить токен (password grant) для admin/manager/hr/employee
- примеры curl для всех endpoints
- как посмотреть rabbitmq management UI
- как проверить что outbox публикует события (описать exchange/keys)
- объяснить почему CRM НЕ создает пользователя в Keycloak синхронно, а делает события для provisioning (избегаем связности).

СГЕНЕРИРУЙ ПОЛНЫЙ КОД ПРОЕКТА: pom.xml, docker-compose.yml, realm-export.json, application.yml, Flyway миграции, классы, README.
Проверь, что сервис стартует, security работает, swagger виден, эндпоинты отвечают, outbox реально публикует события в rabbitmq (можно логировать публикацию).