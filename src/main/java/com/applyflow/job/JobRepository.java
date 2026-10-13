package com.applyflow.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Optional<Job> findBySourceAndExternalId(String source, String externalId);

    boolean existsBySourceAndExternalId(String source, String externalId);

    /**
     * Skill rows of every job that shares at least one skill with the given ones.
     * All skills of those jobs are returned (not only the shared ones), because the
     * match score needs the full list of required skills.
     */
    @Query("""
            select new com.applyflow.job.JobSkillRow(j.id, j.publishedAt, s.name)
            from Job j join j.skills s
            where j.id in (
                select j2.id from Job j2 join j2.skills s2 where s2.id in :skillIds
            )
            """)
    List<JobSkillRow> findSkillRowsOfJobsSharingSkills(@Param("skillIds") Collection<Long> skillIds);
}