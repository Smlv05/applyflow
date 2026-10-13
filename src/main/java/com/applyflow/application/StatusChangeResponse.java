package com.applyflow.application;

import java.time.Instant;

public record StatusChangeResponse(ApplicationStatus from, ApplicationStatus to, Instant changedAt) {

    public static StatusChangeResponse from(ApplicationStatusChange change) {
        return new StatusChangeResponse(change.getFromStatus(), change.getToStatus(), change.getChangedAt());
    }
}