package com.applyflow.matching;

import com.applyflow.candidate.SkillLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WeightedSkillMatchScorerTest {

    private final WeightedSkillMatchScorer scorer = new WeightedSkillMatchScorer();

    @Test
    void allSkillsAtExpertLevel_scoresHundred() {
        MatchResult result = scorer.score(
                List.of("Java", "Docker"),
                Map.of("Java", SkillLevel.EXPERT, "Docker", SkillLevel.EXPERT));

        assertThat(result.score()).isEqualTo(100);
        assertThat(result.missingSkills()).isEmpty();
        assertThat(result.matchedSkills()).hasSize(2);
    }

    @Test
    void noOverlap_scoresZeroAndListsAllMissing() {
        MatchResult result = scorer.score(
                List.of("Kafka", "Redis"),
                Map.of("Java", SkillLevel.EXPERT));

        assertThat(result.score()).isZero();
        assertThat(result.matchedSkills()).isEmpty();
        assertThat(result.missingSkills()).containsExactly("Kafka", "Redis");
    }

    @Test
    void partialMatch_weightsByLevel() {
        // Java EXPERT (1.0) + Docker INTERMEDIATE (0.5) = 1.5 of 4 required => 37.5 => 38
        MatchResult result = scorer.score(
                List.of("Java", "Docker", "Kafka", "SQL"),
                Map.of("Java", SkillLevel.EXPERT, "Docker", SkillLevel.INTERMEDIATE));

        assertThat(result.score()).isEqualTo(38);
        assertThat(result.matchedSkills()).extracting(MatchedSkill::name).containsExactly("Java", "Docker");
        assertThat(result.missingSkills()).containsExactly("Kafka", "SQL");
    }

    @Test
    void higherLevel_givesHigherScore() {
        int beginner = scorer.score(List.of("Java"), Map.of("Java", SkillLevel.BEGINNER)).score();
        int intermediate = scorer.score(List.of("Java"), Map.of("Java", SkillLevel.INTERMEDIATE)).score();
        int advanced = scorer.score(List.of("Java"), Map.of("Java", SkillLevel.ADVANCED)).score();
        int expert = scorer.score(List.of("Java"), Map.of("Java", SkillLevel.EXPERT)).score();

        assertThat(beginner).isLessThan(intermediate);
        assertThat(intermediate).isLessThan(advanced);
        assertThat(advanced).isLessThan(expert);
    }

    @Test
    void skillNamesAreComparedCaseInsensitively() {
        MatchResult result = scorer.score(
                List.of("Spring Boot"),
                Map.of("spring boot", SkillLevel.EXPERT));

        assertThat(result.score()).isEqualTo(100);
    }

    @Test
    void jobWithoutSkills_scoresZero() {
        MatchResult result = scorer.score(List.of(), Map.of("Java", SkillLevel.EXPERT));

        assertThat(result.score()).isZero();
        assertThat(result.matchedSkills()).isEmpty();
        assertThat(result.missingSkills()).isEmpty();
    }

    @Test
    void duplicateJobSkills_areCountedOnce() {
        MatchResult result = scorer.score(
                List.of("Java", "java"),
                Map.of("Java", SkillLevel.EXPERT));

        assertThat(result.score()).isEqualTo(100);
        assertThat(result.matchedSkills()).hasSize(1);
    }
}