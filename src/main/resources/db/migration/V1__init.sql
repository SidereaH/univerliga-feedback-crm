CREATE TABLE persons (
    id VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(200) NOT NULL,
    email VARCHAR(320) NOT NULL,
    department_id VARCHAR(64) NOT NULL,
    team_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL,
    identity_status VARCHAR(32) NOT NULL,
    keycloak_user_id VARCHAR(128),
    last_identity_error VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_persons_email ON persons (email);
CREATE INDEX idx_persons_department_id ON persons (department_id);
CREATE INDEX idx_persons_team_id ON persons (team_id);
CREATE INDEX idx_persons_active ON persons (active);

CREATE TABLE tasks (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(5000),
    status VARCHAR(32) NOT NULL,
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,
    owner_id VARCHAR(64) NOT NULL,
    assignee_id VARCHAR(64),
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_period_valid CHECK (period_from <= period_to)
);

CREATE INDEX idx_tasks_status ON tasks (status);
CREATE INDEX idx_tasks_owner_id ON tasks (owner_id);
CREATE INDEX idx_tasks_assignee_id ON tasks (assignee_id);
CREATE INDEX idx_tasks_period_from ON tasks (period_from);
CREATE INDEX idx_tasks_period_to ON tasks (period_to);

CREATE TABLE task_participants (
    task_id VARCHAR(64) NOT NULL,
    person_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (task_id, person_id)
);

CREATE INDEX idx_task_participants_person_id ON task_participants (person_id);

CREATE TABLE outbox_events (
    id VARCHAR(64) PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    type VARCHAR(64) NOT NULL,
    routing_key VARCHAR(128) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(2000)
);

CREATE INDEX idx_outbox_status_next_attempt ON outbox_events (status, next_attempt_at);
CREATE INDEX idx_outbox_created_at ON outbox_events (created_at);
