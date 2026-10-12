package com.applyflow.common.exception;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(Object from, Object to) {
        super("Cannot change application status from " + from + " to " + to);
    }
}