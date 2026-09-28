# Notes

## B1: double-booking race

Test: `backend/src/test/java/com/campusbooking/service/BookingConcurrencyTest.java`. 20 threads
behind a `CountDownLatch` start gate each call `BookingService.createBooking` for room AB1/101 with
overlapping ranges (start 10:00 + i min, end 13:00) and distinct start times, so the
`(start_time, block, room_no)` primary key can't reject them. Runs on Postgres 16 via Testcontainers.

Before the fix (3 runs, 2026-09-29): **10 of 20 succeeded every time**, leaving 10 overlapping
rows in `booking`. The other 10 got the service's `IllegalStateException` from the conflict SELECT.

Why 10: HikariCP's default pool is 10 connections. The first 10 threads each get a connection and
run the conflict SELECT before any of them commits. Under READ COMMITTED none of them sees the
others' uncommitted INSERTs, so all 10 pass the check. The other 10 wait for a free connection,
start after those commits, and see the conflicts.

After the fix (`no_overlapping_active_booking` GiST exclusion constraint + `saveAndFlush` +
409 mapping, 4 runs): **1 of 20 succeeded every time**, 1 row in `booking`. All 19 losers got
`BookingConflictException` (409): some from the conflict SELECT, the rest from the constraint
(SQLState `23P01`), translated in `BookingService.createBooking`.

`BookingOverlapTest` also covers: sequential overlap is rejected, overlap with a REJECTED or
CANCELLED booking is allowed (the constraint's `WHERE`), back-to-back bookings are allowed
(`tsrange` is `[start, end)`), a raw SQL insert that skips the app check is still rejected with
`23P01`, and an overlapping `POST /api/bookings` returns 409.

Not covered: two requests with the *same* start time racing still collide on the primary key
(SQLState `23505`), which is not mapped and comes back as a 500.
