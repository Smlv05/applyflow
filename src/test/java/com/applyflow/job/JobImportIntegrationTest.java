package com.applyflow.job;

import com.jayway.jsonpath.JsonPath;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class JobImportIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void import_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/jobs/import"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void import_asRegularUser_isForbidden() throws Exception {
        String token = registerAndLogin(false);

        mockMvc.perform(post("/api/jobs/import").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void import_asAdmin_isIdempotent() throws Exception {
        String token = registerAndLogin(true);

        mockMvc.perform(post("/api/jobs/import").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("mock"))
                .andExpect(jsonPath("$[0].fetched").value(8))
                .andExpect(jsonPath("$[0].created").value(8))
                .andExpect(jsonPath("$[0].skipped").value(0));

        mockMvc.perform(post("/api/jobs/import").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].created").value(0))
                .andExpect(jsonPath("$[0].skipped").value(8));

        Integer jobs = jdbcTemplate.queryForObject("SELECT count(*) FROM jobs", Integer.class);
        Integer links = jdbcTemplate.queryForObject("SELECT count(*) FROM job_skills", Integer.class);
        assertThat(jobs).isEqualTo(8);
        assertThat(links).isGreaterThan(8);
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