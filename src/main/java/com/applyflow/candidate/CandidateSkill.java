package com.applyflow.candidate;

import com.applyflow.skill.Skill;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "candidate_skills")
public class CandidateSkill {

    @EmbeddedId
    private CandidateSkillId id;

    @MapsId("candidateId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id")
    private CandidateProfile profile;

    @MapsId("skillId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillLevel level;

    protected CandidateSkill() {
        // required by JPA
    }

    public CandidateSkill(CandidateProfile profile, Skill skill, SkillLevel level) {
        this.id = new CandidateSkillId(profile.getId(), skill.getId());
        this.profile = profile;
        this.skill = skill;
        this.level = level;
    }

    public CandidateSkillId getId() { return id; }
    public CandidateProfile getProfile() { return profile; }
    public Skill getSkill() { return skill; }
    public SkillLevel getLevel() { return level; }

    public void setLevel(SkillLevel level) { this.level = level; }

    @Override
    public boolean equals(Object o) {
        return o instanceof CandidateSkill other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}