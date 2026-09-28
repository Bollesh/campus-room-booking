package com.campusbooking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.campusbooking.TestcontainersConfiguration;

// Seed data from DataLoader. Booking under test: room AB1/101 (managed by floor.manager1),
// Tech Club (faculty head faculty.head1), made by poc.student1. Other approvers: cultural.prof,
// sc.member1, security1. floor.manager2 manages AB2/101 only.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BookingApprovalAuthTest {

    private static final String START = "2030-03-01T10:00:00";
    private static final String APPROVALS_URL = "/api/bookings/AB1/101/" + START + "/approvals";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedPendingBooking() {
        jdbcTemplate.update("DELETE FROM booking_approval");
        jdbcTemplate.update("DELETE FROM booking");
        jdbcTemplate.update("""
                INSERT INTO booking (start_time, block, room_no, end_time, club_name, overall_status, purpose, student_email)
                VALUES (?::timestamp, 'AB1', '101', ?::timestamp, 'Tech Club', 'PENDING_APPROVAL', 'seeded', 'poc.student1@example.com')
                """, START, "2030-03-01T12:00:00");
    }

    @Test
    @WithUserDetails("poc.student1@example.com")
    void studentCannotApprove() throws Exception {
        approve("poc.student1@example.com", "APPROVED").andExpect(status().isForbidden());
        assertThat(approvalCount()).isZero();
    }

    @Test
    @WithUserDetails("poc.student1@example.com")
    void studentCannotApproveByClaimingToBeFloorManager() throws Exception {
        approve("floor.manager1@example.com", "APPROVED").andExpect(status().isForbidden());
        assertThat(approvalCount()).isZero();
    }

    @Test
    @WithUserDetails("floor.manager1@example.com")
    void approvalIsRecordedUnderCallerNotBodyEmail() throws Exception {
        approve("security1@example.com", "APPROVED").andExpect(status().isOk());

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT approver_email, approver_role FROM booking_approval");
        assertThat(row.get("approver_email")).isEqualTo("floor.manager1@example.com");
        assertThat(row.get("approver_role")).isEqualTo("FLOOR_MANAGER");
    }

    @Test
    @WithUserDetails("poc.student1@example.com")
    void bookingOwnerIsCallerNotBodyEmail() throws Exception {
        jdbcTemplate.update("DELETE FROM booking");

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"block": "AB1", "roomNo": "101",
                                 "startTime": "2030-03-02T10:00:00", "endTime": "2030-03-02T11:00:00",
                                 "purpose": "owner test", "studentEmail": "poc.student2@example.com",
                                 "clubName": "Tech Club"}
                                """))
                .andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject("SELECT student_email FROM booking", String.class))
                .isEqualTo("poc.student1@example.com");
    }

    @Test
    @WithUserDetails("poc.student2@example.com")
    void studentCannotBookInAnotherStudentsName() throws Exception {
        jdbcTemplate.update("DELETE FROM booking");

        // poc.student2 is not in Tech Club; claiming to be poc.student1 (who is) must not help.
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"block": "AB1", "roomNo": "101",
                                 "startTime": "2030-03-02T10:00:00", "endTime": "2030-03-02T11:00:00",
                                 "purpose": "impersonation", "studentEmail": "poc.student1@example.com",
                                 "clubName": "Tech Club"}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM booking", Integer.class)).isZero();
    }

    @Test
    void allRequiredRolesApprove_bookingIsApproved() throws Exception {
        for (String approver : new String[] {"floor.manager1@example.com", "faculty.head1@example.com",
                "cultural.prof@example.com", "sc.member1@example.com"}) {
            approveAs(approver, "APPROVED").andExpect(status().isOk());
            assertThat(overallStatus()).isEqualTo("PENDING_APPROVAL");
        }

        approveAs("security1@example.com", "APPROVED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").value("APPROVED"));
        assertThat(overallStatus()).isEqualTo("APPROVED");
    }

    @Test
    void oneRejection_bookingIsRejected() throws Exception {
        approveAs("floor.manager1@example.com", "APPROVED").andExpect(status().isOk());
        approveAs("security1@example.com", "REJECTED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").value("REJECTED"));
        assertThat(overallStatus()).isEqualTo("REJECTED");
    }

    private ResultActions approve(String bodyApproverEmail, String status) throws Exception {
        return mockMvc.perform(post(APPROVALS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approverEmail\": \"" + bodyApproverEmail + "\", \"status\": \"" + status + "\"}"));
    }

    // Authenticates as the approver through the real login endpoint and sends its JWT.
    private ResultActions approveAs(String email, String status) throws Exception {
        String token = com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORDS.get(email) + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.jwt");
        return mockMvc.perform(post(APPROVALS_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approverEmail\": \"" + email + "\", \"status\": \"" + status + "\"}"));
    }

    // Passwords seeded by DataLoader
    private static final Map<String, String> PASSWORDS = Map.of(
            "floor.manager1@example.com", "managerpass1",
            "floor.manager2@example.com", "managerpass2",
            "faculty.head1@example.com", "profpass1",
            "cultural.prof@example.com", "profpass3",
            "sc.member1@example.com", "scpass1",
            "security1@example.com", "securitypass1");

    private String overallStatus() {
        return jdbcTemplate.queryForObject("SELECT overall_status FROM booking", String.class);
    }

    private int approvalCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM booking_approval", Integer.class);
    }
}
