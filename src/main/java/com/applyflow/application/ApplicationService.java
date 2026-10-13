package com.applyflow.application;

import com.applyflow.common.exception.DuplicateApplicationException;
import com.applyflow.common.exception.ResourceNotFoundException;
import com.applyflow.job.Job;
import com.applyflow.job.JobRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    public ApplicationService(ApplicationRepository applicationRepository, JobRepository jobRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public ApplicationResponse create(Long userId, CreateApplicationRequest request) {
        Job job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + request.jobId()));

        if (applicationRepository.existsByUserIdAndJobId(userId, job.getId())) {
            throw new DuplicateApplicationException(job.getId());
        }

        try {
            Application saved = applicationRepository.saveAndFlush(new Application(userId, job, request.notes()));
            return ApplicationResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            // a concurrent request created the same (user_id, job_id) first
            throw new DuplicateApplicationException(job.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> list(Long userId, ApplicationStatus status) {
        List<Application> applications = status == null
                ? applicationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
                : applicationRepository.findAllByUserIdAndStatusOrderByUpdatedAtDesc(userId, status);
        return applications.stream().map(ApplicationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(Long userId, Long id) {
        return ApplicationResponse.from(find(userId, id));
    }

    @Transactional(readOnly = true)
    public ApplicationStats stats(Long userId) {
        return ApplicationStats.of(
                applicationRepository.countByCurrentStatus(userId),
                applicationRepository.countByReachedStatus(userId));
    }

    @Transactional
    public ApplicationResponse changeStatus(Long userId, Long id, ApplicationStatus target) {
        Application application = find(userId, id);
        application.changeStatus(target);
        return ApplicationResponse.from(applicationRepository.saveAndFlush(application));
    }

    private Application find(Long userId, Long id) {
        // another user's application is reported as not found, not as forbidden
        return applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }
}