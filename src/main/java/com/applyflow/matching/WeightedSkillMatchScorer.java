package com.applyflow.matching;

import com.applyflow.candidate.SkillLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class WeightedSkillMatchScorer implements MatchScorer {

    @Override
    public MatchResult score(Collection<String> jobSkills, Map<String, SkillLevel> candidateSkills) {
        Map<String, SkillLevel> candidate = new HashMap<>();
        candidateSkills.forEach((name, level) -> candidate.put(normalize(name), level));

        // dedupe job skills by normalized name, keeping the first spelling and the original order
        Map<String, String> distinct = new LinkedHashMap<>();
        for (String skill : jobSkills) {
            distinct.putIfAbsent(normalize(skill), skill);
        }

        List<MatchedSkill> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        double total = 0;

        for (String skill : distinct.values()) {
            SkillLevel level = candidate.get(normalize(skill));
            if (level == null) {
                missing.add(skill);
            } else {
                matched.add(new MatchedSkill(skill, level));
                total += weight(level);
            }
        }

        int required = matched.size() + missing.size();
        int score = required == 0 ? 0 : (int) Math.round(100.0 * total / required);
        return new MatchResult(score, matched, missing);
    }

    private static double weight(SkillLevel level) {
        return switch (level) {
            case BEGINNER -> 0.25;
            case INTERMEDIATE -> 0.5;
            case ADVANCED -> 0.8;
            case EXPERT -> 1.0;
        };
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}