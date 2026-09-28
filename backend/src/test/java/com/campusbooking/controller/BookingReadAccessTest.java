package com.campusbooking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.campusbooking.TestcontainersConfiguration;

// One booking each for poc.student1 (Tech Club, AB1/101) and poc.student2 (Music Club, AB2/101).
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BookingReadAccessTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedBookings() {
        jdbcTemplate.update("DELETE FROM booking_approval");
        jdbcTemplate.update("DELETE FROM booking");
        insertBooking("AB1", "Tech Club", "poc.student1@example.com");
        insertBooking("AB2", "Music Club", "poc.student2@example.com");
    }

    @Test
    @WithUserDetails("security1@example.com")
    void bookingListDoesNotExposePasswordHashes() throws Exception {
        String body = mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("poc.student1@example.com").doesNotContain("password").doesNotContain("$2a$");
    }

    @Test
    @WithUserDetails("security1@example.com")
    void singleBookingDoesNotExposePasswordHashes() throws Exception {
        String body = mockMvc.perform(get("/api/bookings/AB1/101/2030-04-01T10:00:00"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("password").doesNotContain("$2a$");
    }

    @Test
    void registrationAndLoginStillWorkWithWriteOnlyPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "new.student@example.com", "password": "newpass123", "name": "New Student",
                                 "phone": 5550001111, "userType": "STUDENT", "regno": 23100199}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"new.student@example.com\", \"password\": \"newpass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").exists());
    }

    @Test
    @WithUserDetails("security1@example.com")
    void entityEndpointStillAcceptsPasswordInRequestBody() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "entity.student@example.com", "password": "some-hash", "name": "Entity Student",
                                 "phone": 5550002222, "regno": 23100198}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT password FROM users WHERE email = 'entity.student@example.com'", String.class))
                .isEqualTo("some-hash");
    }

    private void insertBooking(String block, String club, String student) {
        jdbcTemplate.update("""
                INSERT INTO booking (start_time, block, room_no, end_time, club_name, overall_status, purpose, student_email)
                VALUES ('2030-04-01T10:00:00'::timestamp, ?, '101', '2030-04-01T11:00:00'::timestamp, ?, 'PENDING_APPROVAL', 'read test', ?)
                """, block, club, student);
    }
}
