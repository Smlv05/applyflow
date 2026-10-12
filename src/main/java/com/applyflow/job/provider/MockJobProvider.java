package com.applyflow.job.provider;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class MockJobProvider implements JobProvider {

    @Override
    public String source() {
        return "mock";
    }

    @Override
    public List<RawJob> fetchJobs() {
        return List.of(
                new RawJob("m-001", "Senior Java Developer", "Nordic Pay", "Warsaw", false,
                        "Build payment services with Spring Boot and PostgreSQL.",
                        "https://example.com/jobs/m-001", 22000, 28000, "PLN",
                        Instant.parse("2026-10-01T09:00:00Z"),
                        List.of("Java", "Spring Boot", "PostgreSQL", "Docker")),
                new RawJob("m-002", "QA Automation Engineer", "Shopline", "Warsaw", true,
                        "Own the API and UI test automation framework.",
                        "https://example.com/jobs/m-002", 16000, 21000, "PLN",
                        Instant.parse("2026-10-02T09:00:00Z"),
                        List.of("Java", "Selenium", "REST Assured", "JUnit", "CI/CD")),
                new RawJob("m-003", "SDET", "CloudDesk", "Krakow", true,
                        "Design test infrastructure for event-driven services.",
                        "https://example.com/jobs/m-003", 18000, 24000, "PLN",
                        Instant.parse("2026-10-03T09:00:00Z"),
                        List.of("Java", "Playwright", "Kafka", "Docker")),
                new RawJob("m-004", "Backend Engineer", "Streamly", "Remote", true,
                        "Develop high-load backend services on AWS.",
                        "https://example.com/jobs/m-004", 20000, 27000, "PLN",
                        Instant.parse("2026-10-04T09:00:00Z"),
                        List.of("Java", "Spring Boot", "Kafka", "AWS", "Kubernetes")),
                new RawJob("m-005", "Performance Test Engineer", "BankTech", "Warsaw", false,
                        "Load and stress testing of core banking systems.",
                        "https://example.com/jobs/m-005", 17000, 23000, "PLN",
                        Instant.parse("2026-10-05T09:00:00Z"),
                        List.of("Java", "Linux", "CI/CD", "Docker")),
                new RawJob("m-006", "DevOps Engineer", "InfraWorks", "Gdansk", true,
                        "Run Kubernetes platform and CI/CD pipelines.",
                        "https://example.com/jobs/m-006", 19000, 26000, "PLN",
                        Instant.parse("2026-10-06T09:00:00Z"),
                        List.of("Docker", "Kubernetes", "Linux", "AWS", "CI/CD")),
                new RawJob("m-007", "Junior Java Developer", "StartLab", "Wroclaw", false,
                        "Grow as a backend developer in a friendly team.",
                        "https://example.com/jobs/m-007", 9000, 12000, "PLN",
                        Instant.parse("2026-10-07T09:00:00Z"),
                        List.of("Java", "SQL", "Git", "Maven")),
                new RawJob("m-008", "Staff Backend Engineer", "DataGrid", "Remote", true,
                        "Lead architecture of a data-intensive platform.",
                        "https://example.com/jobs/m-008", 30000, 38000, "PLN",
                        Instant.parse("2026-10-08T09:00:00Z"),
                        List.of("Java", "Spring Boot", "Hibernate", "PostgreSQL", "Redis", "RabbitMQ")));
    }
}