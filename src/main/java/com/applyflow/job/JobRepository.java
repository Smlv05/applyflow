package com.applyflow.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Optional<Job> findBySourceAndExternalId(String source, String externalId);

    boolean existsBySourceAndExternalId(String source, String externalId);
}