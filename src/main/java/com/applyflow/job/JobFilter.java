package com.applyflow.job;

public record JobFilter(
        String q,
        String location,
        Boolean remote,
        String skill,
        Integer minSalary) {
}