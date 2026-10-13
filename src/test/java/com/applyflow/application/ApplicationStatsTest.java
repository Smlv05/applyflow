package com.applyflow.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatsTest {

    @Test
    void noData_returnsZerosForEveryStatus() {
        ApplicationStats stats = ApplicationStats.of(List.of(), List.of());

        assertThat(stats.total()).isZero();
        assertThat(stats.byStatus()).containsOnlyKeys(ApplicationStatus.values());
        assertThat(stats.byStatus().values()).containsOnly(0L);
        assertThat(stats.interviewRate()).isZero();
        assertThat(stats.offerRate()).isZero();
    }

    @Test
    void calculatesRatesFromReachedStatuses() {
        ApplicationStats stats = ApplicationStats.of(
                List.of(new StatusCount(ApplicationStatus.REJECTED, 2L),
                        new StatusCount(ApplicationStatus.OFFER, 1L),
                        new StatusCount(ApplicationStatus.SAVED, 1L)),
                List.of(new StatusCount(ApplicationStatus.SAVED, 4L),
                        new StatusCount(ApplicationStatus.APPLIED, 3L),
                        new StatusCount(ApplicationStatus.INTERVIEW, 2L),
                        new StatusCount(ApplicationStatus.OFFER, 1L),
                        new StatusCount(ApplicationStatus.REJECTED, 2L)));

        assertThat(stats.total()).isEqualTo(4);
        assertThat(stats.byStatus().get(ApplicationStatus.REJECTED)).isEqualTo(2L);
        assertThat(stats.applied()).isEqualTo(3);
        assertThat(stats.interviews()).isEqualTo(2);
        assertThat(stats.offers()).isEqualTo(1);
        assertThat(stats.interviewRate()).isEqualTo(66.7);
        assertThat(stats.offerRate()).isEqualTo(33.3);
    }

    @Test
    void interviewsWithoutApplications_doNotDivideByZero() {
        ApplicationStats stats = ApplicationStats.of(
                List.of(new StatusCount(ApplicationStatus.SAVED, 1L)),
                List.of(new StatusCount(ApplicationStatus.SAVED, 1L)));

        assertThat(stats.interviewRate()).isZero();
        assertThat(stats.offerRate()).isZero();
    }
}