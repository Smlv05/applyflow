package com.applyflow.candidate;

import com.applyflow.candidate.dto.AddSkillRequest;
import com.applyflow.candidate.dto.ProfileRequest;
import com.applyflow.candidate.dto.ProfileResponse;
import com.applyflow.common.exception.ResourceNotFoundException;
import com.applyflow.skill.Skill;
import com.applyflow.skill.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateProfileService {

    private final CandidateProfileRepository profileRepository;
    private final SkillRepository skillRepository;

    public CandidateProfileService(CandidateProfileRepository profileRepository,
                                   SkillRepository skillRepository) {
        this.profileRepository = profileRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional
    public ProfileResponse getProfile(Long userId) {
        return ProfileResponse.from(getOrCreate(userId));
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileRequest request) {
        CandidateProfile profile = getOrCreate(userId);
        profile.update(request.summary(), request.location(), request.yearsOfExperience());
        return ProfileResponse.from(profileRepository.saveAndFlush(profile));
    }

    @Transactional
    public ProfileResponse addOrUpdateSkill(Long userId, AddSkillRequest request) {
        CandidateProfile profile = getOrCreate(userId);
        Skill skill = skillRepository.findById(request.skillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + request.skillId()));

        profile.getSkills().stream()
                .filter(cs -> cs.getSkill().getId().equals(skill.getId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setLevel(request.level()),
                        () -> profile.getSkills().add(new CandidateSkill(profile, skill, request.level())));

        return ProfileResponse.from(profileRepository.saveAndFlush(profile));
    }

    @Transactional
    public void removeSkill(Long userId, Long skillId) {
        CandidateProfile profile = getOrCreate(userId);
        boolean removed = profile.getSkills()
                .removeIf(cs -> cs.getSkill().getId().equals(skillId));
        if (!removed) {
            throw new ResourceNotFoundException("Skill " + skillId + " is not in the profile");
        }
        profileRepository.saveAndFlush(profile);
    }

    private CandidateProfile getOrCreate(Long userId) {
        profileRepository.insertIfAbsent(userId);
        return profileRepository.findByUserId(userId).orElseThrow();
    }
}