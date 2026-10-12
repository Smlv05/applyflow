package com.applyflow.candidate.dto;

import com.applyflow.candidate.SkillLevel;
import jakarta.validation.constraints.NotNull;

public record AddSkillRequest(
        @NotNull Long skillId,
        @NotNull SkillLevel level) {
}