package com.campusbooking.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.campusbooking.TestcontainersConfiguration;
import com.campusbooking.types.BookingRequest;

// Seed data comes from DataLoader: room AB1/101, student poc.student1 in Tech Club.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingConcurrencyTest {

    private static final int THREADS = 20;
    private static final LocalDateTime BASE = LocalDateTime.of(2030, 1, 15, 10, 0);

    @Autowired private BookingService bookingService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearBookings() {
        jdbcTemplate.update("DELETE FROM booking_approval");
        jdbcTemplate.update("DELETE FROM booking");
    }

    @Test
    void concurrentOverlappingBookingsForSameRoom_onlyOneSucceeds() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        Map<String, AtomicInteger> failures = new ConcurrentHashMap<>();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            // Distinct start times, so the (start_time, block, room_no) PK can't catch the overlap.
            LocalDateTime start = BASE.plusMinutes(i);
            BookingRequest request = request(start, BASE.plusHours(3));
            futures.add(pool.submit(() -> {
                ready.countDown();
                startGate.await();
                try {
                    bookingService.createBooking(request);
                    successes.incrementAndGet();
                } catch (Exception e) {
                    failures.computeIfAbsent(e.getClass().getSimpleName(), k -> new AtomicInteger()).incrementAndGet();
                }
                return null;
            }));
        }

        ready.await(10, TimeUnit.SECONDS);
        startGate.countDown();
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM booking WHERE block = 'AB1' AND room_no = '101'", Integer.class);
        System.out.printf("RACE RESULT: %d of %d succeeded, %d rows in booking, failures=%s%n",
                successes.get(), THREADS, rows, failures);

        assertThat(successes.get()).isEqualTo(1);
        assertThat(rows).isEqualTo(1);
        // Losers must get the conflict error (409), whether the SELECT or the constraint caught them.
        assertThat(failures.keySet()).containsOnly(BookingConflictException.class.getSimpleName());
    }

    @Test
    void concurrentBookingsWithSameStartTime_onlyOneSucceeds() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        Map<String, AtomicInteger> failures = new ConcurrentHashMap<>();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            // Same start time: all requests also collide on the (start_time, block, room_no) PK.
            BookingRequest request = request(BASE, BASE.plusHours(1).plusMinutes(i));
            futures.add(pool.submit(() -> {
                ready.countDown();
                startGate.await();
                try {
                    bookingService.createBooking(request);
                    successes.incrementAndGet();
                } catch (Exception e) {
                    failures.computeIfAbsent(e.getClass().getSimpleName(), k -> new AtomicInteger()).incrementAndGet();
                }
                return null;
            }));
        }

        ready.await(10, TimeUnit.SECONDS);
        startGate.countDown();
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        System.out.printf("SAME-START RESULT: %d of %d succeeded, failures=%s%n", successes.get(), THREADS, failures);

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.keySet()).containsOnly(BookingConflictException.class.getSimpleName());
    }

    static BookingRequest request(LocalDateTime start, LocalDateTime end) {
        BookingRequest r = new BookingRequest();
        r.setBlock("AB1");
        r.setRoomNo("101");
        r.setStartTime(start);
        r.setEndTime(end);
        r.setPurpose("race test");
        r.setStudentEmail("poc.student1@example.com");
        r.setClubName("Tech Club");
        return r;
    }
}
