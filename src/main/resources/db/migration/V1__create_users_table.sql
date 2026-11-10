CREATE TABLE users (
    id            UUID                     PRIMARY KEY,
    email         VARCHAR(255)             NOT NULL UNIQUE,
    password_hash VARCHAR(255)             NOT NULL,
    display_name  VARCHAR(100)             NOT NULL,
    role          VARCHAR(20)              NOT NULL,
    level         VARCHAR(2),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);
