CREATE TABLE jobs (
                      id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      source          VARCHAR(50)  NOT NULL,
                      external_id     VARCHAR(255) NOT NULL,
                      title           VARCHAR(255) NOT NULL,
                      company         VARCHAR(255) NOT NULL,
                      location        VARCHAR(255),
                      remote          BOOLEAN      NOT NULL DEFAULT FALSE,
                      description     TEXT,
                      url             VARCHAR(1000),
                      salary_min      INTEGER,
                      salary_max      INTEGER,
                      salary_currency VARCHAR(3),
                      published_at    TIMESTAMPTZ,
                      created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
                      CONSTRAINT uq_jobs_source_external UNIQUE (source, external_id),
                      CONSTRAINT chk_jobs_salary_range CHECK (
                          salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max)
);

CREATE INDEX idx_jobs_published_at ON jobs (published_at DESC);

CREATE TABLE job_skills (
                            job_id   BIGINT NOT NULL,
                            skill_id BIGINT NOT NULL,
                            PRIMARY KEY (job_id, skill_id),
                            CONSTRAINT fk_job_skills_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
                            CONSTRAINT fk_job_skills_skill FOREIGN KEY (skill_id) REFERENCES skills (id)
);

CREATE INDEX idx_job_skills_skill ON job_skills (skill_id);