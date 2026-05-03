CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    login         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    master_id     BIGINT,
    CONSTRAINT fk_users_master
        FOREIGN KEY (master_id)
            REFERENCES masters (id)
            ON DELETE SET NULL
);

CREATE INDEX idx_users_master_id ON users (master_id);

INSERT INTO users (login, password_hash, role, is_active)
VALUES (
    'admin',
    'pbkdf2_sha256:120000:YXV0b3NlcnZpY2UtYWRtaW4tc2FsdA==:awRAx33Cv+1rIbys7+HKLwdGjQMdWTzrVSJXoK5OtuQ=',
    'ADMIN',
    TRUE
);
