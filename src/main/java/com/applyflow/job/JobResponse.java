package com.applyflow.job;

import com.applyflow.skill.SkillResponse;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record JobResponse(
        Long id,
        String source,
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
        List<SkillResponse> skills) {

    public static JobResponse from(Job job) {
        List<SkillResponse> skills = job.getSkills().stream()
                .map(SkillResponse::from)
                .sorted(Comparator.comparing(SkillResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new JobResponse(
                job.getId(),
                job.getSource(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.isRemote(),
                job.getDescription(),
                job.getUrl(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getSalaryCurrency(),
                job.getPublishedAt(),
                skills);
    }
}