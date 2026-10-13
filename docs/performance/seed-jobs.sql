-- Load-test data: 20,000 generated jobs, 4 skills each (80,000 job_skills rows).
-- Safe to run more than once. Remove with: DELETE FROM jobs WHERE source = 'seed';

INSERT INTO jobs (source, external_id, title, company, location, remote, description, url,
                  salary_min, salary_max, salary_currency, published_at)
SELECT 'seed',
       'seed-' || g,
       (ARRAY['Java Developer', 'Backend Engineer', 'QA Automation Engineer', 'SDET',
        'DevOps Engineer', 'Data Engineer', 'Platform Engineer'])[1 + g % 7],
       'Company ' || (g % 500),
       (ARRAY['Warsaw', 'Krakow', 'Wroclaw', 'Gdansk', 'Remote'])[1 + g % 5],
       g % 5 = 4,
       'Generated job description number ' || g,
       'https://example.com/jobs/seed-' || g,
       8000 + (g % 20) * 1000,
       12000 + (g % 20) * 1000,
       'PLN',
       now() - (g || ' minutes')::interval
FROM generate_series(1, 20000) AS g
ON CONFLICT (source, external_id) DO NOTHING;

INSERT INTO job_skills (job_id, skill_id)
SELECT j.id, 1 + ((j.id + o.off) % 19)
FROM jobs j
    CROSS JOIN (VALUES (0), (3), (7), (11)) AS o(off)
WHERE j.source = 'seed'
ON CONFLICT DO NOTHING;