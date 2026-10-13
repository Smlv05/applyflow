package com.applyflow.application;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatusTest {

    @ParameterizedTest
    @CsvSource({
            "SAVED, APPLIED",
            "SAVED, WITHDRAWN",
            "APPLIED, INTERVIEW",
            "APPLIED, REJECTED",
            "APPLIED, WITHDRAWN",
            "INTERVIEW, OFFER",
            "INTERVIEW, REJECTED",
            "INTERVIEW, WITHDRAWN",
            "OFFER, WITHDRAWN"
    })
    void allowedTransitions(ApplicationStatus from, ApplicationStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "SAVED, INTERVIEW",
            "SAVED, OFFER",
            "SAVED, REJECTED",
            "APPLIED, SAVED",
            "APPLIED, OFFER",
            "INTERVIEW, APPLIED",
            "OFFER, REJECTED",
            "OFFER, INTERVIEW",
            "REJECTED, APPLIED",
            "REJECTED, OFFER",
            "WITHDRAWN, APPLIED",
            "SAVED, SAVED"
    })
    void forbiddenTransitions(ApplicationStatus from, ApplicationStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"REJECTED", "WITHDRAWN"})
    void terminalStatusesHaveNoTransitions(ApplicationStatus status) {
        assertThat(status.allowedTransitions()).isEmpty();
    }
}