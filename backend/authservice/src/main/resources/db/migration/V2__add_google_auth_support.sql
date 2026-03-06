ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS google_subject VARCHAR(120);

ALTER TABLE users
    ADD CONSTRAINT uk_users_google_subject UNIQUE (google_subject);
