package com.applyflow.job;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class JobSpecifications {

    private JobSpecifications() {
    }

    public static Specification<Job> from(JobFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(filter.q())) {
                String pattern = like(filter.q());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("company")), pattern, '\\')));
            }
            if (hasText(filter.location())) {
                predicates.add(cb.like(cb.lower(root.get("location")), like(filter.location()), '\\'));
            }
            if (filter.remote() != null) {
                predicates.add(cb.equal(root.get("remote"), filter.remote()));
            }
            if (filter.minSalary() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Integer>get("salaryMax"), filter.minSalary()));
            }
            if (hasText(filter.skill())) {
                query.distinct(true);
                var skills = root.join("skills");
                predicates.add(cb.equal(cb.lower(skills.get("name")), filter.skill().trim().toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Builds a case-insensitive "contains" pattern with LIKE wildcards escaped. */
    private static String like(String value) {
        String escaped = value.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}