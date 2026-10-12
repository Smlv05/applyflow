package com.applyflow.candidate.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ProfileRequest(
        @Size(max = 5000) String summary,
        @Size(max = 255) String location,
        @Min(0) @Max(60) int yearsOfExperience) {
}