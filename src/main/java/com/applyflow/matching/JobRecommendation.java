package com.applyflow.matching;

import com.applyflow.job.JobResponse;

import java.util.List;

public record JobRecommendation(
        JobResponse job,
        int score,
        List<MatchedSkill> matchedSkills,
        List<String> missingSkills) {
}