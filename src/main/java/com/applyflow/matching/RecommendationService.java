package com.applyflow.matching;

import com.applyflow.candidate.CandidateProfile;
import com.applyflow.candidate.CandidateProfileRepository;
import com.applyflow.candidate.SkillLevel;
import com.applyflow.common.PageResponse;
import com.applyflow.job.Job;
import com.applyflow.job.JobRepository;
import com.applyflow.job.JobResponse;
import com.applyflow.job.JobSkillRow;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Comparator<ScoredJob> BEST_FIRST =
            Comparator.comparingInt(ScoredJob::score).reversed()
                    .thenComparing(ScoredJob::publishedAt, Comparator.nullsLast(Comparator.<Instant>reverseOrder()))
                    .thenComparing(ScoredJob::jobId, Comparator.<Long>reverseOrder());

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

        // 1. score every candidate job from light (job, skill) rows, without loading entities
        Map<Long, List<JobSkillRow>> rowsByJob = jobRepository.findSkillRowsOfJobsSharingSkills(skillIds).stream()
                .collect(Collectors.groupingBy(JobSkillRow::jobId));

        List<ScoredJob> ranked = rowsByJob.entrySet().stream()
                .map(entry -> score(entry.getKey(), entry.getValue(), levelsByName))
                .filter(job -> job.score() >= safeMinScore)
                .sorted(BEST_FIRST)
                .toList();

        // 2. load full job data only for the requested page
        int from = (int) Math.min((long) safePage * safeSize, ranked.size());
        int to = Math.min(from + safeSize, ranked.size());
        List<ScoredJob> pageItems = ranked.subList(from, to);

        Map<Long, Job> jobsById = jobRepository.findAllById(pageItems.stream().map(ScoredJob::jobId).toList())
                .stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));

        List<JobRecommendation> content = pageItems.stream()
                .map(item -> new JobRecommendation(
                        JobResponse.from(jobsById.get(item.jobId())),
                        item.score(),
                        item.result().matchedSkills(),
                        item.result().missingSkills()))
                .toList();

        return PageResponse.from(new PageImpl<>(content, pageRequest, ranked.size()));
    }

    private ScoredJob score(Long jobId, List<JobSkillRow> rows, Map<String, SkillLevel> levelsByName) {
        List<String> skillNames = rows.stream().map(JobSkillRow::skillName).toList();
        return new ScoredJob(jobId, rows.get(0).publishedAt(), scorer.score(skillNames, levelsByName));
    }

    private record ScoredJob(Long jobId, Instant publishedAt, MatchResult result) {

        int score() {
            return result.score();
        }
    }
}