package com.applyflow.application;

import com.applyflow.job.JobResponse;

import java.time.Instant;
import java.util.List;

public record ApplicationResponse(
        Long id,
        ApplicationStatus status,
        String notes,
        Instant appliedAt,
        Instant createdAt,
        Instant updatedAt,
        JobResponse job,
        List<StatusChangeResponse> history) {

    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getStatus(),
                application.getNotes(),
                application.getAppliedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                JobResponse.from(application.getJob()),
                application.getHistory().stream().map(StatusChangeResponse::from).toList());
    }
}