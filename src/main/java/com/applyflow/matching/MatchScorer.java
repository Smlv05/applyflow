package com.applyflow.matching;

import com.applyflow.candidate.SkillLevel;

import java.util.Collection;
import java.util.Map;

public interface MatchScorer {

    /**
     * @param jobSkills       names of the skills the job requires
     * @param candidateSkills candidate skill names mapped to proficiency level
     */
    MatchResult score(Collection<String> jobSkills, Map<String, SkillLevel> candidateSkills);
}