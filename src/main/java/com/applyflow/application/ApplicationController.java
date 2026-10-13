package com.applyflow.application;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody CreateApplicationRequest request) {
        return service.create(userId(jwt), request);
    }

    @GetMapping
    public List<ApplicationResponse> list(@AuthenticationPrincipal Jwt jwt,
                                          @RequestParam(required = false) ApplicationStatus status) {
        return service.list(userId(jwt), status);
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(userId(jwt), id);
    }

    @PatchMapping("/{id}/status")
    public ApplicationResponse changeStatus(@AuthenticationPrincipal Jwt jwt,
                                            @PathVariable Long id,
                                            @Valid @RequestBody ChangeStatusRequest request) {
        return service.changeStatus(userId(jwt), id, request.status());
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}