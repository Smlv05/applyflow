package com.applyflow.candidate.dto;

import com.applyflow.candidate.CandidateProfile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record ProfileResponse(
        Long id,
        String summary,
        String location,
        int yearsOfExperience,
        List<ProfileSkillResponse> skills,
        Instant updatedAt) {

    public static ProfileResponse from(CandidateProfile profile) {
        List<ProfileSkillResponse> skills = profile.getSkills().stream()
                .map(ProfileSkillResponse::from)
                .sorted(Comparator.comparing(ProfileSkillResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new ProfileResponse(
                profile.getId(),
                profile.getSummary(),
                profile.getLocation(),
                profile.getYearsOfExperience(),
                skills,
                profile.getUpdatedAt());
    }
}