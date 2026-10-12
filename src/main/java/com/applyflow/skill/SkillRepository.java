package com.applyflow.skill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findAllByOrderByCategoryAscNameAsc();

    Optional<Skill> findByNameIgnoreCase(String name);
}