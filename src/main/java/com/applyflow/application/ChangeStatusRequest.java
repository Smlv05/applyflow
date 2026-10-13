package com.applyflow.application;

import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull ApplicationStatus status) {
}