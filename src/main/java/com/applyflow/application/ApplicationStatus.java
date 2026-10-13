package com.applyflow.application;

import java.util.EnumSet;
import java.util.Set;

public enum ApplicationStatus {
    SAVED,
    APPLIED,
    INTERVIEW,
    OFFER,
    REJECTED,
    WITHDRAWN;

    public Set<ApplicationStatus> allowedTransitions() {
        return switch (this) {
            case SAVED -> EnumSet.of(APPLIED, WITHDRAWN);
            case APPLIED -> EnumSet.of(INTERVIEW, REJECTED, WITHDRAWN);
            case INTERVIEW -> EnumSet.of(OFFER, REJECTED, WITHDRAWN);
            case OFFER -> EnumSet.of(WITHDRAWN);
            case REJECTED, WITHDRAWN -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }

    public boolean canTransitionTo(ApplicationStatus target) {
        return allowedTransitions().contains(target);
    }
}