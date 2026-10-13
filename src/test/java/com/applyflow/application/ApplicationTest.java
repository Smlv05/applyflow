package com.applyflow.application;

import com.applyflow.common.exception.InvalidStatusTransitionException;
import com.applyflow.job.Job;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTest {

    private static Application newApplication() {
        return new Application(1L, new Job("mock", "1", "Java Developer", "Acme"), "interesting");
    }

    @Test
    void newApplication_startsSavedWithInitialHistory() {
        Application application = newApplication();

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(application.getAppliedAt()).isNull();
        assertThat(application.getHistory()).hasSize(1);
        assertThat(application.getHistory().get(0).getFromStatus()).isNull();
        assertThat(application.getHistory().get(0).getToStatus()).isEqualTo(ApplicationStatus.SAVED);
    }

    @Test
    void changeStatus_validTransition_updatesStatusAndRecordsHistory() {
        Application application = newApplication();

        application.changeStatus(ApplicationStatus.APPLIED);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(application.getAppliedAt()).isNotNull();
        assertThat(application.getHistory()).hasSize(2);
        ApplicationStatusChange last = application.getHistory().get(1);
        assertThat(last.getFromStatus()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(last.getToStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void changeStatus_invalidTransition_throwsAndKeepsState() {
        Application application = newApplication();

        assertThatThrownBy(() -> application.changeStatus(ApplicationStatus.OFFER))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("SAVED")
                .hasMessageContaining("OFFER");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(application.getHistory()).hasSize(1);
    }

    @Test
    void changeStatus_fromTerminalStatus_isRejected() {
        Application application = newApplication();
        application.changeStatus(ApplicationStatus.APPLIED);
        application.changeStatus(ApplicationStatus.REJECTED);

        assertThatThrownBy(() -> application.changeStatus(ApplicationStatus.APPLIED))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void fullHappyPath_recordsEveryStep() {
        Application application = newApplication();

        application.changeStatus(ApplicationStatus.APPLIED);
        application.changeStatus(ApplicationStatus.INTERVIEW);
        application.changeStatus(ApplicationStatus.OFFER);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.OFFER);
        assertThat(application.getHistory()).extracting(ApplicationStatusChange::getToStatus)
                .containsExactly(ApplicationStatus.SAVED, ApplicationStatus.APPLIED,
                        ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER);
    }
}