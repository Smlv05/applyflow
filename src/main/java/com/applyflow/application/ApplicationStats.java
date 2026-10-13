package com.applyflow.application;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record ApplicationStats(
        long total,
        Map<ApplicationStatus, Long> byStatus,
        long applied,
        long interviews,
        long offers,
        double interviewRate,
        double offerRate) {

    /**
     * @param current applications grouped by their current status
     * @param reached applications grouped by every status they ever reached (from history)
     */
    public static ApplicationStats of(List<StatusCount> current, List<StatusCount> reached) {
        Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            byStatus.put(status, 0L);
        }
        current.forEach(c -> byStatus.put(c.status(), c.count()));

        Map<ApplicationStatus, Long> reachedByStatus = new EnumMap<>(ApplicationStatus.class);
        reached.forEach(c -> reachedByStatus.put(c.status(), c.count()));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long applied = reachedByStatus.getOrDefault(ApplicationStatus.APPLIED, 0L);
        long interviews = reachedByStatus.getOrDefault(ApplicationStatus.INTERVIEW, 0L);
        long offers = reachedByStatus.getOrDefault(ApplicationStatus.OFFER, 0L);

        return new ApplicationStats(total, byStatus, applied, interviews, offers,
                percent(interviews, applied), percent(offers, applied));
    }

    private static double percent(long part, long whole) {
        return whole == 0 ? 0.0 : Math.round(1000.0 * part / whole) / 10.0;
    }
}