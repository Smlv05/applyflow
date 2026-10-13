package com.applyflow.common.exception;

public class DuplicateApplicationException extends RuntimeException {

    public DuplicateApplicationException(Long jobId) {
        super("An application for job " + jobId + " already exists");
    }
}