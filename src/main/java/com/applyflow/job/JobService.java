package com.applyflow.job;

import com.applyflow.common.PageResponse;
import com.applyflow.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Sort NEWEST_FIRST = Sort.by(
            Sort.Order.desc("publishedAt").nullsLast(),
            Sort.Order.desc("id"));

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> search(JobFilter filter, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return PageResponse.from(
                jobRepository.findAll(JobSpecifications.from(filter), PageRequest.of(safePage, safeSize, NEWEST_FIRST))
                        .map(JobResponse::from));
    }

    @Transactional(readOnly = true)
    public JobResponse getById(Long id) {
        return jobRepository.findById(id)
                .map(JobResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));
    }
}