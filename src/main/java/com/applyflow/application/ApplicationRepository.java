package com.applyflow.application;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndJobId(Long userId, Long jobId);

    @EntityGraph(attributePaths = "job")
    List<Application> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "job")
    List<Application> findAllByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, ApplicationStatus status);

    @Query("""
            select new com.applyflow.application.StatusCount(a.status, count(a))
            from Application a
            where a.userId = :userId
            group by a.status
            """)
    List<StatusCount> countByCurrentStatus(@Param("userId") Long userId);

    @Query("""
            select new com.applyflow.application.StatusCount(c.toStatus, count(distinct c.application.id))
            from ApplicationStatusChange c
            where c.application.userId = :userId
            group by c.toStatus
            """)
    List<StatusCount> countByReachedStatus(@Param("userId") Long userId);
}