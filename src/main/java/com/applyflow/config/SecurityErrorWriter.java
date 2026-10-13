package com.applyflow.config;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Writes errors in the same shape as ApiError for failures that happen before the controllers. */
final class SecurityErrorWriter {

    private SecurityErrorWriter() {
    }

    static void write(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        String body = "{\"timestamp\":\"" + Instant.now() + "\","
                + "\"status\":" + status + ","
                + "\"error\":\"" + error + "\","
                + "\"message\":\"" + message + "\","
                + "\"fieldErrors\":{}}";
        response.getWriter().write(body);
    }
}