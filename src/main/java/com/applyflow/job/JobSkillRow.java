package com.applyflow.job;

import java.time.Instant;

/** One (job, required skill) pair. A light projection used for scoring without loading entities. */
public record JobSkillRow(Long jobId, Instant publishedAt, String skillName) {
}