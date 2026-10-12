package com.applyflow.matching;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RecommendationIntegrationTest {

    // ids from the V3 seed migration
    private static final int JAVA = 1;
    private static final int SPRING_BOOT = 3;
    private static final int POSTGRESQL = 5;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void importJobs() throws Exception {
        String adminToken = registerAndLogin(true);
        // idempotent: safe to call before every test
        mockMvc.perform(post("/api/jobs/import").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void recommended_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/jobs/recommended"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recommended_withoutSkills_returnsEmptyPage() throws Exception {
        String token = registerAndLogin(false);

        mockMvc.perform(get("/api/jobs/recommended").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void recommended_ranksJobsByScoreWithBreakdown() throws Exception {
        String token = candidateWithExpertSkills(JAVA, SPRING_BOOT, POSTGRESQL);

        mockMvc.perform(get("/api/jobs/recommended").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // 7 of 8 jobs share at least one skill (the DevOps job shares none)
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.content[0].job.title").value("Senior Java Developer"))
                .andExpect(jsonPath("$.content[0].score").value(75))
                .andExpect(jsonPath("$.content[0].matchedSkills.length()").value(3))
                .andExpect(jsonPath("$.content[0].missingSkills[0]").value("Docker"))
                .andExpect(jsonPath("$.content[1].job.title").value("Staff Backend Engineer"))
                .andExpect(jsonPath("$.content[1].score").value(50))
                .andExpect(jsonPath("$.content[2].job.title").value("Backend Engineer"))
                .andExpect(jsonPath("$.content[2].score").value(40))
                // equal scores are ordered by newest publication first
                .andExpect(jsonPath("$.content[3].job.title").value("Junior Java Developer"))
                .andExpect(jsonPath("$.content[3].score").value(25))
                .andExpect(jsonPath("$.content[6].job.title").value("QA Automation Engineer"))
                .andExpect(jsonPath("$.content[6].score").value(20));
    }

    @Test
    void recommended_respectsMinScore() throws Exception {
        String token = candidateWithExpertSkills(JAVA, SPRING_BOOT, POSTGRESQL);

        mockMvc.perform(get("/api/jobs/recommended?minScore=50").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void recommended_paginates() throws Exception {
        String token = candidateWithExpertSkills(JAVA, SPRING_BOOT, POSTGRESQL);

        mockMvc.perform(get("/api/jobs/recommended?size=3&page=2").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    private String candidateWithExpertSkills(int... skillIds) throws Exception {
        String token = registerAndLogin(false);
        for (int skillId : skillIds) {
            mockMvc.perform(post("/api/profile/skills")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"skillId\":" + skillId + ",\"level\":\"EXPERT\"}"))
                    .andExpect(status().isOk());
        }
        return token;
    }

    private String registerAndLogin(boolean admin) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.com";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123","firstName":"Test","lastName":"User"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        if (admin) {
            jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", email);
        }

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.accessToken");
    }
}