CREATE TABLE users (
    id            UUID PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'MEMBER'))
);

CREATE TABLE projects (
    id             UUID PRIMARY KEY,
    project_key    VARCHAR(10)  UNIQUE NOT NULL,
    name           VARCHAR(100) NOT NULL,
    description    TEXT,
    issue_sequence INT          NOT NULL DEFAULT 0,
    owner_id       UUID         NOT NULL REFERENCES users (id),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_projects_key CHECK (project_key ~ '^[A-Z0-9]{2,10}$'),
    CONSTRAINT chk_projects_sequence CHECK (issue_sequence >= 0)
);

CREATE TABLE sprints (
    id         UUID PRIMARY KEY,
    project_id UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    goal       TEXT,
    start_date DATE,
    end_date   DATE,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sprints_status CHECK (status IN ('PLANNED', 'ACTIVE', 'CLOSED')),
    CONSTRAINT chk_sprints_dates CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date)
);

CREATE UNIQUE INDEX idx_one_active_sprint_per_project
    ON sprints (project_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_sprints_project_id ON sprints (project_id);

CREATE TABLE issues (
    id           UUID PRIMARY KEY,
    issue_key    VARCHAR(20)  UNIQUE NOT NULL,
    project_id   UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    sprint_id    UUID         REFERENCES sprints (id) ON DELETE SET NULL,
    assignee_id  UUID         REFERENCES users (id) ON DELETE SET NULL,
    reporter_id  UUID         NOT NULL REFERENCES users (id),
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'BACKLOG',
    priority     VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    story_points INT,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_issues_status CHECK (status IN ('BACKLOG', 'TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
    CONSTRAINT chk_issues_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_issues_story_points CHECK (story_points IS NULL OR story_points >= 0)
);

CREATE INDEX idx_issues_project_id ON issues (project_id);
CREATE INDEX idx_issues_sprint_id ON issues (sprint_id);
CREATE INDEX idx_issues_assignee_id ON issues (assignee_id);
CREATE INDEX idx_issues_status ON issues (status);

CREATE TABLE issue_audit_logs (
    id         UUID PRIMARY KEY,
    issue_id   UUID         NOT NULL REFERENCES issues (id) ON DELETE CASCADE,
    changed_by UUID         NOT NULL REFERENCES users (id),
    field_name VARCHAR(50)  NOT NULL,
    old_value  VARCHAR(255),
    new_value  VARCHAR(255),
    changed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_audit_field CHECK (field_name IN ('STATUS', 'ASSIGNEE', 'PRIORITY', 'SPRINT', 'STORY_POINTS'))
);

CREATE INDEX idx_audit_logs_issue_id ON issue_audit_logs (issue_id, changed_at);
