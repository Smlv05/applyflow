package com.applyflow.job;

import com.applyflow.job.provider.JobProvider;
import com.applyflow.job.provider.RawJob;
import com.applyflow.skill.Skill;
import com.applyflow.skill.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobImportServiceTest {

    @Mock
    private JobProvider provider;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SkillRepository skillRepository;

    private JobImportService service;

    @BeforeEach
    void setUp() {
        service = new JobImportService(List.of(provider), jobRepository, skillRepository);
        when(provider.source()).thenReturn("test");
    }

    @Test
    void importAll_createsNewJobsAndSkipsExisting() {
        when(provider.fetchJobs()).thenReturn(List.of(raw("1", List.of("Java", "Unknown")), raw("2", List.of())));
        when(jobRepository.existsBySourceAndExternalId("test", "1")).thenReturn(false);
        when(jobRepository.existsBySourceAndExternalId("test", "2")).thenReturn(true);
        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(new Skill("Java", "LANGUAGE")));
        when(skillRepository.findByNameIgnoreCase("Unknown")).thenReturn(Optional.empty());

        List<ImportResult> results = service.importAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(new ImportResult("test", 2, 1, 1));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository, times(1)).save(captor.capture());
        Job saved = captor.getValue();
        assertThat(saved.getExternalId()).isEqualTo("1");
        assertThat(saved.getSkills()).hasSize(1);
    }

    @Test
    void importAll_whenConcurrentInsertWins_countsAsSkipped() {
        when(provider.fetchJobs()).thenReturn(List.of(raw("1", List.of())));
        when(jobRepository.existsBySourceAndExternalId("test", "1")).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        ImportResult result = service.importAll().get(0);

        assertThat(result.created()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
    }

    private static RawJob raw(String externalId, List<String> skills) {
        return new RawJob(externalId, "Title " + externalId, "Company", "Warsaw", false,
                "desc", "https://example.com/" + externalId, 1000, 2000, "PLN",
                Instant.parse("2026-10-01T09:00:00Z"), skills);
    }
}