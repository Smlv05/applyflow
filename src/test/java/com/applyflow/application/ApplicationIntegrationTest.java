package com.applyflow.application;

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApplicationIntegrationTest {

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
        mockMvc.perform(post("/api/jobs/import").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void applications_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_savesApplicationWithInitialHistory() throws Exception {
        String token = registerAndLogin(false);
        long jobId = jobId(token, "Senior");

        createRaw(token, jobId, "looks great")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SAVED"))
                .andExpect(jsonPath("$.notes").value("looks great"))
                .andExpect(jsonPath("$.job.title").value("Senior Java Developer"))
                .andExpect(jsonPath("$.history.length()").value(1))
                .andExpect(jsonPath("$.history[0].to").value("SAVED"));
    }

    @Test
    void create_duplicate_returnsConflict() throws Exception {
        String token = registerAndLogin(false);
        long jobId = jobId(token, "Senior");
        createRaw(token, jobId, null).andExpect(status().isCreated());

        createRaw(token, jobId, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("APPLICATION_ALREADY_EXISTS"));
    }

    @Test
    void create_unknownJob_returnsNotFound() throws Exception {
        String token = registerAndLogin(false);

        createRaw(token, 999999, null).andExpect(status().isNotFound());
    }

    @Test
    void create_withoutJobId_returnsBadRequest() throws Exception {
        String token = registerAndLogin(false);

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void changeStatus_followsHappyPathAndRecordsHistory() throws Exception {
        String token = registerAndLogin(false);
        long id = createApplication(token, jobId(token, "Senior"));

        changeStatus(token, id, "APPLIED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.appliedAt").isNotEmpty());
        changeStatus(token, id, "INTERVIEW").andExpect(status().isOk());
        changeStatus(token, id, "OFFER")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFFER"))
                .andExpect(jsonPath("$.history.length()").value(4))
                .andExpect(jsonPath("$.history[3].from").value("INTERVIEW"))
                .andExpect(jsonPath("$.history[3].to").value("OFFER"));
    }

    @Test
    void changeStatus_invalidTransition_returnsConflict() throws Exception {
        String token = registerAndLogin(false);
        long id = createApplication(token, jobId(token, "Senior"));

        changeStatus(token, id, "OFFER")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"));

        mockMvc.perform(get("/api/applications/" + id).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.status").value("SAVED"));
    }

    @Test
    void changeStatus_withUnknownStatus_returnsBadRequest() throws Exception {
        String token = registerAndLogin(false);
        long id = createApplication(token, jobId(token, "Senior"));

        changeStatus(token, id, "FLYING").andExpect(status().isBadRequest());
    }

    @Test
    void list_filtersByStatus() throws Exception {
        String token = registerAndLogin(false);
        long first = createApplication(token, jobId(token, "Senior"));
        createApplication(token, jobId(token, "Junior"));
        changeStatus(token, first, "APPLIED").andExpect(status().isOk());

        mockMvc.perform(get("/api/applications").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/applications?status=APPLIED").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(first));
    }

    @Test
    void list_withInvalidStatus_returnsBadRequest() throws Exception {
        String token = registerAndLogin(false);

        mockMvc.perform(get("/api/applications?status=FOO").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
    }

    @Test
    void otherUsersApplication_isNotFound() throws Exception {
        String owner = registerAndLogin(false);
        String stranger = registerAndLogin(false);
        long id = createApplication(owner, jobId(owner, "Senior"));

        mockMvc.perform(get("/api/applications/" + id).header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound());
        changeStatus(stranger, id, "APPLIED").andExpect(status().isNotFound());

        mockMvc.perform(get("/api/applications").header("Authorization", bearer(stranger)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void stats_withoutApplications_returnsZeros() throws Exception {
        String token = registerAndLogin(false);

        mockMvc.perform(get("/api/applications/stats").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.byStatus.SAVED").value(0))
                .andExpect(jsonPath("$.interviewRate").value(0.0));
    }

    @Test
    void stats_summarizesCurrentAndReachedStatuses() throws Exception {
        String token = registerAndLogin(false);
        long offered = createApplication(token, jobId(token, "Senior"));
        long rejected = createApplication(token, jobId(token, "Junior"));
        createApplication(token, jobId(token, "Staff"));

        changeStatus(token, offered, "APPLIED").andExpect(status().isOk());
        changeStatus(token, offered, "INTERVIEW").andExpect(status().isOk());
        changeStatus(token, offered, "OFFER").andExpect(status().isOk());
        changeStatus(token, rejected, "APPLIED").andExpect(status().isOk());
        changeStatus(token, rejected, "REJECTED").andExpect(status().isOk());

        mockMvc.perform(get("/api/applications/stats").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.byStatus.SAVED").value(1))
                .andExpect(jsonPath("$.byStatus.OFFER").value(1))
                .andExpect(jsonPath("$.byStatus.REJECTED").value(1))
                .andExpect(jsonPath("$.byStatus.APPLIED").value(0))
                .andExpect(jsonPath("$.applied").value(2))
                .andExpect(jsonPath("$.interviews").value(1))
                .andExpect(jsonPath("$.offers").value(1))
                .andExpect(jsonPath("$.interviewRate").value(50.0))
                .andExpect(jsonPath("$.offerRate").value(50.0));
    }

    @Test
    void create_concurrently_createsExactlyOne() throws Exception {
        String token = registerAndLogin(false);
        long jobId = jobId(token, "Staff");
        int threads = 8;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return createRaw(token, jobId, null).andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();

        int created = 0;
        int conflicts = 0;
        for (Future<Integer> future : futures) {
            int httpStatus = future.get(15, TimeUnit.SECONDS);
            if (httpStatus == 201) {
                created++;
            } else if (httpStatus == 409) {
                conflicts++;
            }
        }
        pool.shutdown();

        assertThat(created).isEqualTo(1);
        assertThat(conflicts).isEqualTo(threads - 1);
    }

    private ResultActions createRaw(String token, long jobId, String notes) throws Exception {
        String notesJson = notes == null ? "null" : "\"" + notes + "\"";
        return mockMvc.perform(post("/api/applications")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jobId\":" + jobId + ",\"notes\":" + notesJson + "}"));
    }

    private long createApplication(String token, long jobId) throws Exception {
        String body = createRaw(token, jobId, null)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions changeStatus(String token, long id, String newStatus) throws Exception {
        return mockMvc.perform(patch("/api/applications/" + id + "/status")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + newStatus + "\"}"));
    }

    private long jobId(String token, String titlePart) throws Exception {
        String body = mockMvc.perform(get("/api/jobs?q=" + titlePart).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.content[0].id")).longValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
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