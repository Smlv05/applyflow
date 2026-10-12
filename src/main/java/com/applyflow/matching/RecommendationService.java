package com.applyflow.matching;

import com.applyflow.candidate.CandidateProfile;
import com.applyflow.candidate.CandidateProfileRepository;
import com.applyflow.candidate.SkillLevel;
import com.applyflow.common.PageResponse;
import com.applyflow.job.Job;
import com.applyflow.job.JobRepository;
import com.applyflow.job.JobResponse;
import com.applyflow.skill.Skill;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class RecommendationService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Comparator<JobRecommendation> BEST_FIRST =
            Comparator.comparingInt(JobRecommendation::score).reversed()
                    .thenComparing((JobRecommendation r) -> r.job().publishedAt(),
                            Comparator.nullsLast(Comparator.<Instant>reverseOrder()))
                    .thenComparing((JobRecommendation r) -> r.job().id(), Comparator.<Long>reverseOrder());

    private final CandidateProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final MatchScorer scorer;

    public RecommendationService(CandidateProfileRepository profileRepository,
                                 JobRepository jobRepository,
                                 MatchScorer scorer) {
        this.profileRepository = profileRepository;
        this.jobRepository = jobRepository;
        this.scorer = scorer;
    }

    @Transactional(readOnly = true)
    public PageResponse<JobRecommendation> recommend(Long userId, int minScore, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safeMinScore = Math.min(Math.max(minScore, 0), 100);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);

        Map<String, SkillLevel> levelsByName = new HashMap<>();
        Set<Long> skillIds = new HashSet<>();
        Optional<CandidateProfile> profile = profileRepository.findByUserId(userId);
        profile.ifPresent(p -> p.getSkills().forEach(cs -> {
            levelsByName.put(cs.getSkill().getName(), cs.getLevel());
            skillIds.add(cs.getSkill().getId());
        }));

        if (skillIds.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageRequest, 0));
        }

        List<JobRecommendation> ranked = jobRepository.findAllBySkillIds(skillIds).stream()
                .map(job -> toRecommendation(job, levelsByName))
                .filter(r -> r.score() >= safeMinScore)
                .sorted(BEST_FIRST)
                .toList();

        int from = (int) Math.min((long) safePage * safeSize, ranked.size());
        int to = Math.min(from + safeSize, ranked.size());
        return PageResponse.from(new PageImpl<>(ranked.subList(from, to), pageRequest, ranked.size()));
    }

    private JobRecommendation toRecommendation(Job job, Map<String, SkillLevel> levelsByName) {
        List<String> jobSkillNames = job.getSkills().stream().map(Skill::getName).toList();
        MatchResult result = scorer.score(jobSkillNames, levelsByName);
        return new JobRecommendation(
                JobResponse.from(job),
                result.score(),
                result.matchedSkills(),
                result.missingSkills());
    }
}