package com.applyflow.application;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndJobId(Long userId, Long jobId);

    @EntityGraph(attributePaths = "job")
    List<Application> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "job")
    List<Application> findAllByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, ApplicationStatus status);
}