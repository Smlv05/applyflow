package com.applyflow.candidate.dto;

import com.applyflow.candidate.CandidateSkill;
import com.applyflow.candidate.SkillLevel;

public record ProfileSkillResponse(Long skillId, String name, String category, SkillLevel level) {

    public static ProfileSkillResponse from(CandidateSkill cs) {
        return new ProfileSkillResponse(
                cs.getSkill().getId(),
                cs.getSkill().getName(),
                cs.getSkill().getCategory(),
                cs.getLevel());
    }
}