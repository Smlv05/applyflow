CREATE TABLE candidate_profiles (
                                    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    user_id             BIGINT      NOT NULL,
                                    summary             TEXT,
                                    location            VARCHAR(255),
                                    years_of_experience INTEGER     NOT NULL DEFAULT 0,
                                    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    CONSTRAINT uq_candidate_profiles_user UNIQUE (user_id),
                                    CONSTRAINT fk_candidate_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                    CONSTRAINT chk_candidate_profiles_years CHECK (years_of_experience >= 0)
);

CREATE TABLE skills (
                        id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        name     VARCHAR(100) NOT NULL,
                        category VARCHAR(50)
);

CREATE UNIQUE INDEX uq_skills_name_lower ON skills (lower(name));

CREATE TABLE candidate_skills (
                                  candidate_id BIGINT      NOT NULL,
                                  skill_id     BIGINT      NOT NULL,
                                  level        VARCHAR(20) NOT NULL,
                                  PRIMARY KEY (candidate_id, skill_id),
                                  CONSTRAINT fk_candidate_skills_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_profiles (id) ON DELETE CASCADE,
                                  CONSTRAINT fk_candidate_skills_skill FOREIGN KEY (skill_id) REFERENCES skills (id),
                                  CONSTRAINT chk_candidate_skills_level CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'))
);

CREATE INDEX idx_candidate_skills_skill ON candidate_skills (skill_id);