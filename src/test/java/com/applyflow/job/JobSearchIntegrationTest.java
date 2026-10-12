package com.applyflow.job;

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
import org.springframework.test.web.servlet.ResultActions;
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
class JobSearchIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAdminAndLogin();
        // idempotent: safe to call before every test
        mockMvc.perform(post("/api/jobs/import").header("Authorization", bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void search_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void search_returnsNewestFirst() throws Exception {
        search("")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.content[0].title").value("Staff Backend Engineer"))
                .andExpect(jsonPath("$.content[7].title").value("Senior Java Developer"));
    }

    @Test
    void search_paginates() throws Exception {
        search("size=3&page=0")
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.totalPages").value(3));

        search("size=3&page=2")
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void search_capsPageSize() throws Exception {
        search("size=1000")
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void search_filtersByRemote() throws Exception {
        search("remote=true")
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void search_filtersBySkillCaseInsensitive() throws Exception {
        search("skill=kafka")
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void search_filtersByTextInTitleOrCompany() throws Exception {
        search("q=java")
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void search_filtersByLocation() throws Exception {
        search("location=warsaw")
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void search_filtersByMinSalary() throws Exception {
        search("minSalary=25000")
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void search_combinesFilters() throws Exception {
        search("remote=false&skill=Java")
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void getById_returnsJobWithSkills() throws Exception {
        String list = search("q=Staff").andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(list, "$.content[0].id");

        mockMvc.perform(get("/api/jobs/" + id).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Staff Backend Engineer"))
                .andExpect(jsonPath("$.skills.length()").value(6));
    }

    @Test
    void getById_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/jobs/999999").header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }

    private ResultActions search(String query) throws Exception {
        return mockMvc.perform(get("/api/jobs?" + query).header("Authorization", bearer()));
    }

    private String bearer() {
        return "Bearer " + token;
    }

    private String registerAdminAndLogin() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@test.com";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123","firstName":"Test","lastName":"Admin"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", email);

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