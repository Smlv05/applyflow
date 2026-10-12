package com.applyflow.candidate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "candidate_profiles")
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(columnDefinition = "text")
    private String summary;

    private String location;

    @Column(name = "years_of_experience", nullable = false)
    private int yearsOfExperience;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CandidateSkill> skills = new HashSet<>();

    protected CandidateProfile() {
        // required by JPA
    }

    public CandidateProfile(Long userId, String summary, String location, int yearsOfExperience) {
        this.userId = userId;
        this.summary = summary;
        this.location = location;
        this.yearsOfExperience = yearsOfExperience;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void update(String summary, String location, int yearsOfExperience) {
        this.summary = summary;
        this.location = location;
        this.yearsOfExperience = yearsOfExperience;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSummary() { return summary; }
    public String getLocation() { return location; }
    public int getYearsOfExperience() { return yearsOfExperience; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<CandidateSkill> getSkills() { return skills; }
}