package com.applyflow.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotNull Long jobId,
        @Size(max = 5000) String notes) {
}