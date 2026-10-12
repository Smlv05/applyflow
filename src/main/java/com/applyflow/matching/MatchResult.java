package com.applyflow.matching;

import java.util.List;

public record MatchResult(int score, List<MatchedSkill> matchedSkills, List<String> missingSkills) {
}