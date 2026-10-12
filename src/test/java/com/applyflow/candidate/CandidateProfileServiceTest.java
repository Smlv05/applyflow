package com.applyflow.candidate;

import com.applyflow.candidate.dto.AddSkillRequest;
import com.applyflow.candidate.dto.ProfileRequest;
import com.applyflow.candidate.dto.ProfileResponse;
import com.applyflow.common.exception.ResourceNotFoundException;
import com.applyflow.skill.Skill;
import com.applyflow.skill.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateProfileServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private CandidateProfileRepository profileRepository;

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private CandidateProfileService service;

    private CandidateProfile profile;
    private Skill java;

    @BeforeEach
    void setUp() {
        profile = new CandidateProfile(USER_ID, null, null, 0);
        ReflectionTestUtils.setField(profile, "id", 10L);

        java = new Skill("Java", "LANGUAGE");
        ReflectionTestUtils.setField(java, "id", 1L);

        lenient().when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        lenient().when(profileRepository.saveAndFlush(any(CandidateProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void getProfile_ensuresProfileExists() {
        ProfileResponse response = service.getProfile(USER_ID);

        verify(profileRepository).insertIfAbsent(USER_ID);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.skills()).isEmpty();
    }

    @Test
    void updateProfile_changesFields() {
        ProfileResponse response = service.updateProfile(USER_ID, new ProfileRequest("QA to backend", "Warsaw", 6));

        assertThat(response.summary()).isEqualTo("QA to backend");
        assertThat(response.location()).isEqualTo("Warsaw");
        assertThat(response.yearsOfExperience()).isEqualTo(6);
    }

    @Test
    void addSkill_newSkill_isAdded() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(java));

        ProfileResponse response = service.addOrUpdateSkill(USER_ID, new AddSkillRequest(1L, SkillLevel.ADVANCED));

        assertThat(response.skills()).hasSize(1);
        assertThat(response.skills().get(0).name()).isEqualTo("Java");
        assertThat(response.skills().get(0).level()).isEqualTo(SkillLevel.ADVANCED);
    }

    @Test
    void addSkill_existingSkill_updatesLevelWithoutDuplicate() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(java));

        service.addOrUpdateSkill(USER_ID, new AddSkillRequest(1L, SkillLevel.BEGINNER));
        ProfileResponse response = service.addOrUpdateSkill(USER_ID, new AddSkillRequest(1L, SkillLevel.EXPERT));

        assertThat(response.skills()).hasSize(1);
        assertThat(response.skills().get(0).level()).isEqualTo(SkillLevel.EXPERT);
    }

    @Test
    void addSkill_unknownSkill_throwsNotFound() {
        when(skillRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addOrUpdateSkill(USER_ID, new AddSkillRequest(999L, SkillLevel.EXPERT)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(profileRepository, never()).saveAndFlush(any());
    }

    @Test
    void removeSkill_present_removesIt() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(java));
        service.addOrUpdateSkill(USER_ID, new AddSkillRequest(1L, SkillLevel.EXPERT));

        service.removeSkill(USER_ID, 1L);

        assertThat(profile.getSkills()).isEmpty();
    }

    @Test
    void removeSkill_notInProfile_throwsNotFound() {
        assertThatThrownBy(() -> service.removeSkill(USER_ID, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}