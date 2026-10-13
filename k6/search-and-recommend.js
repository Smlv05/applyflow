import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://host.docker.internal:8080';

export const options = {
    scenarios: {
        search: { executor: 'constant-vus', vus: 10, duration: '30s', exec: 'search' },
        recommended: { executor: 'constant-vus', vus: 5, duration: '30s', exec: 'recommended' },
    },
    thresholds: {
        'http_req_duration{scenario:search}': ['p(95)<300'],
        'http_req_duration{scenario:recommended}': ['p(95)<800'],
        http_req_failed: ['rate<0.01'],
    },
};

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export function setup() {
    const email = 'k6-user@test.com';
    const credentials = JSON.stringify({ email, password: 'Password123' });

    // 409 on repeated runs is fine
    http.post(
        `${BASE}/api/auth/register`,
        JSON.stringify({ email, password: 'Password123', firstName: 'K6', lastName: 'User' }),
        { headers: JSON_HEADERS },
    );

    const login = http.post(`${BASE}/api/auth/login`, credentials, { headers: JSON_HEADERS });
    const token = login.json('accessToken');

    // Java, Spring Boot, PostgreSQL, Docker
    [1, 3, 5, 7].forEach((skillId) => {
        http.post(
            `${BASE}/api/profile/skills`,
            JSON.stringify({ skillId, level: 'ADVANCED' }),
            { headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` } },
        );
    });

    return { token };
}

function authHeaders(data) {
    return { Authorization: `Bearer ${data.token}` };
}

const SEARCH_QUERIES = [
    'skill=Java&size=20',
    'remote=true&minSalary=15000&size=20',
    'q=engineer&location=warsaw&size=20',
];

export function search(data) {
    const query = SEARCH_QUERIES[Math.floor(Math.random() * SEARCH_QUERIES.length)];
    const page = Math.floor(Math.random() * 5);
    const res = http.get(`${BASE}/api/jobs?${query}&page=${page}`, {
        headers: authHeaders(data),
        tags: { endpoint: 'search' },
    });
    check(res, { 'search 200': (r) => r.status === 200 });
}

export function recommended(data) {
    const res = http.get(`${BASE}/api/jobs/recommended?size=20`, {
        headers: authHeaders(data),
        tags: { endpoint: 'recommended' },
    });
    check(res, { 'recommended 200': (r) => r.status === 200 });
}