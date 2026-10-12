package com.applyflow.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {

    Optional<CandidateProfile> findByUserId(Long userId);

    @Modifying
    @Query(value = "INSERT INTO candidate_profiles (user_id) VALUES (:userId) ON CONFLICT (user_id) DO NOTHING",
            nativeQuery = true)
    void insertIfAbsent(@Param("userId") Long userId);
}