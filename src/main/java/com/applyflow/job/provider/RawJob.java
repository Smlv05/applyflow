package com.applyflow.job.provider;

import java.time.Instant;
import java.util.List;

public record RawJob(
        String externalId,
        String title,
        String company,
        String location,
        boolean remote,
        String description,
        String url,
        Integer salaryMin,
        Integer salaryMax,
        String salaryCurrency,
        Instant publishedAt,
        List<String> skills) {
}