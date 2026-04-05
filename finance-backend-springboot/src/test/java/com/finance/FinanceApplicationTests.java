package com.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.entity.Role;
import com.finance.entity.User;
import com.finance.repository.FinancialRecordRepository;
import com.finance.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FinanceApplicationTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepo;
    @Autowired FinancialRecordRepository recordRepo;
    @Autowired PasswordEncoder encoder;

    static String adminToken;
    static String analystToken;
    static String viewerToken;
    static Long   createdRecordId;
    static Long   analystUserId;

    @BeforeEach
    void setup() {
        if (userRepo.count() == 0) {
            userRepo.save(User.builder().name("Admin").email("admin@test.com")
                    .password(encoder.encode("Admin@123")).role(Role.ADMIN).active(true).build());
            userRepo.save(User.builder().name("Analyst").email("analyst@test.com")
                    .password(encoder.encode("Pass@123")).role(Role.ANALYST).active(true).build());
            userRepo.save(User.builder().name("Viewer").email("viewer@test.com")
                    .password(encoder.encode("Pass@123")).role(Role.VIEWER).active(true).build());
        }
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Test @Order(1)
    void loginAdmin() throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "email", "admin@test.com", "password", "Admin@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();
        adminToken = mapper.readTree(res.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test @Order(2)
    void loginAnalyst() throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "email", "analyst@test.com", "password", "Pass@123"))))
                .andExpect(status().isOk()).andReturn();
        analystToken = mapper.readTree(res.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test @Order(3)
    void loginViewer() throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "email", "viewer@test.com", "password", "Pass@123"))))
                .andExpect(status().isOk()).andReturn();
        viewerToken = mapper.readTree(res.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test @Order(4)
    void loginBadCredentials() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "email", "admin@test.com", "password", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test @Order(5)
    void getMe() throws Exception {
        mvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // ── User Management ───────────────────────────────────────────────────────

    @Test @Order(10)
    void adminCanListUsers() throws Exception {
        mvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test @Order(11)
    void viewerCannotListUsers() throws Exception {
        mvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test @Order(12)
    void adminCreatesUser() throws Exception {
        MvcResult res = mvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "name", "New Analyst", "email", "newanalyst@test.com",
                        "password", "Pass@123", "role", "ANALYST"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ANALYST"))
                .andReturn();
        analystUserId = mapper.readTree(res.getResponse().getContentAsString())
                .get("id").asLong();
    }

    @Test @Order(13)
    void duplicateEmailRejected() throws Exception {
        mvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "name", "Dup", "email", "admin@test.com",
                        "password", "Pass@123", "role", "VIEWER"))))
                .andExpect(status().isConflict());
    }

    @Test @Order(14)
    void adminUpdatesUser() throws Exception {
        mvc.perform(patch("/api/users/" + analystUserId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // ── Financial Records ─────────────────────────────────────────────────────

    @Test @Order(20)
    void analystCreatesRecord() throws Exception {
        MvcResult res = mvc.perform(post("/api/records")
                .header("Authorization", "Bearer " + analystToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "amount", 5000, "type", "INCOME",
                        "category", "Salary", "date", "2025-12-01"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andReturn();
        createdRecordId = mapper.readTree(res.getResponse().getContentAsString())
                .get("id").asLong();
    }

    @Test @Order(21)
    void viewerCannotCreateRecord() throws Exception {
        mvc.perform(post("/api/records")
                .header("Authorization", "Bearer " + viewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "amount", 100, "type", "INCOME",
                        "category", "Test", "date", "2025-12-01"))))
                .andExpect(status().isForbidden());
    }

    @Test @Order(22)
    void viewerCanReadRecords() throws Exception {
        mvc.perform(get("/api/records")
                .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test @Order(23)
    void filterRecordsByType() throws Exception {
        mvc.perform(get("/api/records?type=INCOME")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].type", everyItem(is("INCOME"))));
    }

    @Test @Order(24)
    void analystUpdatesRecord() throws Exception {
        mvc.perform(patch("/api/records/" + createdRecordId)
                .header("Authorization", "Bearer " + analystToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("notes", "Updated note"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Updated note"));
    }

    @Test @Order(25)
    void invalidAmountRejected() throws Exception {
        mvc.perform(post("/api/records")
                .header("Authorization", "Bearer " + analystToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "amount", -50, "type", "INCOME",
                        "category", "Test", "date", "2025-12-01"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test @Order(26)
    void viewerCannotDeleteRecord() throws Exception {
        mvc.perform(delete("/api/records/" + createdRecordId)
                .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test @Order(27)
    void adminDeletesRecord() throws Exception {
        mvc.perform(delete("/api/records/" + createdRecordId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test @Order(28)
    void deletedRecordNotFound() throws Exception {
        mvc.perform(get("/api/records/" + createdRecordId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Test @Order(30)
    void dashboardSummaryAccessible() throws Exception {
        mvc.perform(get("/api/dashboard/summary")
                .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.totalIncome").exists())
                .andExpect(jsonPath("$.overview.totalExpenses").exists())
                .andExpect(jsonPath("$.overview.netBalance").exists())
                .andExpect(jsonPath("$.byCategory").isArray())
                .andExpect(jsonPath("$.recentActivity").isArray());
    }

    @Test @Order(31)
    void trendsEndpointWorks() throws Exception {
        mvc.perform(get("/api/dashboard/trends?months=3")
                .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months").value(3))
                .andExpect(jsonPath("$.trends").isArray());
    }

    @Test @Order(32)
    void unauthenticatedCannotAccessDashboard() throws Exception {
        mvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }
}
