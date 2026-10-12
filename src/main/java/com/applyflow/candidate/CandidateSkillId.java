package com.applyflow.candidate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CandidateSkillId implements Serializable {

    @Column(name = "candidate_id")
    private Long candidateId;

    @Column(name = "skill_id")
    private Long skillId;

    protected CandidateSkillId() {
        // required by JPA
    }

    public CandidateSkillId(Long candidateId, Long skillId) {
        this.candidateId = candidateId;
        this.skillId = skillId;
    }

    public Long getCandidateId() { return candidateId; }
    public Long getSkillId() { return skillId; }

    @Override
    public boolean equals(Object o) {
        return o instanceof CandidateSkillId other
                && Objects.equals(candidateId, other.candidateId)
                && Objects.equals(skillId, other.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(candidateId, skillId);
    }
}