package com.applyflow.job;

public record ImportResult(String source, int fetched, int created, int skipped) {
}