CREATE TABLE applications (
                              id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              user_id    BIGINT      NOT NULL,
                              job_id     BIGINT      NOT NULL,
                              status     VARCHAR(20) NOT NULL,
                              notes      TEXT,
                              applied_at TIMESTAMPTZ,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                              updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                              CONSTRAINT uq_applications_user_job UNIQUE (user_id, job_id),
                              CONSTRAINT fk_applications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                              CONSTRAINT fk_applications_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
                              CONSTRAINT chk_applications_status CHECK (
                                  status IN ('SAVED', 'APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN'))
);

CREATE INDEX idx_applications_user_status ON applications (user_id, status);

CREATE TABLE application_status_history (
                                            id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                            application_id BIGINT      NOT NULL,
                                            from_status    VARCHAR(20),
                                            to_status      VARCHAR(20) NOT NULL,
                                            changed_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                                            CONSTRAINT fk_status_history_application FOREIGN KEY (application_id)
                                                REFERENCES applications (id) ON DELETE CASCADE
);

CREATE INDEX idx_status_history_application ON application_status_history (application_id);