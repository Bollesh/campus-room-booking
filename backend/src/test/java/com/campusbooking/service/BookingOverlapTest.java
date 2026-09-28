package com.campusbooking.service;

import static com.campusbooking.service.BookingConcurrencyTest.request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import com.campusbooking.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BookingOverlapTest {

    private static final LocalDateTime TEN = LocalDateTime.of(2030, 2, 1, 10, 0);

    @Autowired private BookingService bookingService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void clearBookings() {
        jdbcTemplate.update("DELETE FROM booking_approval");
        jdbcTemplate.update("DELETE FROM booking");
    }

    @Test
    void overlappingBookingIsRejectedWithConflict() {
        bookingService.createBooking(request(TEN, TEN.plusHours(2)));

        assertThatThrownBy(() -> bookingService.createBooking(request(TEN.plusMinutes(30), TEN.plusMinutes(90))))
                .isInstanceOf(BookingConflictException.class);
    }

    @Test
    void overlapWithRejectedBookingIsAllowed() {
        bookingService.createBooking(request(TEN, TEN.plusHours(2)));
        setStatus(TEN, "REJECTED");

        bookingService.createBooking(request(TEN.plusMinutes(30), TEN.plusMinutes(90)));

        assertThat(bookingCount()).isEqualTo(2);
    }

    @Test
    void overlapWithCancelledBookingIsAllowed() {
        bookingService.createBooking(request(TEN, TEN.plusHours(2)));
        setStatus(TEN, "CANCELLED");

        bookingService.createBooking(request(TEN.plusMinutes(30), TEN.plusMinutes(90)));

        assertThat(bookingCount()).isEqualTo(2);
    }

    @Test
    void backToBackBookingsAreAllowed() {
        bookingService.createBooking(request(TEN, TEN.plusHours(1)));
        bookingService.createBooking(request(TEN.plusHours(1), TEN.plusHours(2)));

        assertThat(bookingCount()).isEqualTo(2);
    }

    @Test
    void constraintRejectsOverlapEvenWhenAppCheckIsBypassed() {
        insertDirectly(TEN, TEN.plusHours(2));

        assertThatThrownBy(() -> insertDirectly(TEN.plusMinutes(30), TEN.plusMinutes(90)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasRootCauseInstanceOf(SQLException.class)
                .rootCause()
                .satisfies(e -> assertThat(((SQLException) e).getSQLState()).isEqualTo("23P01"));
    }

    @Test
    @WithUserDetails("poc.student1@example.com")
    void overlappingBookingOverHttpReturns409() throws Exception {
        bookingService.createBooking(request(TEN, TEN.plusHours(2)));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"block": "AB1", "roomNo": "101",
                                 "startTime": "2030-02-01T10:30:00", "endTime": "2030-02-01T11:30:00",
                                 "purpose": "overlap", "studentEmail": "poc.student1@example.com",
                                 "clubName": "Tech Club"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    private void setStatus(LocalDateTime start, String status) {
        jdbcTemplate.update("UPDATE booking SET overall_status = ? WHERE block = 'AB1' AND room_no = '101' AND start_time = ?",
                status, Timestamp.valueOf(start));
    }

    private void insertDirectly(LocalDateTime start, LocalDateTime end) {
        jdbcTemplate.update("""
                INSERT INTO booking (start_time, block, room_no, end_time, club_name, overall_status, purpose, student_email)
                VALUES (?, 'AB1', '101', ?, 'Tech Club', 'PENDING_APPROVAL', 'direct insert', 'poc.student1@example.com')
                """, Timestamp.valueOf(start), Timestamp.valueOf(end));
    }

    private int bookingCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM booking", Integer.class);
    }
}
