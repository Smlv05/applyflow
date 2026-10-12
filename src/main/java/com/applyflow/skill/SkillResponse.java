package com.applyflow.skill;

public record SkillResponse(Long id, String name, String category) {

    public static SkillResponse from(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.getCategory());
    }
}