package com.applyflow.auth.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) {
}