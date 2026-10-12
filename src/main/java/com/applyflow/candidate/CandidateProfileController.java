package com.applyflow.candidate;

import com.applyflow.candidate.dto.AddSkillRequest;
import com.applyflow.candidate.dto.ProfileRequest;
import com.applyflow.candidate.dto.ProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class CandidateProfileController {

    private final CandidateProfileService service;

    public CandidateProfileController(CandidateProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
        return service.getProfile(userId(jwt));
    }

    @PutMapping
    public ProfileResponse update(@AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody ProfileRequest request) {
        return service.updateProfile(userId(jwt), request);
    }

    @PostMapping("/skills")
    public ProfileResponse addSkill(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody AddSkillRequest request) {
        return service.addOrUpdateSkill(userId(jwt), request);
    }

    @DeleteMapping("/skills/{skillId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSkill(@AuthenticationPrincipal Jwt jwt, @PathVariable Long skillId) {
        service.removeSkill(userId(jwt), skillId);
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}