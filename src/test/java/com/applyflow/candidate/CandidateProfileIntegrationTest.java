package com.applyflow.candidate;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CandidateProfileIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void profile_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_createsEmptyProfileOnFirstAccess() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/profile").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearsOfExperience").value(0))
                .andExpect(jsonPath("$.skills").isEmpty());
    }

    @Test
    void updateProfile_persistsChanges() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"summary":"QA to backend","location":"Warsaw","yearsOfExperience":6}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/profile").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.summary").value("QA to backend"))
                .andExpect(jsonPath("$.location").value("Warsaw"))
                .andExpect(jsonPath("$.yearsOfExperience").value(6));
    }

    @Test
    void updateProfile_withInvalidYears_returnsBadRequest() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"summary":"x","location":"y","yearsOfExperience":-1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addSkill_twice_updatesLevelAndKeepsSingleEntry() throws Exception {
        String token = registerAndLogin();

        addSkill(token, 1, "BEGINNER").andExpect(status().isOk());
        addSkill(token, 1, "EXPERT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills.length()").value(1))
                .andExpect(jsonPath("$.skills[0].name").value("Java"))
                .andExpect(jsonPath("$.skills[0].level").value("EXPERT"));
    }

    @Test
    void addSkill_unknownSkill_returnsNotFound() throws Exception {
        String token = registerAndLogin();

        addSkill(token, 99999, "EXPERT").andExpect(status().isNotFound());
    }

    @Test
    void addSkill_withInvalidLevel_returnsBadRequest() throws Exception {
        String token = registerAndLogin();

        addSkill(token, 1, "GOD_MODE").andExpect(status().isBadRequest());
    }

    @Test
    void removeSkill_removesOnce_thenNotFound() throws Exception {
        String token = registerAndLogin();
        addSkill(token, 2, "ADVANCED").andExpect(status().isOk());

        mockMvc.perform(delete("/api/profile/skills/2").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/profile").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.skills").isEmpty());

        mockMvc.perform(delete("/api/profile/skills/2").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void profiles_areIsolatedBetweenUsers() throws Exception {
        String firstToken = registerAndLogin();
        String secondToken = registerAndLogin();

        addSkill(firstToken, 1, "EXPERT").andExpect(status().isOk());

        mockMvc.perform(get("/api/profile").header("Authorization", bearer(secondToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills").isEmpty());
    }

    private ResultActions addSkill(String token, long skillId, String level) throws Exception {
        return mockMvc.perform(post("/api/profile/skills")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skillId\":" + skillId + ",\"level\":\"" + level + "\"}"));
    }

    private String registerAndLogin() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.com";
        String registerBody = """
                {"email":"%s","password":"Password123","firstName":"Test","lastName":"User"}
                """.formatted(email);
        String loginBody = """
                {"email":"%s","password":"Password123"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}