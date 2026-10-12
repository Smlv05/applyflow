package com.applyflow.job;

import com.applyflow.job.provider.JobProvider;
import com.applyflow.job.provider.RawJob;
import com.applyflow.skill.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobImportService {

    private static final Logger log = LoggerFactory.getLogger(JobImportService.class);

    private final List<JobProvider> providers;
    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;

    public JobImportService(List<JobProvider> providers,
                            JobRepository jobRepository,
                            SkillRepository skillRepository) {
        this.providers = providers;
        this.jobRepository = jobRepository;
        this.skillRepository = skillRepository;
    }

    public List<ImportResult> importAll() {
        return providers.stream().map(this::importFrom).toList();
    }

    private ImportResult importFrom(JobProvider provider) {
        List<RawJob> rawJobs = provider.fetchJobs();
        int created = 0;
        int skipped = 0;

        for (RawJob raw : rawJobs) {
            if (jobRepository.existsBySourceAndExternalId(provider.source(), raw.externalId())) {
                skipped++;
                continue;
            }
            try {
                jobRepository.save(toJob(provider.source(), raw));
                created++;
            } catch (DataIntegrityViolationException e) {
                // a concurrent import inserted the same (source, external_id) first
                skipped++;
            }
        }

        log.info("Imported jobs from '{}': fetched={}, created={}, skipped={}",
                provider.source(), rawJobs.size(), created, skipped);
        return new ImportResult(provider.source(), rawJobs.size(), created, skipped);
    }

    private Job toJob(String source, RawJob raw) {
        Job job = new Job(source, raw.externalId(), raw.title(), raw.company());
        job.setDetails(raw.location(), raw.remote(), raw.description(), raw.url(),
                raw.salaryMin(), raw.salaryMax(), raw.salaryCurrency(), raw.publishedAt());
        raw.skills().forEach(name ->
                skillRepository.findByNameIgnoreCase(name).ifPresent(job::addSkill));
        return job;
    }
}