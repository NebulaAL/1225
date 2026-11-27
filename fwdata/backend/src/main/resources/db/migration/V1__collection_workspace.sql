CREATE TABLE bootstrap_lock (
    id INTEGER NOT NULL PRIMARY KEY
);
INSERT INTO bootstrap_lock (id) VALUES (1);

CREATE TABLE accounts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(40) NOT NULL UNIQUE,
    display_name VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(12) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE datasets (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (created_by) REFERENCES accounts(id)
);

CREATE TABLE dataset_fields (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    label VARCHAR(120) NOT NULL,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('SINGLE', 'MULTIPLE', 'TEXT')),
    required BOOLEAN NOT NULL,
    position_no INT NOT NULL,
    UNIQUE (dataset_id, position_no),
    UNIQUE (id, dataset_id),
    FOREIGN KEY (dataset_id) REFERENCES datasets(id)
);

CREATE TABLE field_options (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    field_id BIGINT NOT NULL,
    label VARCHAR(120) NOT NULL,
    position_no INT NOT NULL,
    UNIQUE (field_id, position_no),
    UNIQUE (id, field_id),
    FOREIGN KEY (field_id) REFERENCES dataset_fields(id)
);

CREATE TABLE collection_sources (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    public_key CHAR(32) NOT NULL UNIQUE,
    UNIQUE (dataset_id, name),
    UNIQUE (id, dataset_id),
    FOREIGN KEY (dataset_id) REFERENCES datasets(id)
);

CREATE TABLE submissions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    source_id BIGINT NOT NULL,
    request_key CHAR(32) NOT NULL,
    submitted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE (source_id, request_key),
    UNIQUE (id, dataset_id),
    FOREIGN KEY (source_id, dataset_id) REFERENCES collection_sources(id, dataset_id)
);
CREATE INDEX idx_submissions_dataset_id ON submissions(dataset_id, id);
CREATE INDEX idx_submissions_source_id ON submissions(source_id, id);

CREATE TABLE submission_answers (
    submission_id BIGINT NOT NULL,
    dataset_id BIGINT NOT NULL,
    field_id BIGINT NOT NULL,
    text_value TEXT,
    PRIMARY KEY (submission_id, field_id),
    FOREIGN KEY (submission_id, dataset_id) REFERENCES submissions(id, dataset_id),
    FOREIGN KEY (field_id, dataset_id) REFERENCES dataset_fields(id, dataset_id)
);

CREATE TABLE answer_choices (
    submission_id BIGINT NOT NULL,
    field_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    PRIMARY KEY (submission_id, field_id, option_id),
    FOREIGN KEY (submission_id, field_id) REFERENCES submission_answers(submission_id, field_id),
    FOREIGN KEY (option_id, field_id) REFERENCES field_options(id, field_id)
);
